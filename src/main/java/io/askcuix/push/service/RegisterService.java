package io.askcuix.push.service;

import io.askcuix.push.common.Constant;
import io.askcuix.push.entity.UserPushInfo;
import io.askcuix.push.persist.UserPushInfoDao;
import io.askcuix.push.thrift.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Created by Chris on 15/12/3.
 */
@Service
public class RegisterService {
    private static final Logger logger = LoggerFactory.getLogger(RegisterService.class);
    private static final Logger registerLogger = LoggerFactory.getLogger(Constant.LOG_REGISTER);

    @Autowired
    private UserPushInfoDao userInfoDao;

    public boolean register(UserInfo userInfo) {
        if (userInfo == null) {
            return false;
        }

        try {
            UserPushInfo pushInfo = userInfoDao.findByUid(userInfo.getUid());
            boolean exists = pushInfo != null ? true : false;

            if (exists && userInfo.getOsType().getValue() == pushInfo.getOs()
                    && pushInfo.getNotifySysType() == userInfo.getNotifySysType().getValue()
                    && StringUtils.equals(pushInfo.getNotifyId(), userInfo.getNotifyId())
                    && pushInfo.getPushSysType() == userInfo.getPushSysType().getValue()
                    && StringUtils.equals(pushInfo.getPushId(), userInfo.getPushId())) {
                logger.debug("User info exists. User: {}, Exists pushInfo: {}", userInfo, pushInfo);
                return true;
            }

            if (!exists) {
                pushInfo = new UserPushInfo();
                pushInfo.setUid(userInfo.getUid());
                pushInfo.setCreateTime(new Date());
            }

            pushInfo.setOs(userInfo.getOsType().getValue());
            pushInfo.setNotifySysType(userInfo.getNotifySysType().getValue());
            pushInfo.setNotifyId(userInfo.getNotifyId());
            pushInfo.setPushSysType(userInfo.getPushSysType().getValue());
            pushInfo.setPushId(userInfo.getPushId());
            pushInfo.setUpdateTime(new Date());

            userInfoDao.saveUserInfo(pushInfo);

            registerLogger.info("Register user: {}", pushInfo);
            logger.debug("Register user: {}", pushInfo);

            return true;
        } catch (Exception e) {
            logger.error("Register user info error.", e);
            return false;
        }
    }

    public boolean unregisterPush(UserInfo userInfo) {
        if (userInfo == null) {
            return false;
        }

        try {
            UserPushInfo pushInfo = userInfoDao.findByUid(userInfo.getUid());

            if (pushInfo == null) {
                logger.info("User[{}] not registed push info, skip to unregister.", pushInfo);
                return true;
            }

            if (userInfo.getOsType().getValue() != pushInfo.getOs()
                    || userInfo.getNotifySysType().getValue() != pushInfo.getNotifySysType()
                    || (StringUtils.isNotBlank(pushInfo.getNotifyId()) && !StringUtils.equals(userInfo.getNotifyId(), pushInfo.getNotifyId()))
                    || userInfo.getPushSysType().getValue() != pushInfo.getPushSysType()
                    || (StringUtils.isNotBlank(pushInfo.getPushId()) && !StringUtils.equals(userInfo.getPushId(), pushInfo.getPushId()))) {
                logger.warn("Unregister user[{}] is expired. Exists push info: {}", userInfo,
                        pushInfo);
                return false;
            }

            userInfoDao.removeUserInfo(userInfo.getUid());

            registerLogger.info("Unregister user: {}", pushInfo);
            logger.debug("Unregister user: {}", pushInfo);

            return true;
        } catch (Exception e) {
            logger.error("Unregister user info error.", e);
            return false;
        }

    }

}
