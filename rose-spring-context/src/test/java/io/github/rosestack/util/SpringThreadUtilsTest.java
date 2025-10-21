package io.github.rosestack.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.github.rosestack.spring.util.SpringThreadUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * SpringThreadUtils 测试类 展示基于 Spring 框架的线程工具类的各种使用场景
 */
@Slf4j
class SpringThreadUtilsTest {

    // ==================== 基础功能测试 ====================

    @Test
    void testSleep() {
        long startTime = System.currentTimeMillis();
        SpringThreadUtils.sleep(100);
        long endTime = System.currentTimeMillis();

        assertTrue(endTime - startTime >= 90, "Sleep time should be at least 90ms");
        assertTrue(endTime - startTime <= 200, "Sleep time should be less than 200ms");
    }

    @Test
    void testSleepWithZeroOrNegative() {
        long startTime = System.currentTimeMillis();
        SpringThreadUtils.sleep(0);
        SpringThreadUtils.sleep(-100);
        long endTime = System.currentTimeMillis();

        assertTrue(endTime - startTime < 50, "Should not sleep for zero or negative values");
    }

    @Test
    void testSleepInterruption() {
        Thread testThread = new Thread(() -> {
            SpringThreadUtils.sleep(5000); // 长时间睡眠
            // 验证中断状态被正确恢复
            assertTrue(Thread.currentThread().isInterrupted(), "Thread should be interrupted");
        });

        testThread.start();
        SpringThreadUtils.sleep(100);
        testThread.interrupt();

        assertDoesNotThrow(() -> {
            testThread.join(1000);
            assertFalse(testThread.isAlive(), "Thread should have finished");
        });
    }

    // ==================== ThreadPoolTaskExecutor 测试 ====================

    @Test
    void testCreateThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = SpringThreadUtils.createThreadPoolTaskExecutor(2, 4, 10, "test-");

        assertNotNull(executor);
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(4, executor.getMaxPoolSize());
        assertEquals(10, executor.getQueueCapacity());
        assertEquals("test-", executor.getThreadNamePrefix());

        // 测试线程池状态
        String status = SpringThreadUtils.getThreadPoolStatus(executor);
        assertNotNull(status);
        assertTrue(status.contains("test-"));
        assertTrue(status.contains("Active: 0"));

