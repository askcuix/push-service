package io.askcuix.push.service;

import io.askcuix.push.common.Constant;
import io.askcuix.push.entity.ResultCode;
import io.askcuix.push.thrift.*;
import io.askcuix.push.util.RequestThreadHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Chris on 15/11/27.
 */
@Service
public class PushServiceImpl implements PushService.Iface {
    private static final Logger logger = LoggerFactory.getLogger(PushServiceImpl.class);
    private final static Logger accessLogger = LoggerFactory.getLogger(Constant.LOG_THRIFT_ACCESS);
    private final static Logger monitorLogger = LoggerFactory.getLogger(Constant.LOG_MONITOR);

    @Autowired
    private RegisterService registerService;

    @Autowired
    private SubscribeService subscribeService;

    @Autowired
    private SendMessageService senderService;

    private static final long MONITOR_THRESHOLDS = 3000L;

    @Override
    public boolean registerPush(UserInfo userInfo) throws TException {
        accessLogger.info("registerPush - userInfo: {}, ip: {}", userInfo, RequestThreadHelper.getRequestorIp());
        long start = System.currentTimeMillis();

        if (userInfo == null || StringUtils.isBlank(userInfo.getUid()) || userInfo.getOsType() == null) {
            logger.warn("[registerPush] Request parameter is invalid. userInfo: {}", userInfo);
            return false;
        }

        boolean result = registerService.register(userInfo);

        long duration = System.currentTimeMillis() - start;
        if (duration > MONITOR_THRESHOLDS) {
            monitorLogger.warn("registerPush - userInfo: {}, cost time: {}ms", userInfo, duration);
        }

        return result;
    }

    @Override
    public boolean unregisterPush(UserInfo userInfo) throws TException {
        accessLogger.info("unregisterPush - userInfo: {}, ip: {}", userInfo, RequestThreadHelper.getRequestorIp());
        long start = System.currentTimeMillis();

        if (userInfo == null || StringUtils.isBlank(userInfo.getUid()) || userInfo.getOsType() == null) {
            logger.warn("[unregisterPush] Request parameter is invalid. userInfo: {}", userInfo);
            return false;
        }

        boolean result = registerService.unregisterPush(userInfo);

        long duration = System.currentTimeMillis() - start;
        if (duration > MONITOR_THRESHOLDS) {
            monitorLogger.warn("unregisterPush - userInfo: {}, cost time: {}ms", userInfo, duration);
        }

        return result;
    }

    @Override
    public PushResult sendToDevice(OsType osType, PushSysType pushSysType, String pushId, PushMessage message) throws TException {
        accessLogger.info("sendToDevice - osType: {}, pushSysType: {}, pushId: {}, message: {}, ip: {}", osType,
                pushSysType, pushId, message.toString(), RequestThreadHelper.getRequestorIp());
        long start = System.currentTimeMillis();

        if (osType == null || pushSysType == null || StringUtils.isBlank(pushId) || message == null
                || StringUtils.isBlank(message.getData())) {
            logger.warn(
                    "[sendToDevice] Request parameter is invalid. osType: {}, pushSysType: {}, pushId: {}, message: {}",
                    osType, pushSysType, pushId, message);

            return new PushResult(ResultCode.PARAM_ERROR.getCode(), "Invalid parameter");
        }

        if (message.getMsgType() == MessageType.Notification) {
            if (StringUtils.isBlank(message.getDesc())) {
                logger.warn(
                        "[sendToDevice] Missed message description. osType: {}, pushSysType: {}, pushId: {}, message: {}",
                        osType, pushSysType, pushId, message);

                return new PushResult(ResultCode.PARAM_ERROR.getCode(), "Missed message description");
            }

            if (osType == OsType.Android && StringUtils.isBlank(message.getTitle())) {
                logger.warn(
                        "[sendToDevice] Missed message title. osType: {}, pushSysType: {}, pushId: {}, message: {}",
                        osType, pushSysType, pushId, message);

                return new PushResult(ResultCode.PARAM_ERROR.getCode(), "Missed message title");
            }
        }

        PushResult result = senderService.sendToDevice(osType, pushSysType, pushId, message);

        long duration = System.currentTimeMillis() - start;
        if (duration > MONITOR_THRESHOLDS) {
            monitorLogger.warn("sendToDevice - osType: {}, pushSysType: {}, pushId: {}, message: {}, cost time: {}ms",
                    osType, pushSysType, pushId, message.toString(), duration);
        }

        return result;
    }

    @Override
    public PushResult sendToUser(String uid, PushMessage message) throws TException {
        accessLogger.info("sendToUser - uid: {} message: {}, ip: {}", uid, message.toString(),
                RequestThreadHelper.getRequestorIp());
        long start = System.currentTimeMillis();

        if (StringUtils.isBlank(uid) || message == null || StringUtils.isBlank(message.getData())) {
            logger.warn("[sendToUser] Request parameter is invalid. uid: {}, message: {}", uid, message);

            return new PushResult(ResultCode.PARAM_ERROR.getCode(), "Invalid parameter");
        }

        if (message.getMsgType() == MessageType.Notification
                && (StringUtils.isBlank(message.getTitle()) || StringUtils.isBlank(message.getDesc()))) {
            logger.warn("[sendToUser] Missed message title and description. uid: {}, message: {}", uid, message);

            return new PushResult(ResultCode.PARAM_ERROR.getCode(), "Missed message title and description");
        }

        PushResult result = senderService.sendToUser(uid, message);

        long duration = System.currentTimeMillis() - start;
        if (duration > MONITOR_THRESHOLDS) {
            monitorLogger.warn("sendToUser - uid: {} message: {}, cost time: {}ms", uid, message.toString(), duration);
        }

        return result;
    }

