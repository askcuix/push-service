package io.askcuix.push.service.mipush;

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
        if ((os == OsType.Android && !enableAndroidPush) || (os == OsType.iOS && !enableiOSPush)) {
            logger.warn("[MiPush] Push system not enabled!");
            return;
        }

        MiPushPayload payload = new MiPushPayload();
        payload.setType(PayloadType.SEND);
        payload.setDeviceList(deviceList);
        payload.setOsType(os.getValue());

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
        try {
            miPushExecutor.execute(new ThreadUtil.WrapExceptionRunnable(new Runnable() {
                @Override
                public void run() {
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
                }
            }));
        } catch (RejectedExecutionException e) {
            logger.warn("[MiPush] Rejected push payload: {}. Error Message: {}", payload, e.getMessage());
        }

    }


}