        executor.shutdown();
    }

    @Test
    void testShutdownAndAwaitTermination() {
        ThreadPoolTaskExecutor executor = SpringThreadUtils.createThreadPoolTaskExecutor(2, 2, 5, "shutdown-test-");

        // 提交一些任务
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                SpringThreadUtils.sleep(50);
                counter.incrementAndGet();
            });
        }

        // 关闭线程池
        executor.shutdown();

        assertEquals(5, counter.get(), "All tasks should have completed");
    }

    // ==================== 异步执行测试 ====================

    @Test
    void testSubmitCompletable() throws Exception {
        ThreadPoolTaskExecutor executor = SpringThreadUtils.createThreadPoolTaskExecutor(2, 4, 10, "completable-test-");

        try {
            // 测试正常任务
            CompletableFuture<Integer> future = SpringThreadUtils.submitCompletable(executor, () -> {
                SpringThreadUtils.sleep(100);
                return 42;
            });

            Integer result = future.get(1, TimeUnit.SECONDS);
            assertEquals(42, result);

            // 测试异常任务
            CompletableFuture<Void> exceptionFuture = SpringThreadUtils.submitCompletable(executor, () -> {
                throw new RuntimeException("Test exception");
            });

            assertThrows(ExecutionException.class, () -> exceptionFuture.get(1, TimeUnit.SECONDS));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testSubmitWithTimeout() throws Exception {
        ThreadPoolTaskExecutor executor = SpringThreadUtils.createThreadPoolTaskExecutor(2, 4, 10, "timeout-test-");

        try {
            // 测试正常超时
            Integer result = SpringThreadUtils.submitWithTimeout(
                    executor,
                    () -> {
                        SpringThreadUtils.sleep(100);
                        return 100;
                    },
                    1,
                    TimeUnit.SECONDS);

            assertEquals(100, result);

            // 测试超时异常
            assertThrows(TimeoutException.class, () -> {
                SpringThreadUtils.submitWithTimeout(
                        executor,
                        () -> {
                            SpringThreadUtils.sleep(2000);
                            return 200;
                        },
                        500,
                        TimeUnit.MILLISECONDS);
            });
        } finally {
            executor.shutdown();
        }
    }

    // ==================== 批量任务测试 ====================

    @Test
    void testSubmitBatch() throws Exception {
        ThreadPoolTaskExecutor executor = SpringThreadUtils.createThreadPoolTaskExecutor(4, 8, 20, "batch-test-");

        try {
            // 创建多个任务
            @SuppressWarnings("unchecked")
            CompletableFuture<Integer>[] futures = SpringThreadUtils.submitBatch(
                    executor,
                    () -> {
                        SpringThreadUtils.sleep(100);
                        return 1;
                    },
                    () -> {
                        SpringThreadUtils.sleep(150);
                        return 2;
                    },
                    () -> {
                        SpringThreadUtils.sleep(200);
                        return 3;
                    });

            // 等待所有任务完成
            Object[] results = SpringThreadUtils.waitForAll(futures);

            assertEquals(3, results.length);
            assertEquals(1, results[0]);
            assertEquals(2, results[1]);
            assertEquals(3, results[2]);
        } finally {
            executor.shutdown();
        }
    }

    // ==================== 任务创建测试 ====================

    @Test
    void testCreateCounterTask() throws Exception {
        ThreadPoolTaskExecutor executor = SpringThreadUtils.createThreadPoolTaskExecutor(2, 4, 10, "counter-test-");

        try {
            // 创建计数器任务
            var counterTask = SpringThreadUtils.createCounterTask("TestCounter", 3, 50);
            CompletableFuture<Integer> future = SpringThreadUtils.submitCompletable(executor, counterTask);

            Integer result = future.get(1, TimeUnit.SECONDS);
            assertEquals(3, result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testCreateExceptionTask() throws Exception {
        ThreadPoolTaskExecutor executor = SpringThreadUtils.createThreadPoolTaskExecutor(2, 4, 10, "exception-test-");

        try {
            // 创建异常任务
            RuntimeException testException = new RuntimeException("Test exception");
            var exceptionTask = SpringThreadUtils.createExceptionTask("TestException", 100, testException);
            CompletableFuture<Void> future = SpringThreadUtils.submitCompletable(executor, exceptionTask);

            // 验证异常被正确传播
            ExecutionException executionException =
                    assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
            assertEquals(testException, executionException.getCause());
        } finally {
            executor.shutdown();
        }
    }

    // ==================== 监控功能测试 ====================

    @Test
    void testGetThreadPoolStatus() {
        ThreadPoolTaskExecutor executor = SpringThreadUtils.createThreadPoolTaskExecutor(2, 4, 10, "status-test-");

        try {
            String status = SpringThreadUtils.getThreadPoolStatus(executor);
            assertNotNull(status);
            assertTrue(status.contains("status-test-"));
            assertTrue(status.contains("Active: 0"));
            assertTrue(status.contains("PoolSize: 0"));
            assertTrue(status.contains("CorePoolSize: 2"));
            assertTrue(status.contains("MaxPoolSize: 4"));
            assertTrue(status.contains("QueueSize: 0"));
            assertTrue(status.contains("CompletedTasks: 0"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testMonitorThreadPool() {
        ThreadPoolTaskExecutor executor = SpringThreadUtils.createThreadPoolTaskExecutor(2, 4, 10, "monitor-test-");

        try {
            // 提交一些任务
            for (int i = 0; i < 3; i++) {
                executor.submit(() -> {
                    SpringThreadUtils.sleep(200);
                    return null;
                });
            }

            // 监控线程池状态（短暂监控）
            assertDoesNotThrow(() -> {
                SpringThreadUtils.monitorThreadPool(executor, 100, 300);
            });
        } finally {
            executor.shutdown();
        }
    }

    // ==================== 异常处理测试 ====================

    @Test
    void testPrintException() {
        RuntimeException testException = new RuntimeException("Test exception");

        // 测试直接异常处理
        assertDoesNotThrow(() -> SpringThreadUtils.printException(null, testException));

        // 测试 Future 异常处理
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            throw new RuntimeException("Future exception");
        });

        SpringThreadUtils.sleep(100); // 等待 Future 完成

        assertDoesNotThrow(() -> {
            try {
                future.get();
            } catch (Exception e) {
                // 异常被正确捕获
                assertNotNull(e.getCause());
            }
        });
    }

    // ==================== 性能测试 ====================

    @Test
    void testConcurrentTaskExecution() throws Exception {
        ThreadPoolTaskExecutor executor = SpringThreadUtils.createThreadPoolTaskExecutor(4, 8, 50, "concurrent-test-");

        try {
            int taskCount = 20;
            AtomicInteger completedTasks = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(taskCount);

            long startTime = System.currentTimeMillis();

            // 提交多个并发任务
            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    try {
                        SpringThreadUtils.sleep(50);
                        completedTasks.incrementAndGet();
                        log.debug("Task {} completed", taskId);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 等待所有任务完成
            boolean allCompleted = latch.await(5, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();

            assertTrue(allCompleted, "All tasks should complete within timeout");
            assertEquals(taskCount, completedTasks.get(), "All tasks should be completed");
            assertTrue(endTime - startTime < 2000, "Tasks should complete efficiently");

            log.info("Completed {} tasks in {} ms", taskCount, endTime - startTime);
        } finally {
            executor.shutdown();
        }
    }

    // ==================== 缓存和状态测试 ====================

    @Test
    void testThreadPoolCacheAndStatus() {
        ThreadPoolTaskExecutor executor = SpringThreadUtils.createThreadPoolTaskExecutor(2, 4, 10, "cache-test-");

        try {
            // 初始状态
            String initialStatus = SpringThreadUtils.getThreadPoolStatus(executor);
            assertTrue(initialStatus.contains("Active: 0"));

            // 提交任务后状态
            executor.submit(() -> {
                SpringThreadUtils.sleep(500);
                return null;
            });

            SpringThreadUtils.sleep(100); // 等待任务开始执行

            String runningStatus = SpringThreadUtils.getThreadPoolStatus(executor);
            assertTrue(runningStatus.contains("Active: 1") || runningStatus.contains("Active: 0"));

            // 等待任务完成
            SpringThreadUtils.sleep(600);

            String finalStatus = SpringThreadUtils.getThreadPoolStatus(executor);
            assertTrue(finalStatus.contains("Active: 0"));
            assertTrue(finalStatus.contains("CompletedTasks: 1"));
        } finally {
            executor.shutdown();
        }
    }
}
