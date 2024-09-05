package com.edu.yudada.utils;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author : Yinghao Zhang
 * @Date : 2024/8/27 16:35
 * @Version : V1.2
 * @Description : 测试自定义线程池的方法
 */
    public class CustomThreadPool {

        public static void main(String[] args) {
            ExecutorService executorService = new ThreadPoolExecutor(
                    5,
                    10,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),// 一个容量为10的工作队列
                    new CustomThreadFactory()// 自定义的线程工厂
            );
            ExecutorService executorService1 = Executors.newScheduledThreadPool(10,new CustomThreadFactory());

            for (int i = 0; i < 20; i++) {
                executorService.execute(() -> {
                    System.out.println(Thread.currentThread().getName() + " is running");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }

            executorService.shutdown();
        }

        static class CustomThreadFactory implements ThreadFactory {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "custom-thread-" + threadNumber.getAndIncrement());
                t.setDaemon(false);
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                return t;
            }
        }
    }
