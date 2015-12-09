package io.askcuix.push.service.ios;

import io.askcuix.push.common.Constant;
import io.askcuix.push.entity.UserPushInfo;
import io.askcuix.push.exception.PushException;
import io.askcuix.push.payload.PayloadMsgType;
import io.askcuix.push.persist.SubscribeDao;
import io.askcuix.push.persist.UserPushInfoDao;
import io.askcuix.push.thrift.OsType;
import io.askcuix.push.thrift.PushSysType;
import javapns.Push;
import javapns.notification.Payload;
import javapns.notification.PushedNotification;
import javapns.notification.transmission.PushQueue;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.util.List;

/**
 * Created by Chris on 15/11/27.
 */
public class ApplePushService {
    private static final Logger logger = LoggerFactory.getLogger(ApplePushService.class);
    private static final Logger pushMessageLogger = LoggerFactory.getLogger(Constant.LOG_PUSH_MESSAGE);

    private static ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Autowired
    private SubscribeDao subscribeDao;

    @Autowired
    private UserPushInfoDao userPushInfoDao;

    private String password;
    private File certFile;

    public ApplePushService(String certFilePath, String password) {
        this.password = password;

        try {
            certFile = resourceLoader.getResource(certFilePath).getFile();
        } catch (Exception e) {
            logger.error("Load cert file error.", e);
            throw new IllegalArgumentException("Cert file not exits!");
        }
    }

    public void sendToDevice(String deviceId, Payload message) throws PushException {
        try {
            List<PushedNotification> notifications = Push.payload(message, certFile, password, true,
                    new String[] { deviceId });

            List<PushedNotification> failedNotifications = PushedNotification.findFailedNotifications(notifications);
            List<PushedNotification> successfulNotifications = PushedNotification
                    .findSuccessfulNotifications(notifications);
            int failed = failedNotifications.size();
            int successful = successfulNotifications.size();

            if (successful > 0 && failed == 0) {
                logger.info("[APNs] Notification pushed successfully: {}", successfulNotifications.get(0).toString());

                pushMessageLogger.info("[APNs] Send to device success. deviceId: {}, Message: {}", deviceId, message);
            } else if (successful == 0 && failed == 0) {
                logger.error("[APNs] No notification could be sent, probably because of a critical error.");
            } else {
                logger.error("[APNs] Notification push failed: {}", failedNotifications.get(0).toString());
            }
        } catch (Exception e) {
            logger.error("[APNs] Fail to push message to device: " + deviceId, e);

            throw new PushException("[APNs] Send error: " + e.getMessage());
        }
    }

    public void sendToDevices(List<String> deviceList, Payload message) throws PushException {
        try {
            List<PushedNotification> notifications = null;

            if (deviceList.size() > 200) {
                notifications = Push.payload(message, certFile, password, true, 5, deviceList);
            } else {
                notifications = Push.payload(message, certFile, password, true, deviceList);
            }

            List<PushedNotification> failedNotifications = PushedNotification.findFailedNotifications(notifications);
            List<PushedNotification> successfulNotifications = PushedNotification
                    .findSuccessfulNotifications(notifications);
            int failed = failedNotifications.size();
            int successful = successfulNotifications.size();

            if (successful > 0 && failed == 0) {
                logger.info("[APNs] Notification pushed successfully: {}", successful);
                logSentMessage(successfulNotifications);

                pushMessageLogger.info("[APNs] Send to devices success. devices: [{}], Message: {}",
                        StringUtils.join(deviceList, ", "), message);
            } else if (successful == 0 && failed == 0) {
                logger.error("[APNs] No notification could be sent, probably because of a critical error.");
            } else {
                logger.error("[APNs] Notification push failed: {}", failed);
                logSentMessage(failedNotifications);
            }
        } catch (Exception e) {
            logger.error("[APNs] Fail to push message to devices: ", e);

            throw new PushException("[APNs] Send error: " + e.getMessage());
        }
    }

    public void broadcast(String topic, PayloadMsgType msgType, Payload message) throws PushException {
        List<String> uidList = null;
        String lastUid = null;
        List<UserPushInfo> userInfoList = null;
        int count = 0;
        boolean isNotify = msgType == PayloadMsgType.Notification ? true : false;

        try {

            /* Create the queue */
            PushQueue pushQueue = Push.queue(certFile, password, true, 5);

            /* Start the queue (all threads and connections and initiated) */
            pushQueue.start();

            while (true) {
                uidList = subscribeDao.getUsersByTopic(topic, lastUid, 1000);
                if (uidList == null || uidList.isEmpty()) {
                    break;
                }

                lastUid = uidList.get(uidList.size() - 1);

                userInfoList = userPushInfoDao.findUsers(uidList, OsType.iOS.getValue(), isNotify,
                        PushSysType.APNs.getValue());
                for (UserPushInfo userPushInfo : userInfoList) {
                    try {
                        String deviceId = isNotify ? userPushInfo.getNotifyId() : userPushInfo.getPushId();

                        if (StringUtils.isNotBlank(deviceId)) {
                            pushQueue.add(message, deviceId);
                        }
                    } catch (Exception e) {
                        logger.warn("Device token not valid: " + userPushInfo, e);
                    }
                }
                count += userInfoList.size();
            }

            pushMessageLogger.info("[APNs] Broadcast topic[{}] to {} devices: {}", topic, count, message);
        } catch (Exception e) {
            logger.error("[APNs] Fail to push message to topic: " + topic, e);

            throw new PushException("[APNs] Multicast error: " + e.getMessage());
        }
    }

    public void broadcastAll(PayloadMsgType msgType, Payload message) throws PushException {
        String lastUid = null;
        List<UserPushInfo> userInfoList = null;
        int count = 0;
        boolean isNotify = msgType == PayloadMsgType.Notification ? true : false;

        try {
            /* Create the queue */
            PushQueue pushQueue = Push.queue(certFile, password, true, 5);

            /* Start the queue (all threads and connections and initiated) */
            pushQueue.start();

            while (true) {
                userInfoList = userPushInfoDao.findUsers(lastUid, OsType.iOS.getValue(), isNotify,
                        PushSysType.APNs.getValue(), 1000);
                if (userInfoList == null || userInfoList.isEmpty()) {
                    break;
                }

                lastUid = userInfoList.get(userInfoList.size() - 1).getUid();

                for (UserPushInfo userPushInfo : userInfoList) {
                    try {
                        String deviceId = isNotify ? userPushInfo.getNotifyId() : userPushInfo.getPushId();

                        if (StringUtils.isNotBlank(deviceId)) {
                            pushQueue.add(message, deviceId);
                        }
                    } catch (Exception e) {
                        logger.warn("Device token not valid: " + userPushInfo, e);
                    }
                }
                count += userInfoList.size();
            }

            pushMessageLogger.info("[APNs] Broadcast to {} devices: {}", count, message);
        } catch (Exception e) {
            logger.error("[APNs] Fail to push message to all.", e);

            throw new PushException("[APNs] Broadcast error: " + e.getMessage());
        }
    }

    private void logSentMessage(List<PushedNotification> notifications) {
        logger.info("\tResult: ");
        for (PushedNotification notification : notifications) {
            logger.info("\t\t{}", notification.toString());
        }
    }
}
