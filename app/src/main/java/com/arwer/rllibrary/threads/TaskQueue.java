package com.arwer.rllibrary.threads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * 任务队列管理类。
 * TaskQueue是一个基于Java ExecutorService的线程池包装类，对上层代码提供简单易用的接口方法，使上层代码聚焦业务而无需繁琐实现线程池的细节代码。
 *
 * 现有功能：
 * 1、管理并发线程，可以1个线程串行，也可以自定义并发线程数；
 * 2、可以添加任务，执行任意Callable任务，比如网络存取、数据库存取、复杂计算等等；
 * 3、可以对某一任务执行取消、得到状态/执行结果；可以取消未执行的任务；可以手动控制队列开始/停止；
 * 4、队列中所有任务完成后有回调，可以得到每个任务的执行状况以及返回值。
 *
 * 示例可以参考单元测试类：TaskQueueUnitTest 或 TaskQueueAppTest
 *
 * Created by long on 16/2/27.
 */
public class TaskQueue {

    ////////////////////////////////////////////////////////////////
    // 定义接口
    ////////////////////////////////////////////////////////////////
    public interface IQueueFinishedCallback {
        /**
         * 任务队列里所有任务完成后的回调
         * @param result 每个任务的返回结果对象集合。key为任务名；value为正常对象或异常对象。
         */
        public void onFinished(Map<String, Object> result);
    }


    ////////////////////////////////////////////////////////////////
    // 自定义FutureTask的子类
    ////////////////////////////////////////////////////////////////
    private final class Task<V> extends FutureTask<V> {

        private String mTaskName;

        private Task(Callable<V> callable) {
            super(callable);
        }
        public Task(String taskName, Callable<V> callable) {
            super(callable);
            mTaskName = taskName;
        }

        @Override
        protected void done() {
            super.done();
            // 调用队列完成函数
            queueFinished();
        }

        public String getName() {
            return mTaskName;
        }
//        public void setName(String val) {
//            mTaskName = val;
//        }
    }

    ////////////////////////////////////////////////////////////////
    // 定义常量、变量、枚举
    ////////////////////////////////////////////////////////////////

    // 最大并发线程数
    private int maxConcurrentRequestCount = 4; //默认4个线程
    // 定义线程池对象
    private ExecutorService mPool = null;
//    private ScheduledThreadPoolExecutor mScheduledPool = null;
    // HashMap缓存线程和tag的映射关系，便于cancel操作
    private List<Task> mTaskList = null;
//    private Map<String, FutureTask<?>> mTaskList = null;
    // 队列完成后的回调
    private IQueueFinishedCallback mQueueFinishedCallback = null;

    private int mAddingTaskCount = 0;


    ////////////////////////////////////////////////////////////////
    // 定义属性
    ////////////////////////////////////////////////////////////////

    /**
     * 得到最大并发线程数
     * @return
     */
    public int getMaxConcurrentThreadCount() {
        return maxConcurrentRequestCount;
    }

    /**
     * 设置最大并发线程数
     * @param maxConcurrentRequestCount
     */
    public void setMaxConcurrentThreadCount(int maxConcurrentRequestCount) {
        this.maxConcurrentRequestCount = maxConcurrentRequestCount;
    }

    /**
     * 得到线程池的执行工具
     * @return
     */
    private ExecutorService getPool() {
        /*
        注：线程池类型：
        1. newSingleThreadExecutor 创建一个单线程的线程池。这个线程池只有一个线程在工作，也就是相当于单线程串行执行所有任务。
        2. newFixedThreadPool 创建固定大小的线程池。每次提交一个任务就创建一个线程，直到线程达到线程池的最大大小。
        3. newCachedThreadPool 创建一个可缓存的线程池。如果线程池的大小超过了处理任务所需要的线程，那么就会回收部分空闲（60秒不执行任务）的线程，
        当任务数增加时，此线程池又可以智能的添加新线程来处理任务。
        此线程池不会对线程池大小做限制，线程池大小完全依赖于操作系统（或者说JVM）能够创建的最大线程大小。
        4.newScheduledThreadPool 创建一个大小无限的线程池。此线程池支持定时以及周期性执行任务的需求。

        在JDK帮助文档中，有如此一段话：
        “强烈建议程序员使用较为方便的Executors工厂方法Executors.newCachedThreadPool()（无界线程池，可以进行自动线程回收）

        本模块考虑实际业务场景可能存在串行或并行等多种情况，所以交由开发者决定
         */
        if (mPool == null) {
            if (maxConcurrentRequestCount == 1) {
                mPool = Executors.newSingleThreadExecutor();
            }
//            else if (maxConcurrentRequestCount > 1 && maxConcurrentRequestCount < 11) {
//                mPool = Executors.newFixedThreadPool(maxConcurrentRequestCount);
//                // 一个线程1M，考虑手机内存控制，当大于10个线程，则用newCachedThreadPool
////                mPool = Executors.newCachedThreadPool();
//            }
//            else if (maxConcurrentRequestCount > 10) {
            else {
                // 一个线程1M，考虑手机内存控制，当大于10个线程，则用newCachedThreadPool
                mPool = Executors.newCachedThreadPool();
            }
        }
        return mPool;
    }

