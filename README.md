ARTaskQueue
===========

ARTaskQueue是一个基于Java ExecutorService的线程池包装类，对上层代码提供简单易用的接口方法，使上层代码聚焦业务而无需繁琐实现线程池的细节代码。  
<br/><br/>
ARTaskQueue现有的功能：

1、管理并发线程，可以1个线程串行，也可以自定义并发线程数；

2、可以添加任务，执行任意Callable任务，比如网络存取、数据库存取、复杂计算等等；

3、可以对某一任务执行取消、得到状态/执行结果；可以取消未执行的任务；可以手动控制队列开始/停止；

4、队列中所有任务完成后有回调，可以得到每个任务的执行状况以及返回值。


如何使用？
========
Android
-------
```
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

    }
});
// 开始队列
taskQueue.start();
```



注：更多例子请参考源码工程下的单元测试例子“TaskQueueUnitTest.java” 或 “TaskQueueAppTest.java”。



联系方式
=======
ArwerSoftware@gmail.com




License
=======
The MIT License (MIT)

Copyright © 2016 LongJun

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
