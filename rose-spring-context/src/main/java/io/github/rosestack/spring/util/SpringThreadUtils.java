package io.github.rosestack.spring.util;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于 Spring 框架的线程工具类 提供更强大和灵活的线程管理功能，替代原有的 ThreadUtils
 */
@Slf4j
public class SpringThreadUtils {

    /**
     * 线程睡眠等待（毫秒） 改进异常处理，恢复中断状态
     *
     * @param milliseconds 睡眠时间（毫秒）
     */
    public static void sleep(long milliseconds) {
        if (milliseconds <= 0) {
            return;
        }
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            log.debug("Thread sleep interrupted");
            // 恢复中断状态
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 打印线程异常信息 增强的异常处理，支持 Spring 的 ListenableFuture
     *
     * @param runnable  可运行对象
     * @param throwable 异常
     */
    public static void printException(Runnable runnable, Throwable throwable) {
        if (throwable == null && runnable instanceof Future<?>) {
            try {
                Future<?> future = (Future<?>) runnable;
                if (future.isDone()) {
                    future.get();
                }
            } catch (CancellationException ce) {
                throwable = ce;
            } catch (ExecutionException ee) {
                throwable = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        if (throwable != null) {
            log.error("线程执行异常: {}", throwable.getMessage(), throwable);
        }
    }

    /**
     * 创建 Spring 的 ThreadPoolTaskExecutor 提供更灵活的线程池配置
     *
     * @param corePoolSize     核心线程数
     * @param maxPoolSize      最大线程数
     * @param queueCapacity    队列容量
     * @param threadNamePrefix 线程名前缀
     * @return ThreadPoolTaskExecutor
     */
    public static ThreadPoolTaskExecutor createThreadPoolTaskExecutor(
            int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }

    /**
     * 异步执行任务，返回 CompletableFuture
     *
     * @param executor 线程池执行器
     * @param task     任务
     * @param <T>      返回类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> submitCompletable(ThreadPoolTaskExecutor executor, Supplier<T> task) {

        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                T result = task.get();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * 带超时的异步执行
     *
     * @param executor 线程池执行器
     * @param task     任务
     * @param timeout  超时时间
     * @param unit     时间单位
     * @param <T>      返回类型
     * @return 执行结果
     * @throws TimeoutException     超时异常
     * @throws ExecutionException   执行异常
     * @throws InterruptedException 中断异常
     */
    public static <T> T submitWithTimeout(
            ThreadPoolTaskExecutor executor, Supplier<T> task, long timeout, TimeUnit unit)
            throws TimeoutException, ExecutionException, InterruptedException {

        Future<T> future = executor.submit(task::get);
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    /**
     * 批量异步执行任务
     *
     * @param executor 线程池执行器
     * @param tasks    任务列表
     * @param <T>      返回类型
     * @return CompletableFuture 列表
     */
    public static <T> CompletableFuture<T>[] submitBatch(ThreadPoolTaskExecutor executor, Supplier<T>... tasks) {

        @SuppressWarnings("unchecked")
        CompletableFuture<T>[] futures = new CompletableFuture[tasks.length];

        for (int i = 0; i < tasks.length; i++) {
            futures[i] = submitCompletable(executor, tasks[i]);
        }

        return futures;
    }

    /**
     * 等待所有任务完成
     *
     * @param futures CompletableFuture 数组
     * @param <T>     返回类型
     * @return 结果数组
     * @throws InterruptedException 中断异常
     * @throws ExecutionException   执行异常
     */
    public static <T> T[] waitForAll(CompletableFuture<T>... futures) throws InterruptedException, ExecutionException {

        @SuppressWarnings("unchecked")
        T[] results = (T[]) new Object[futures.length];

        for (int i = 0; i < futures.length; i++) {
            results[i] = futures[i].get();
        }

        return results;
    }

    /**
     * 获取线程池状态信息
     *
     * @param executor 线程池执行器
     * @return 状态信息字符串
     */
    public static String getThreadPoolStatus(ThreadPoolTaskExecutor executor) {
        if (executor == null) {
            return "Executor is null";
        }

        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        return String.format(
                "ThreadPool[%s] - Active: %d, PoolSize: %d, CorePoolSize: %d, MaxPoolSize: %d, QueueSize: %d, CompletedTasks: %d",
                executor.getThreadNamePrefix(),
                threadPoolExecutor.getActiveCount(),
                threadPoolExecutor.getPoolSize(),
                threadPoolExecutor.getCorePoolSize(),
                threadPoolExecutor.getMaximumPoolSize(),
                threadPoolExecutor.getQueue().size(),
                threadPoolExecutor.getCompletedTaskCount());
    }

    /**
     * 监控线程池状态
     *
     * @param executor   线程池执行器
     * @param intervalMs 监控间隔（毫秒）
     * @param durationMs 监控持续时间（毫秒）
     */
    public static void monitorThreadPool(ThreadPoolTaskExecutor executor, long intervalMs, long durationMs) {
        if (executor == null) {
            return;
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + durationMs;

        while (System.currentTimeMillis() < endTime) {
            log.info(getThreadPoolStatus(executor));
            sleep(intervalMs);
        }
    }

    /**
     * 创建计数器任务 用于测试和演示
     *
     * @param name    任务名称
     * @param count   计数次数
     * @param delayMs 每次计数间隔（毫秒）
     * @return 任务
     */
    public static Supplier<Integer> createCounterTask(String name, int count, long delayMs) {
        return () -> {
            AtomicInteger counter = new AtomicInteger(0);
            for (int i = 0; i < count; i++) {
                log.debug("{} - Count: {}", name, counter.incrementAndGet());
                sleep(delayMs);
            }
            return counter.get();
        };
    }

    /**
     * 创建异常任务 用于测试异常处理
     *
     * @param name      任务名称
     * @param delayMs   延迟时间（毫秒）
     * @param exception 要抛出的异常
     * @return 任务
     */
    public static Supplier<Void> createExceptionTask(String name, long delayMs, RuntimeException exception) {
        return () -> {
            log.debug("{} - Starting exception task", name);
            sleep(delayMs);
            throw exception;
        };
    }
}
