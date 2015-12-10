package io.askcuix.push.service;

import io.askcuix.push.persist.SubscribeDao;
import io.askcuix.push.service.mipush.MiPushSubscription;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Created by Chris on 15/12/10.
 */
@Service
public class SubscribeService {
    private static final Logger logger = LoggerFactory.getLogger(SubscribeService.class);

    @Qualifier("miPushAndroidSubscription")
    @Autowired
    private MiPushSubscription miPushAndroidSubscription;

    @Qualifier("miPushiOSSubscription")
    @Autowired
    private MiPushSubscription miPushiOSSubscription;

    @Autowired
    private SubscribeDao subscribeDao;

    @Value("${mipush.android.enable}")
    private boolean enableAndroidMiPush;

    @Value("${mipush.ios.enable}")
    private boolean enableiOSMiPush;

    public boolean subscribe(String topic, String uid) {
        if (StringUtils.isBlank(topic) || StringUtils.isBlank(uid)) {
            logger.warn("Missed required parameter.");
            return false;
        }

        boolean result;

        try {
            // 订阅topic到mongo
            result = subscribeDao.subscribe(topic, uid);

            // TODO: change to parallel process
            // 订阅到MiPush
            if (enableAndroidMiPush) {
                miPushAndroidSubscription.subscribeToMiPush(topic, uid);
            }

            if (enableiOSMiPush) {
                miPushiOSSubscription.subscribeToMiPush(topic, uid);
            }
        } catch (Exception e) {
            logger.error("Subscribe failed. topic: " + topic + ", uid: " + uid, e);
            result = false;
        }

        return result;
    }

    public boolean unsubscribe(String topic, String uid) {
        if (StringUtils.isBlank(topic) || StringUtils.isBlank(uid)) {
            logger.warn("Missed required parameter.");
            return false;
        }

        boolean result;

        try {
            // 取消订阅topic到mongo
            result = subscribeDao.unsubscribe(topic, uid);

            // TODO: change to parallel process
            // 取消订阅到MiPush
            if (enableAndroidMiPush) {
                miPushAndroidSubscription.unsubscribeToMiPush(topic, uid);
            }

            if (enableiOSMiPush) {
                miPushiOSSubscription.unsubscribeToMiPush(topic, uid);
            }
        } catch (Exception e) {
            logger.error("Unsubscribe failed. topic: " + topic + ", uid: " + uid, e);
            result = false;
        }

        return result;
    }
}
