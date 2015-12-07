package io.askcuix.push.service;

import io.askcuix.push.common.Constant;
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

    private static final long MONITOR_THRESHOLDS = 1000L;

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
        return null;
    }

    @Override
    public PushResult sendToUser(String uid, PushMessage message) throws TException {
        return null;
    }

    @Override
    public PushResult sendToUsers(List<String> uids, PushMessage message) throws TException {
        return null;
    }

    @Override
    public boolean subscribe(String topic, String uid) throws TException {
        return false;
    }

    @Override
    public boolean unsubscribe(String topic, String uid) throws TException {
        return false;
    }

    @Override
    public PushResult broadcast(String topic, PushMessage message) throws TException {
        return null;
    }

    @Override
    public PushResult broadcastAll(PushMessage message) throws TException {
        return null;
    }
}