    ////////////////////////////////////////////////////////////////
    // 类生命周期函数
    ////////////////////////////////////////////////////////////////
    public TaskQueue() {
        this(null);
    }

    public TaskQueue(IQueueFinishedCallback callback) {
        // 缓存callback、初始化定时任务线程池（检查是否全部完成和callback）
        setQueueFinishedCallback(callback);
        // 初始化任务列表
//        mTaskList = new HashMap<String, FutureTask<?>>();
        mTaskList = new ArrayList<>();
    }


    ////////////////////////////////////////////////////////////////
    // 方法定义
    ////////////////////////////////////////////////////////////////

    /**
     * 添加一个任务，立即执行
     * @param taskName 任务名，每个任务的唯一标识符
     * @param task 要执行的任务（实现Callable的类）
     * @param <V> 任务执行完返回的类型
     * @throws Exception
     */
    public <V> void add(String taskName, Callable<V> task) throws Exception {
        if (taskName == null || taskName.length() < 1) {
            throw new Exception("param \"taskName\" can't be empty.");
        }

        // 创建任务
////        FutureTask<V> futureTask = new FutureTask<V>(task);
//        Task<V> futureTask = new Task<V>(task);
//
//        // 添加任务到列表
//        Map<String, FutureTask<?>> hashMap = new HashMap<String, FutureTask<?>>();
//        hashMap.put(taskName, futureTask);
        Task<V> futureTask = new Task<V>(taskName, task);
        mTaskList.add(futureTask);
        //
        ++mAddingTaskCount;
//        if (mPool != null) {
//            // 执行任务
//            getPool().execute(futureTask);
//        }
    }

    /**
     * 任务队列开始执行
     */
    public void start() {
        // 如果线程池对象不为空，表示已经运行队列了，则退出
        if (mPool != null) return;

//        // 定时检查队列完成状态的任务
//        if (mScheduledPool != null) {
//            mScheduledPool.shutdownNow();
//            mScheduledPool = null;
//        }
//        mScheduledPool = new ScheduledThreadPoolExecutor(1);
//        mScheduledPool.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                if (mTaskList == null || mTaskList.size() < 1) {
//                    return;
//                }
//
//                // 统计队列里完成的数量和结果值
//                int count = 0;
//                Map<String, Object> result = new HashMap<String, Object>(); //结果集合
//                for (Map.Entry entry : mTaskList.entrySet()) {
//                    FutureTask<Object> futureTask = (FutureTask<Object>)entry.getValue();
//                    System.out.println(">>> 遍历任务检查状态，taskName=" + entry.getKey() + ", isDone=" + futureTask.isDone());
//                    if ( futureTask.isDone() ) {
//                        ++count;
//                        try {
//                            if (futureTask.isCancelled())
//                                result.put(entry.getKey().toString(), null);
//                            else
//                                result.put(entry.getKey().toString(), futureTask.get());
//                        } catch (InterruptedException e) { //中断异常
//                            e.printStackTrace();
//                            result.put(entry.getKey().toString(), e);
//                        } catch (ExecutionException e) { //执行中抛出的异常
//                            e.printStackTrace();
//                            result.put(entry.getKey().toString(), e);
//                        }
//                    }
//                }
//
//                // 如果队列里任务都完成，则执行完成回调
//                if (count == mTaskList.size()) {
//                    if (mQueueFinishedCallback != null) {
//                        mQueueFinishedCallback.onFinished(result);
//                    }
//                    mTaskList.clear();
//                }
//            }
//        }, 1, 1, TimeUnit.SECONDS);


        for (int i=0; i<mTaskList.size(); i++) {
            Task futureTask = mTaskList.get(i);
            getPool().execute(futureTask);
        }
    }

