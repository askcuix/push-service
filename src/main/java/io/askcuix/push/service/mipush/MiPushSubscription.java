package io.askcuix.push.service.mipush;

import com.google.common.collect.Lists;
import com.xiaomi.push.sdk.ErrorCode;
import com.xiaomi.xmpush.server.Constants;
import com.xiaomi.xmpush.server.Result;
import com.xiaomi.xmpush.server.Subscription;
import io.askcuix.push.common.Constant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * Created by Chris on 15/12/10.
 */
public class MiPushSubscription {
    private static final Logger logger = LoggerFactory.getLogger(MiPushSubscription.class);
    private static final Logger subscribeLogger = LoggerFactory.getLogger(Constant.LOG_SUBSCRIBE);

    private Subscription miSubscription;
    private String deviceType;

    @Value("${dev.mode}")
    private boolean devMode;

    public MiPushSubscription(Subscription miSubscription, String deviceType) {
        this.miSubscription = miSubscription;
        this.deviceType = deviceType;
    }

    private void initEnv() {
        if (devMode && StringUtils.equalsIgnoreCase(deviceType, Constant.DEVICE_IOS)) {
            Constants.useSandbox();
        } else {
            Constants.useOfficial();
        }
    }

    public boolean subscribeToMiPush(final String topic, final String uid) {
        boolean subscribeResult = false;

        try {
            initEnv();

            Result result = miSubscription.subscribeTopicByAlias(topic, Lists.newArrayList(uid), null, 3);

            if (result == null) {
                logger.error("[MiPush] Fail to subscribe topic: {} by alias: {}", topic, uid);
            } else if (result.getErrorCode() != ErrorCode.Success) {
                logger.error("[MiPush] Fail to subscribe topic: {} by alias: {}. Reason: {}", topic, uid,
                        result.getReason());
            } else {
                subscribeResult = true;

                subscribeLogger.info("[MiPush] Subscribe - topic: {}, uid: {}", topic, uid);

                logger.info("[MiPush] Subscribed topic[{}] by alias[{}]: {}", topic, uid, result.toString());
            }

        } catch (Exception e) {
            logger.error("[MiPush] Subscribe failed - topic: " + topic + ", alias: " + uid, e);
        }

        return subscribeResult;
    }

    public boolean unsubscribeToMiPush(final String topic, final String uid) {
        boolean unsubscribeResult = false;

        try {
            Result result = miSubscription.unsubscribeTopicByAlias(topic, Lists.newArrayList(uid), null, 3);

            if (result == null) {
                logger.error("[MiPush] Fail to unsubscribe topic: {} by alias: {}", topic, uid);
            } else if (result.getErrorCode() != ErrorCode.Success) {
                logger.error("[MiPush] Fail to unsubscribe topic: {} by alias: {}. Reason: {}", topic, uid,
                        result.getReason());
            } else {
                unsubscribeResult = true;

                subscribeLogger.info("[MiPush] Unsubscribe - topic: {}, uid: {}", topic, uid);

                logger.info("[MiPush] Unsubscribed topic[{}] by alias[{}]: {}", topic, uid, result.toString());
            }

        } catch (Exception e) {
            logger.error("[MiPush] Unsubscribe failed - topic: " + topic + ", alias: " + uid, e);
        }

        return unsubscribeResult;
    }
}
