package io.askcuix.push.service.ios;

import io.askcuix.push.common.Constant;
import io.askcuix.push.payload.PayloadMsgType;
import io.askcuix.push.payload.PayloadType;
import io.askcuix.push.thrift.MessageType;
import io.askcuix.push.thrift.PushMessage;
import io.askcuix.push.util.ThreadUtil;
import javapns.notification.Payload;
import javapns.notification.PushNotificationBigPayload;
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
        APNsPayload payload = new APNsPayload();
        payload.setType(PayloadType.SEND);
        payload.setDeviceList(deviceList);
        payload.setMsgType(PayloadMsgType.findByValue(message.getMsgType().getValue()));

        Payload msg = buildMessage(message);
        payload.setMessage(msg);

        logger.debug("Put APNs payload to queue: {}", payload);

        process(payload);
    }

    public void putMulticast(String topic, PushMessage message) {
        APNsPayload payload = new APNsPayload();
        payload.setType(PayloadType.MULTICAST);
        payload.setTopic(topic);
        payload.setMsgType(PayloadMsgType.findByValue(message.getMsgType().getValue()));

        Payload msg = buildMessage(message);
        payload.setMessage(msg);

        logger.debug("Put APNs multicast payload to queue: {}", payload);

        process(payload);
    }

    public void putBroadcast(PushMessage message) {
        APNsPayload payload = new APNsPayload();
        payload.setType(PayloadType.BROADCAST);
        payload.setMsgType(PayloadMsgType.findByValue(message.getMsgType().getValue()));

        Payload msg = buildMessage(message);
        payload.setMessage(msg);

        logger.debug("Put APNs broadcast payload to queue: {}", payload);

        process(payload);
    }

    private void process(final APNsPayload payload) {
        if (!enablePush) {
            logger.warn("[APNs] Push system not enabled!");
            return;
        }

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

    /**
     * 构造iOS通知消息。
     *
     * @param pushMessage
     * @return ios message
     */
    private Payload buildMessage(PushMessage pushMessage) {
        PushNotificationBigPayload payload = null;

        try {
            /* Build a blank payload to customize, max payload is 2048byte */
            payload = PushNotificationBigPayload.complex();

            /* Customize the payload */
            if (pushMessage.getMsgType() == MessageType.Notification) {
                payload.addAlert(pushMessage.getDesc());
                payload.addSound("default");
            }

            payload.addCustomDictionary(Constant.MESSAGE_DATA_KEY, pushMessage.getData());

            if (pushMessage.getExpiry() > 0L) {
                payload.setExpiry(new Long(pushMessage.getExpiry() / 1000).intValue());
            }
        } catch (Exception e) {
            logger.error("Fail to build ios notification message.", e);
            payload = null;
        }

        return payload;
    }
}