    /**
     * 任务队列停止、清理和触发队列完成的回调
     */
    public void stop() {

        // 获得队列里任务的完成结果
        Map<String, Object> result = new HashMap<String, Object>(); //结果集合
        for (int i=0; i<mTaskList.size(); i++) {
            Task<Object> futureTask = (Task<Object>)mTaskList.get(i);
//            System.out.println(">>> 遍历任务检查状态，taskName=" + entry.getKey() + ", isDone=" + futureTask.isDone());
            if (futureTask.isDone()) {
                try {
                    if (futureTask.isCancelled())
                        result.put(futureTask.getName(), null);
                    else
                        result.put(futureTask.getName(), futureTask.get());
                } catch (InterruptedException e) { //中断异常
                    e.printStackTrace();
                    result.put(futureTask.getName(), e);
                } catch (ExecutionException e) { //执行中抛出的异常
                    e.printStackTrace();
                    result.put(futureTask.getName(), e);
                }
            }
        }

//        // 停止定时检查队列完成状态的任务
//        if (mScheduledPool != null) {
//            mScheduledPool.shutdownNow();
//            mScheduledPool = null;
//        }

        // 取消队列中所有的任务，包括执行中和未执行的; 清理缓存对象
        cancelAll();

            // 触发队列完成的回调
        if (mQueueFinishedCallback != null) mQueueFinishedCallback.onFinished(result);


    }

    /**
     * 指定任务是否执行完成
     * @param taskName 任务名称
     * @return true=完成; false=未完成
     */
    public boolean isDone(String taskName) {

        for (int i=0; i<mTaskList.size(); i++) {
            Task<?> task = mTaskList.get(i);
            if (task != null && task.getName().equals(taskName)) {
                return task.isDone();
            }
        }
        return false;
    }

    /**
     * 指定任务是否已经取消了
     * @param taskName 任务名称
     * @return true=已经取消；false=未取消
     */
    public boolean isCancelled(String taskName) {

        for (int i=0; i<mTaskList.size(); i++) {
            Task<?> task = mTaskList.get(i);
            if (task != null && task.getName().equals(taskName)) {
                return task.isCancelled();
            }
        }
        return false;
    }

    /**
     * 取消一个任务的执行
     * @param taskName 任务名称
     */
    public void cancel(String taskName) {

        for (int i=0; i<mTaskList.size(); i++) {
            Task<?> task = mTaskList.get(i);
            if (task != null && task.getName().equals(taskName)) {
                task.cancel(true);
                mTaskList.remove(task);
                return;
            }
        }
    }

    /**
     * 取消队列中所有的任务，包括执行中和未执行的; 清理缓存对象
     */
    public void cancelAll() {
        // 停止线程池
        if (mPool != null) {
            mPool.shutdownNow();
        }
        // 清除缓存的任务列表
        mTaskList.clear();
    }

    /**
     * 取消队列中所有未执行的任务
     */
    public void cancelAllUnexecuted() {
//        Iterator<Map.Entry<String, FutureTask<?>>> it = mTaskList.entrySet().iterator();
//        while(it.hasNext()){
//            Map.Entry<String, FutureTask<?>> entry = it.next();
//            FutureTask<?> futureTask = (FutureTask<?>)entry.getValue();
//            if ( !futureTask.isDone() ) {
//                futureTask.cancel(false);
//                //map.put(key, "奇数");   //ConcurrentModificationException
//                //map.remove(key);      //ConcurrentModificationException
//                it.remove();        //OK
//            }
//        }

        for (int i=mTaskList.size()-1; i>=0; i--) {
            Task<?> task = mTaskList.get(i);
            if (task != null && !task.isDone()) {
                task.cancel(false);
                mTaskList.remove(i);
            }
        }

    }

    /**
     * 设置队列完成时的回调
     * @param callback 回调函数
     */
    public void setQueueFinishedCallback(IQueueFinishedCallback callback) {
        if (callback == null) return;

        mQueueFinishedCallback = callback;


    }

    private void queueFinished() {
        --mAddingTaskCount;
        if (mAddingTaskCount < 1) {
            stop();
        }
    }


}
