package com.arwer.artaskqueue;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.arwer.arlibrary.threads.TaskQueue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by jun on 16/6/20.
 */
public class TaskQueueAppTest extends ApplicationTestCase<Application> {

    boolean isQueueFinished = false;

    public TaskQueueAppTest() {
        super(Application.class);
    }

    /**
     * 测试任务队列，1个线程串行执行任务
     */
    public void testTaskQueue() {
        try {
            System.out.println(">>> ===== 测试开始 =====");
            isQueueFinished = false;

            // 实例化任务队列对象
            final TaskQueue taskQueue = new TaskQueue();
            // 设置并发线程数量
            taskQueue.setMaxConcurrentThreadCount(1);
            // 添加第1个任务
            taskQueue.add("task1", new Callable<String>() {
                String resultStr = null;
                @Override
                public String call() throws Exception {
                    System.out.println(">>> task1 start execute");

                    String resultStr = httpGet("http://www.baidu.com");

                    System.out.println(">>> task1 finished time: " + (new Date().toString()));
                    return resultStr;
                }
            });
            // 添加第2个任务
            taskQueue.add("task2", new Callable<String>() {
                String resultStr = null;
                @Override
                public String call() throws Exception {
                    System.out.println(">>> task2 start execute");

                    try {
                        resultStr = httpGet("http://www.163.com");
                    }
                    catch (Exception e) {
                        // 如果任务2执行出错则取消后面的任务执行，队列将结束
                        taskQueue.cancelAllUnexecuted();
                    }
                    System.out.println(">>> task2 finished time: " + (new Date().toString()));
                    return resultStr;
                }
            });
            // 添加第3个任务
            taskQueue.add("task3", new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    System.out.println(">>> task3 start execute");
                    Thread.sleep(2000);
                    System.out.println(">>> task3 finished time: " + (new Date().toString()));
                    return Integer.valueOf(1+1);
                }
            });
            // 设置整个队列完成后的回调
            taskQueue.setQueueFinishedCallback(new TaskQueue.IQueueFinishedCallback() {
                @Override
                public void onFinished(Map<String, Object> result) {
                    System.out.println(">>> All task finished, total=" + result.size());
                    for (Map.Entry entry : result.entrySet()) {
//                        System.out.println(">>> TaskQueue finished, taskName=" + entry.getKey() + ", value=" + (entry.getValue()==null ? "null" : entry.getValue().toString()) );
                        System.out.println(">>> taskName=" + entry.getKey() + ", value=" + entry.getValue());
                    }

                    isQueueFinished = true;
                }
            });
            // 开始队列
            taskQueue.start();


            // 阻塞单元测试进程，等待所有任务完成再退出
            while (!isQueueFinished) {
//                Thread.sleep(2000);
            }
            System.out.println(">>> ===== 测试结束 =====");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试任务队列，多个线程并发执行任务
     */
    public void testTaskQueueMutilThread() {
        try {
            System.out.println(">>> ===== 测试开始 =====");
            isQueueFinished = false;

            // 实例化任务队列对象
            final TaskQueue taskQueue = new TaskQueue();
            // 设置并发线程数量
            taskQueue.setMaxConcurrentThreadCount(4);
            // 添加第1个任务
            taskQueue.add("task1", new Callable<String>() {
                String resultStr = null;
                @Override
                public String call() throws Exception {
                    System.out.println(">>> task1 start execute");

                    String resultStr = httpGet("http://www.baidu.com");

                    System.out.println(">>> task1 finished time: " + (new Date().toString()));
                    return resultStr;
                }
            });
            // 添加第2个任务
            taskQueue.add("task2", new Callable<String>() {
                String resultStr = null;
                @Override
                public String call() throws Exception {
                    System.out.println(">>> task2 start execute");

                    try {
                        resultStr = httpGet("http://www.163.com");
                    }
                    catch (Exception e) {
                        // 如果任务2执行出错则取消后面的任务执行，队列将结束
                        taskQueue.cancelAllUnexecuted();
                    }
                    System.out.println(">>> task2 finished time: " + (new Date().toString()));
                    return resultStr;
                }
            });
            // 添加第3个任务
            taskQueue.add("task3", new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    System.out.println(">>> task3 start execute");
                    System.out.println(">>> task3 finished time: " + (new Date().toString()));
                    return Integer.valueOf(1+1);
                }
            });
            // 添加第4个任务
            taskQueue.add("task4", new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    System.out.println(">>> task4 start execute");
                    System.out.println(">>> task4 finished time: " + (new Date().toString()));
                    return Integer.valueOf(9*9);
                }
            });
            // 设置整个队列完成后的回调
            taskQueue.setQueueFinishedCallback(new TaskQueue.IQueueFinishedCallback() {
                @Override
                public void onFinished(Map<String, Object> result) {
                    System.out.println(">>> All task finished, total=" + result.size());
                    for (Map.Entry entry : result.entrySet()) {
//                        System.out.println(">>> TaskQueue finished, taskName=" + entry.getKey() + ", value=" + (entry.getValue()==null ? "null" : entry.getValue().toString()) );
                        System.out.println(">>> taskName=" + entry.getKey() + ", value=" + entry.getValue());
                    }

                    isQueueFinished = true;
                }
            });
            // 开始队列
            taskQueue.start();



            // 阻塞单元测试进程，等待所有任务完成再退出
            while (!isQueueFinished) {
                Thread.sleep(2000);
            }
            System.out.println(">>> ===== 测试结束 =====");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String httpGet(String urlString) throws Exception {

        HttpURLConnection connection = null;
        try {
            // 创建HttpURLConnection对象
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setReadTimeout(30*1000); //设置从主机读取数据超时（单位：毫秒）
            connection.setConnectTimeout(30*1000); //设置连接主机超时（单位：毫秒）

            // 发送请求
            connection.connect(); //和远程资源建立真正的连接，但尚无返回的数据流

            // 得到响应
//            if (connection.getResponseCode() == 200) {
            InputStream is = connection.getInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int len = 0;
            byte buffer[] = new byte[1024];
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            is.close();
            os.close();

            String resultStr = new String(os.toByteArray());
            System.out.print(resultStr);
            return resultStr != null ? resultStr.substring(0, 50) + "..." : null;
//            }
//            else {
//                System.out.print(">>> 请求失败:" + connection.getResponseMessage());
//                callback.onFailure(connection.getResponseMessage());
//            }
        }
        catch (Exception e) {
            System.out.print(e.getMessage());
            e.printStackTrace();
        }
        finally {
            connection.disconnect();
        }
        return null;
    }

}