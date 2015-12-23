package io.askcuix.push.service.ios;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.RateLimiter;
import io.askcuix.push.common.Constant;
import io.askcuix.push.payload.PayloadType;
import io.askcuix.push.thrift.PushMessage;
import io.askcuix.push.util.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Chris on 15/12/9.
 */
@Component
public class ApplePushQueueProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ApplePushQueueProcessor.class);
    private static final Logger monitorLogger = LoggerFactory.getLogger(Constant.LOG_MONITOR);

    private static final long MONITOR_SEC = 60;

    @Autowired
    private ApplePushService pushService;

    @Value("${apns.enable}")
    private boolean enablePush;

    private ExecutorService apnsExecutor;

    private AtomicLong processCounter = new AtomicLong(0);
    private AtomicLong reqCounter = new AtomicLong(0);
    private AtomicLong processTime = new AtomicLong(0);
    private ScheduledExecutorService monitorExecutor;

    private RateLimiter rateLimiter;

    @PostConstruct
    public void init() {
        if (!enablePush) {
            return;
        }

        rateLimiter = RateLimiter.create(10000);

        apnsExecutor = new ThreadUtil.FixedThreadPoolBuilder().setThreadFactory(ThreadUtil.buildThreadFactory("APNs-Queue"))
                .setPoolSize(5).setQueueSize(10000).setRejectHanlder(new ThreadPoolExecutor.DiscardOldestPolicy()).build();

        monitorExecutor = Executors.newSingleThreadScheduledExecutor(ThreadUtil.buildThreadFactory("APNs-Monitor"));
        monitorExecutor.scheduleAtFixedRate(new ThreadUtil.WrapExceptionRunnable(new Runnable() {

            @Override
            public void run() {
                long count = processCounter.getAndSet(0);
                long reqCount = reqCounter.getAndSet(0);
                long processMs = processTime.getAndSet(0);
                monitorLogger.info("[APNs] Total messages: {}, Process count: {}. Average process time per message: {}ms",
                        reqCount, count, (count == 0 ? 0 : (processMs / count)));
            }
        }), MONITOR_SEC, MONITOR_SEC, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        ThreadUtil.gracefulShutdown(monitorExecutor, 1000);

        ThreadUtil.gracefulShutdown(apnsExecutor, 5000);
    }

    public void putMessage(List<String> deviceList, PushMessage message) {
        if (!enablePush) {
            logger.warn("[APNs] Push system not enabled!");
            return;
        }

        APNsPayload payload = new APNsPayload();
        payload.setType(PayloadType.SEND);
        payload.setDeviceList(deviceList);
        payload.setExpiry(message.getExpiry());

        try {
            payload.setPushMessage(message);
        } catch (Exception e) {
            logger.error("Construct payload from push message error, ignore to send.", e);
            return;
        }

        logger.debug("Put APNs payload to queue: {}", payload);

        process(payload);
    }

    public void putMulticast(String topic, PushMessage message) {
        if (!enablePush) {
            logger.warn("[APNs] Push system not enabled!");
            return;
        }

        APNsPayload payload = new APNsPayload();
        payload.setType(PayloadType.MULTICAST);
        payload.setTopic(topic);
        payload.setExpiry(message.getExpiry());

        try {
            payload.setPushMessage(message);
        } catch (Exception e) {
            logger.error("Construct payload from push message error, ignore to send.", e);
            return;
        }

        logger.debug("Put APNs multicast payload to queue: {}", payload);

        process(payload);
    }

    public void putBroadcast(PushMessage message) {
        if (!enablePush) {
            logger.warn("[APNs] Push system not enabled!");
            return;
        }

        APNsPayload payload = new APNsPayload();
        payload.setType(PayloadType.BROADCAST);
        payload.setExpiry(message.getExpiry());

        try {
            payload.setPushMessage(message);
        } catch (Exception e) {
            logger.error("Construct payload from push message error, ignore to send.", e);
            return;
        }

        logger.debug("Put APNs broadcast payload to queue: {}", payload);

        process(payload);
    }

    private void process(final APNsPayload payload) {
        if (payload.getExpiry() > 0L && payload.getExpiry() < System.currentTimeMillis()) {
            logger.warn("Ignore to process expired message: {}", payload);
            return;
        }

        if (!rateLimiter.tryAcquire()) {
            monitorLogger.warn("[MiPush] Exceed rate limit: {}, discard message: {}", rateLimiter.getRate(), payload);
            return;
        }

        reqCounter.incrementAndGet();

        apnsExecutor.execute(new ThreadUtil.WrapExceptionRunnable(new Runnable() {
            @Override
            public void run() {
                processCounter.incrementAndGet();
                Stopwatch stopWatch = Stopwatch.createStarted();

                PayloadType type = payload.getType();
                switch (type) {
                    case SEND:
                        pushService.sendToDevices(payload.getDeviceList(), payload.getMessage());
                        break;
                    case MULTICAST:
                        pushService.broadcast(payload.getTopic(), payload.getMsgType(), payload.getMessage());
                        break;
                    case BROADCAST:
                        pushService.broadcastAll(payload.getMsgType(), payload.getMessage());
                        break;
                    default:
                        logger.warn("Invalid message: {}", payload);
                        break;
                }

                stopWatch.stop();
                processTime.addAndGet(stopWatch.elapsed(TimeUnit.MILLISECONDS));
            }
        }));

    }

}
