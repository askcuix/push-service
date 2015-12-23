package io.askcuix.push.util;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Created by Chris on 15/12/3.
 */
public class ThreadUtil {

    private static RejectedExecutionHandler defaultRejectHandler = new ThreadPoolExecutor.AbortPolicy();

    /**
     * sleep等待, 单位为毫秒, 已捕捉并处理InterruptedException.
     */
    public static void sleep(long durationMillis) {
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * sleep等待，已捕捉并处理InterruptedException.
     */
    public static void sleep(long duration, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(duration));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 先使用shutdown, 停止接收新任务并尝试完成所有已存在任务.
     * <p>
     * 如果1/2超时时间后, 则调用shutdownNow,取消在workQueue中Pending的任务,并中断所有阻塞函数.
     * <p>
     * 如果1/2超时仍然超時，则强制退出.
     * <p>
     * 另对在shutdown时线程本身被调用中断做了处理.
     * <p>
     * 返回线程最后是否被中断.
     */
    public static boolean gracefulShutdown(ExecutorService threadPool, int shutdownTimeoutMills) {
        if (threadPool == null) {
            return true;
        }
        return MoreExecutors.shutdownAndAwaitTermination(threadPool, shutdownTimeoutMills, TimeUnit.MILLISECONDS);
    }

    /**
     * 创建ThreadFactory，使得创建的线程有自己的名字而不是默认的"pool-x-thread-y"
     * <p>
     * 格式如"mythread-%d"
     */
    public static ThreadFactory buildThreadFactory(String nameFormat) {
        return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
    }

    /**
     * 可设定是否daemon, daemon线程在主线程已执行完毕时, 不会阻塞应用不退出, 而非daemon线程则会阻塞.
     *
     * @see #buildThreadFactory(String)
     */
    public static ThreadFactory buildThreadFactory(String nameFormat, boolean daemon) {
        return new ThreadFactoryBuilder().setNameFormat(nameFormat).setDaemon(daemon).build();
    }

    /**
     * 保证不会有Exception抛出到线程池的Runnable包裹类，防止没有捕捉异常导致中断了线程池中的线程, 使得SchedulerService无法执行.
     */
    public static class WrapExceptionRunnable implements Runnable {

        private static Logger logger = LoggerFactory.getLogger(WrapExceptionRunnable.class);

        private Runnable runnable;

        public WrapExceptionRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } catch (Throwable e) {
                // catch any exception, because the scheduled thread will break if the exception thrown to outside.
                logger.error("Unexpected error occurred in task.", e);
            }
        }
    }


    /**
     * 创建FixedThreadPool.
     * <p>
     * 1. 任务提交时, 如果线程数还没达到poolSize即创建新线程并绑定任务(即poolSize次提交后线程总数必达到poolSize，不会重用之前的线程)
     *      - poolSize是必填项，不能忽略.
     * <p>
     * 2. 第poolSize次任务提交后, 新增任务放入Queue中, Pool中的所有线程从Queue中take任务执行.
     *      - Queue默认为无限长的LinkedBlockingQueue， 也可以设置queueSize换成有界的队列.
     *      - 如果使用有界队列, 当队列满了之后,会调用RejectHandler进行处理, 默认为AbortPolicy，抛出RejectedExecutionException异常.
     *        其他可选的Policy包括静默放弃当前任务(Discard)，放弃Queue里最老的任务(DisacardOldest)，或由主线程来直接执行(CallerRuns).
     * <p>
     * 3. 因为线程全部为core线程，所以不会在空闲回收.
     */
    public static class FixedThreadPoolBuilder {

        private int poolSize = 0;
        private int queueSize = 0;

        private ThreadFactory threadFactory = null;
        private RejectedExecutionHandler rejectHandler;

        public FixedThreadPoolBuilder setPoolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public FixedThreadPoolBuilder setQueueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        public FixedThreadPoolBuilder setThreadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public FixedThreadPoolBuilder setRejectHanlder(RejectedExecutionHandler rejectHandler) {
            this.rejectHandler = rejectHandler;
            return this;
        }

        public ExecutorService build() {
            if (poolSize < 1) {
                throw new IllegalArgumentException("size not set");
            }

            BlockingQueue<Runnable> queue = null;
            if (queueSize == 0) {
                queue = new LinkedBlockingQueue<Runnable>();
            } else {
                queue = new ArrayBlockingQueue<Runnable>(queueSize);
            }

            if (threadFactory == null) {
                threadFactory = Executors.defaultThreadFactory();
            }

            if (rejectHandler == null) {
                rejectHandler = defaultRejectHandler;
            }

            return new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, queue, threadFactory,
                    rejectHandler);
        }
    }

    /**
     * 创建CachedThreadPool.
     * <p>
     * 1. 任务提交时, 如果线程数还没达到minSize即创建新线程并绑定任务(即minSize次提交后线程总数必达到minSize，不会重用之前的线程)
     *      - minSize默认为0, 可进行设置保证有基本的线程处理请求.
     * <p>
     * 2. 第minSize次任务提交后, 新增任务提交进SynchronousQueue后，如果没有空闲线程立刻处理，则会创建新的线程, 直到总线程数达到上限.
     *      - maxSize默认为Integer.Max，可进行设置.
     *      - 如果设置了maxSize, 当总线程数达到上限, 会调用RejectHandler进行处理, 默认为AbortPolicy，抛出RejectedExecutionException异常.
     *        其他可选的Policy包括静默放弃当前任务(Discard)，或由主线程来直接执行(CallerRuns).
     * <p>
     * 3. minSize以上，maxSize以下的线程，如果在keepAliveTime中都poll不到任务执行将会被结束掉, keeAliveTime默认为60秒，可设置.
     */
    public static class CachedThreadPoolBuilder {

        private int minSize = 0;
        private int maxSize = Integer.MAX_VALUE;
        private int keepAliveSecs = 60;

        private ThreadFactory threadFactory = null;
        private RejectedExecutionHandler rejectHandler;

        public CachedThreadPoolBuilder setMinSize(int minSize) {
            this.minSize = minSize;
            return this;
        }

        public CachedThreadPoolBuilder setMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public CachedThreadPoolBuilder setKeepAliveSecs(int keepAliveSecs) {
            this.keepAliveSecs = keepAliveSecs;
            return this;
        }

        public CachedThreadPoolBuilder setThreadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public CachedThreadPoolBuilder setRejectHanlder(RejectedExecutionHandler rejectHandler) {
            this.rejectHandler = rejectHandler;
            return this;
        }

        public ExecutorService build() {

            if (threadFactory == null) {
                threadFactory = Executors.defaultThreadFactory();
            }

            if (rejectHandler == null) {
                rejectHandler = defaultRejectHandler;
            }

            return new ThreadPoolExecutor(minSize, maxSize, keepAliveSecs, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), threadFactory, rejectHandler);
        }
    }

}
