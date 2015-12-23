package io.askcuix.push.service.mipush;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.RateLimiter;
import io.askcuix.push.common.Constant;
import io.askcuix.push.payload.PayloadType;
import io.askcuix.push.thrift.OsType;
import io.askcuix.push.thrift.PushMessage;
import io.askcuix.push.util.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Chris on 15/12/10.
 */
@Component
public class MiPushQueueProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MiPushQueueProcessor.class);
    private static final Logger monitorLogger = LoggerFactory.getLogger(Constant.LOG_MONITOR);

    private static final long MONITOR_SEC = 60;

    @Autowired
    @Qualifier("androidMiBundle")
    private String androidBundle;

    @Autowired
    @Qualifier("miPushAndroidService")
    private MiPushService androidPushService;

    @Autowired
    @Qualifier("iOSMiBundle")
    private String iOSBundle;

    @Autowired
    @Qualifier("miPushiOSService")
    private MiPushService iOSPushService;

    @Value("${mipush.android.enable}")
    private boolean enableAndroidPush;

    @Value("${mipush.ios.enable}")
    private boolean enableiOSPush;

    private ExecutorService miPushExecutor;

    private AtomicLong processCounter = new AtomicLong(0);
    private AtomicLong reqCounter = new AtomicLong(0);
    private AtomicLong processTime = new AtomicLong(0);
    private ScheduledExecutorService monitorExecutor;

    private RateLimiter rateLimiter;

    @PostConstruct
    public void init() {
        if (!enableAndroidPush && !enableiOSPush) {
            return;
        }

        rateLimiter = RateLimiter.create(10000);

        miPushExecutor = new ThreadUtil.FixedThreadPoolBuilder().setThreadFactory(ThreadUtil.buildThreadFactory("MiPush-Queue"))
                .setPoolSize(1).setQueueSize(10000).setRejectHanlder(new ThreadPoolExecutor.DiscardOldestPolicy()).build();

        monitorExecutor = Executors.newSingleThreadScheduledExecutor(ThreadUtil.buildThreadFactory("MiPush-Monitor"));
        monitorExecutor.scheduleAtFixedRate(new ThreadUtil.WrapExceptionRunnable(new Runnable() {

            @Override
            public void run() {
                long count = processCounter.getAndSet(0);
                long reqCount = reqCounter.getAndSet(0);
                long processMs = processTime.getAndSet(0);
                monitorLogger.info("[MiPush] Total messages: {}, Process count: {}. Average process time per message: {}ms",
                        reqCount, count, (count == 0 ? 0 : (processMs / count)));
            }
        }), MONITOR_SEC, MONITOR_SEC, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        ThreadUtil.gracefulShutdown(monitorExecutor, 1000);

        ThreadUtil.gracefulShutdown(miPushExecutor, 5000);
    }

    public void putMessage(OsType os, List<String> deviceList, PushMessage message) {
        if ((os == OsType.Android && !enableAndroidPush) || (os == OsType.iOS && !enableiOSPush)) {
            logger.warn("[MiPush] Push system not enabled!");
            return;
        }

        MiPushPayload payload = new MiPushPayload();
        payload.setType(PayloadType.SEND);
        payload.setDeviceList(deviceList);
        payload.setOsType(os.getValue());
        payload.setExpiry(message.getExpiry());

        if (os == OsType.Android) {
            payload.setBundle(androidBundle);
        } else {
            payload.setBundle(iOSBundle);
        }
        payload.setPushMessage(message);

        logger.debug("Put MiPush payload to queue: {}", payload);

        process(os, payload);
    }

    public void putMulticast(OsType os, String topic, PushMessage message) {
        if ((os == OsType.Android && !enableAndroidPush) || (os == OsType.iOS && !enableiOSPush)) {
            logger.warn("[MiPush] Push system not enabled!");
            return;
        }

        MiPushPayload payload = new MiPushPayload();
        payload.setType(PayloadType.MULTICAST);
        payload.setTopic(topic);
        payload.setOsType(os.getValue());
        payload.setExpiry(message.getExpiry());

        if (os == OsType.Android) {
            payload.setBundle(androidBundle);
        } else {
            payload.setBundle(iOSBundle);
        }
        payload.setPushMessage(message);

        logger.debug("Put MiPush multicast payload to queue: {}", payload);

        process(os, payload);
    }

    public void putBroadcast(OsType os, PushMessage message) {
        if ((os == OsType.Android && !enableAndroidPush) || (os == OsType.iOS && !enableiOSPush)) {
            logger.warn("[MiPush] Push system not enabled!");
            return;
        }

        MiPushPayload payload = new MiPushPayload();
        payload.setType(PayloadType.BROADCAST);
        payload.setOsType(os.getValue());
        payload.setExpiry(message.getExpiry());

        if (os == OsType.Android) {
            payload.setBundle(androidBundle);
        } else {
            payload.setBundle(iOSBundle);
        }
        payload.setPushMessage(message);

        logger.debug("Put MiPush broadcast payload to queue: {}", payload);

        process(os, payload);
    }

    private void process(final OsType os, final MiPushPayload payload) {
        if (payload.getExpiry() > 0L && payload.getExpiry() < System.currentTimeMillis()) {
            logger.warn("Ignore to process expired message: {}", payload);
            return;
        }

        if (!rateLimiter.tryAcquire()) {
            monitorLogger.warn("[MiPush] Exceed rate limit: {}, discard message: {}", rateLimiter.getRate(), payload);
            return;
        }

        reqCounter.incrementAndGet();

        miPushExecutor.execute(new ThreadUtil.WrapExceptionRunnable(new Runnable() {
            @Override
            public void run() {
                processCounter.incrementAndGet();
                Stopwatch stopWatch = Stopwatch.createStarted();

                PayloadType type = payload.getType();
                switch (type) {
                    case SEND:
                        if (os == OsType.Android) {
                            androidPushService.sendToDevices(payload.getDeviceList(), payload.getMessage());
                        } else if (os == OsType.iOS) {
                            iOSPushService.sendToDevices(payload.getDeviceList(), payload.getMessage());
                        }

                        break;
                    case MULTICAST:
                        if (os == OsType.Android) {
                            androidPushService.broadcast(payload.getTopic(), payload.getMessage());
                        } else if (os == OsType.iOS) {
                            iOSPushService.broadcast(payload.getTopic(), payload.getMessage());
                        }

                        break;
                    case BROADCAST:
                        if (os == OsType.Android) {
                            androidPushService.broadcastAll(payload.getMessage());
                        } else if (os == OsType.iOS) {
                            iOSPushService.broadcastAll(payload.getMessage());
                        }

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
