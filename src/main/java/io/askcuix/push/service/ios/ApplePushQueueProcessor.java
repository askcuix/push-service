package io.askcuix.push.service.ios;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by Chris on 15/12/9.
 */
@Component
public class ApplePushQueueProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ApplePushQueueProcessor.class);

    @Autowired
    private ApplePushService pushService;

    @Value("${apns.enable}")
    private boolean enablePush;

    private ExecutorService apnsExecutor;

    @PostConstruct
    public void init() {
        if (!enablePush) {
            return;
        }

        apnsExecutor = new ThreadUtil.FixedThreadPoolBuilder().setThreadFactory(ThreadUtil.buildThreadFactory("APNs-Queue"))
                .setPoolSize(5).setQueueSize(10000).build();
    }

    @PreDestroy
    public void destroy() {
        if (apnsExecutor == null) {
            return;
        }

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
        try {
            apnsExecutor.execute(new ThreadUtil.WrapExceptionRunnable(new Runnable() {
                @Override
                public void run() {
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
                }
            }));
        } catch (RejectedExecutionException e) {
            logger.warn("[APNs] Rejected push payload: {}. Error Message: {}", payload, e.getMessage());
        }
    }

}
