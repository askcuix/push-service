package io.askcuix.push.service.mipush;

import com.xiaomi.xmpush.server.Message;
import io.askcuix.push.common.Constant;
import io.askcuix.push.payload.PayloadMsgType;
import io.askcuix.push.payload.PayloadType;
import io.askcuix.push.thrift.MessageType;
import io.askcuix.push.thrift.OsType;
import io.askcuix.push.thrift.PushMessage;
import io.askcuix.push.util.ThreadUtil;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by Chris on 15/12/10.
 */
@Component
public class MiPushQueueProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MiPushQueueProcessor.class);

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

    @PostConstruct
    public void init() {
        if (!enableAndroidPush && !enableiOSPush) {
            return;
        }

        miPushExecutor = new ThreadUtil.FixedThreadPoolBuilder().setThreadFactory(ThreadUtil.buildThreadFactory("MiPush-Queue"))
                .setPoolSize(1).setQueueSize(10000).build();
    }

    @PreDestroy
    public void destroy() {
        if (miPushExecutor == null) {
            return;
        }

        ThreadUtil.gracefulShutdown(miPushExecutor, 5000);
    }

    public void putMessage(OsType os, List<String> deviceList, PushMessage message) {
        MiPushPayload payload = new MiPushPayload();
        payload.setType(PayloadType.SEND);
        payload.setDeviceList(deviceList);
        payload.setMsgType(PayloadMsgType.findByValue(message.getMsgType().getValue()));

        Message miMsg = buildMessage(os, message);
        payload.setMessage(miMsg);

        logger.debug("Put MiPush payload to queue: {}", payload);

        process(os, payload);
    }

    public void putMulticast(OsType os, String topic, PushMessage message) {
        MiPushPayload payload = new MiPushPayload();
        payload.setType(PayloadType.MULTICAST);
        payload.setTopic(topic);
        payload.setMsgType(PayloadMsgType.findByValue(message.getMsgType().getValue()));

        Message miMsg = buildMessage(os, message);
        payload.setMessage(miMsg);

        logger.debug("Put MiPush multicast payload to queue: {}", payload);

        process(os, payload);
    }

    public void putBroadcast(OsType os, PushMessage message) {
        MiPushPayload payload = new MiPushPayload();
        payload.setType(PayloadType.BROADCAST);
        payload.setMsgType(PayloadMsgType.findByValue(message.getMsgType().getValue()));

        Message miMsg = buildMessage(os, message);
        payload.setMessage(miMsg);

        logger.debug("Put MiPush broadcast payload to queue: {}", payload);

        process(os, payload);
    }

    private void process(final OsType os, final MiPushPayload payload) {
        if (!enableAndroidPush && !enableiOSPush) {
            logger.warn("[MiPush] Push system not enabled!");
            return;
        }

        try {
            miPushExecutor.execute(new ThreadUtil.WrapExceptionRunnable(new Runnable() {
                @Override
                public void run() {
                    PayloadType type = payload.getType();
                    switch (type) {
                        case SEND:
                            if (os == OsType.Android && enableAndroidPush) {
                                androidPushService.sendToDevices(payload.getDeviceList(), payload.getMessage());
                            } else if (os == OsType.iOS && enableiOSPush){
                                iOSPushService.sendToDevices(payload.getDeviceList(), payload.getMessage());
                            }

                            break;
                        case MULTICAST:
                            if (os == OsType.Android && enableAndroidPush) {
                                androidPushService.broadcast(payload.getTopic(), payload.getMessage());
                            } else if (os == OsType.iOS && enableiOSPush){
                                iOSPushService.broadcast(payload.getTopic(), payload.getMessage());
                            }

                            break;
                        case BROADCAST:
                            if (os == OsType.Android && enableAndroidPush) {
                                androidPushService.broadcastAll(payload.getMessage());
                            } else if (os == OsType.iOS && enableiOSPush){
                                iOSPushService.broadcastAll(payload.getMessage());
                            }

                            break;
                        default:
                            logger.warn("Invalid message: {}", payload);
                            break;
                    }
                }
            }));
        } catch (RejectedExecutionException e) {
            logger.warn("[MiPush] Rejected push payload: {}. Error Message: {}", payload, e.getMessage());
        }

    }

    /**
     * 构造android通知消息。
     *
     * @param pushMessage
     * @return android message
     */
    private Message buildMessage(OsType os, PushMessage pushMessage) {
        if (pushMessage == null) {
            return null;
        }

        Map<String, String> dataMap = new HashMap<String, String>();
        dataMap.put(Constant.MESSAGE_DATA_KEY, pushMessage.getData());

        JSONObject dataJson = new JSONObject(dataMap);
        String payload = dataJson.toJSONString();

        Message.Builder builder = new Message.Builder().payload(payload) // 数据
                .passThrough(pushMessage.getMsgType().getValue()); // 消息通知方式

        if (os == OsType.Android) {
            builder.restrictedPackageName(androidBundle); // 包名
        } else {
            builder.restrictedPackageName(iOSBundle); // 包名
        }

        if (pushMessage.getMsgType() == MessageType.Notification) {
            builder.title(pushMessage.getTitle()); // 通知栏标题
            builder.description(pushMessage.getDesc()); // 通知栏描述
            builder.notifyType(-1); // 使用全部提示
        }

        if (pushMessage.getExpiry() > 0L) {
            builder.timeToLive(pushMessage.getExpiry());
        }

        return builder.build();
    }
}