    @Override
    public PushResult sendToUsers(List<String> uids, PushMessage message) throws TException {
        accessLogger.info("sendToUsers - uids: [{}], message: {}, ip: {}", StringUtils.join(uids, ", "),
                message.toString(), RequestThreadHelper.getRequestorIp());
        long start = System.currentTimeMillis();

        if (uids == null || uids.isEmpty() || message == null || StringUtils.isBlank(message.getData())) {
            logger.warn("[sendToUsers] Request parameter is invalid. uids: {}, message: {}",
                    StringUtils.join(uids, ", "), message);

            return new PushResult(ResultCode.PARAM_ERROR.getCode(), "Invalid parameter");
        }

        if ((message.getMsgType() == MessageType.All || message.getMsgType() == MessageType.Notification)
                && (StringUtils.isBlank(message.getTitle()) || StringUtils.isBlank(message.getDesc()))) {
            logger.warn("[sendToUsers] Missed message title and description. uids: {}, message: {}",
                    StringUtils.join(uids, ", "), message);

            return new PushResult(ResultCode.PARAM_ERROR.getCode(), "Missed message title and description");
        }

        PushResult result = senderService.sendToUsers(uids, message);

        long duration = System.currentTimeMillis() - start;
        if (duration > MONITOR_THRESHOLDS) {
            monitorLogger.warn("sendToUsers - uid count: {}, message: {}, cost time: {}ms", uids.size(),
                    message.toString(), duration);
        }

        return result;
    }

    @Override
    public boolean subscribe(String topic, String uid) throws TException {
        accessLogger.info("subscribe - topic: {}, uid: {}, ip: {}", topic, uid, RequestThreadHelper.getRequestorIp());
        long start = System.currentTimeMillis();

        if (StringUtils.isBlank(topic) || StringUtils.isBlank(uid)) {
            logger.warn("[subscribe] Request parameter is invalid. topic: {}, uid: {}", topic, uid);

            return false;
        }

        boolean result = subscribeService.subscribe(topic, uid);

        long duration = System.currentTimeMillis() - start;
        if (duration > MONITOR_THRESHOLDS) {
            monitorLogger.warn("subscribe - topic: {}, uid: {}, cost time: {}ms", topic, uid, duration);
        }

        return result;
    }

    @Override
    public boolean unsubscribe(String topic, String uid) throws TException {
        accessLogger.info("unsubscribe - topic: {}, uid: {}, ip: {}", topic, uid, RequestThreadHelper.getRequestorIp());
        long start = System.currentTimeMillis();

        if (StringUtils.isBlank(topic) || StringUtils.isBlank(uid)) {
            logger.warn("[unsubscribe] Request parameter is invalid. topic: {}, uid: {}", topic, uid);

            return false;
        }

        boolean result = subscribeService.unsubscribe(topic, uid);

        long duration = System.currentTimeMillis() - start;
        if (duration > MONITOR_THRESHOLDS) {
            monitorLogger.warn("unsubscribe - topic: {}, uid: {}, cost time: {}ms", topic, uid, duration);
        }

        return result;
    }

    @Override
    public PushResult broadcast(String topic, PushMessage message) throws TException {
        accessLogger.info("broadcast - topic: {}, message: {}, ip: {}", topic, message.toString(),
                RequestThreadHelper.getRequestorIp());
        long start = System.currentTimeMillis();

        if (StringUtils.isBlank(topic) || message == null || StringUtils.isBlank(message.getData())) {
            logger.warn("[broadcast] Request parameter is invalid. topic: {}, message: {}", topic, message);

            return new PushResult(ResultCode.PARAM_ERROR.getCode(), "Invalid parameter");
        }

        if ((message.getMsgType() == MessageType.All || message.getMsgType() == MessageType.Notification)
                && (StringUtils.isBlank(message.getTitle()) || StringUtils.isBlank(message.getDesc()))) {
            logger.warn("[broadcast] Missed message title and description. topic: {}, message: {}", topic, message);

            return new PushResult(ResultCode.PARAM_ERROR.getCode(), "Missed message title and description");
        }

        PushResult result = senderService.broadcast(topic, message);

        long duration = System.currentTimeMillis() - start;
        if (duration > MONITOR_THRESHOLDS) {
            monitorLogger.warn("broadcast - topic: {}, message: {}, cost time: {}ms", topic,
                    message.toString(), duration);
        }

        return result;
    }

    @Override
    public PushResult broadcastAll(PushMessage message) throws TException {
        accessLogger.info("broadcastAll - message: {}, ip: {}", message.toString(), RequestThreadHelper.getRequestorIp());
        long start = System.currentTimeMillis();

        if (message == null || StringUtils.isBlank(message.getData())) {
            logger.warn("[broadcastAll] Request parameter is invalid. message: {}", message);

            return new PushResult(ResultCode.PARAM_ERROR.getCode(), "Invalid parameter");
        }

        if ((message.getMsgType() == MessageType.All || message.getMsgType() == MessageType.Notification)
                && (StringUtils.isBlank(message.getTitle()) || StringUtils.isBlank(message.getDesc()))) {
            logger.warn("[broadcastAll] Missed message title and description. message: {}", message);

            return new PushResult(ResultCode.PARAM_ERROR.getCode(), "Missed message title and description");
        }

        PushResult result = senderService.broadcastAll(message);

        long duration = System.currentTimeMillis() - start;
        if (duration > MONITOR_THRESHOLDS) {
            monitorLogger.warn("broadcastAll - message: {}, cost time: {}ms", message.toString(), duration);
        }

        return result;
    }
}
