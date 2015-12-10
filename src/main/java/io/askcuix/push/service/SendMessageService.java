package io.askcuix.push.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.askcuix.push.entity.ResultCode;
import io.askcuix.push.entity.UserPushInfo;
import io.askcuix.push.persist.UserPushInfoDao;
import io.askcuix.push.service.ios.ApplePushQueueProcessor;
import io.askcuix.push.service.mipush.MiPushQueueProcessor;
import io.askcuix.push.thrift.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by Chris on 15/12/10.
 */
@Service
public class SendMessageService {
    private static final Logger logger = LoggerFactory.getLogger(SendMessageService.class);

    @Autowired
    private MiPushQueueProcessor miPushProcessor;

    @Autowired
    private ApplePushQueueProcessor iosProcessor;

    @Autowired
    private UserPushInfoDao userInfoDao;

    public PushResult sendToDevice(OsType osType, PushSysType pushSysType, String pushId, PushMessage message) {
        if (message.getMsgType() == MessageType.All) {
            PushResult result = new PushResult();
            result.setCode(ResultCode.PARAM_ERROR.getCode());
            result.setDesc("Unsupported message type.");

            return result;
        } else if (message.getMsgType() == MessageType.Notification) {
            // 通知栏消息
            return sendNotificationToDevice(osType, pushSysType, pushId, message);
        }

        // 长连接透传消息
        return sendPushToDevice(osType, pushSysType, pushId, message);
    }

    public PushResult sendToUser(String uid, PushMessage message) {
        UserPushInfo userInfo = null;

        try {
            userInfo = userInfoDao.findByUid(uid);
        } catch (Exception e) {
            logger.error("Fail to find user: " + uid, e);

            PushResult pushResult = new PushResult();
            pushResult.setCode(ResultCode.SERVER_ERROR.getCode());
            pushResult.setDesc(e.getMessage());

            return pushResult;
        }

        if (userInfo == null) {
            logger.warn("Cannot found user: {}, ignore to send message.", uid);

            PushResult pushResult = new PushResult();
            pushResult.setCode(ResultCode.USER_NOT_EXISTS.getCode());
            pushResult.setDesc("user not found");

            return pushResult;
        }

        if (message.getMsgType() == MessageType.All) {
            PushResult notifyResult = sendNotificationToDevice(userInfo, copyMessage(message, MessageType.Notification));

            PushResult pushResult = sendPushToDevice(userInfo, copyMessage(message, MessageType.PassThrough));

            PushResult result = new PushResult();

            if (notifyResult.getCode() != ResultCode.SUCCESS.getCode()
                    && pushResult.getCode() != ResultCode.SUCCESS.getCode()) {
                result.setCode(ResultCode.SEND_ERROR.getCode());
                result.setDesc("Send message faild");
            } else if (notifyResult.getCode() == ResultCode.SUCCESS.getCode()
                    && pushResult.getCode() != ResultCode.SUCCESS.getCode()) {
                result = pushResult;
            } else if (notifyResult.getCode() != ResultCode.SUCCESS.getCode()
                    && pushResult.getCode() == ResultCode.SUCCESS.getCode()) {
                result = notifyResult;
            } else {
                result.setCode(ResultCode.SUCCESS.getCode());
                result.setDesc("Success");
            }

            return result;
        } else if (message.getMsgType() == MessageType.Notification) {
            // 通知栏消息
            return sendNotificationToDevice(userInfo, message);
        }
        // 长连接透传消息
        return sendPushToDevice(userInfo, message);
    }

    private PushResult sendNotificationToDevice(UserPushInfo userInfo, PushMessage message) {
        PushSysType notifySystem = PushSysType.findByValue(userInfo.getNotifySysType());
        String notifyId = userInfo.getNotifyId();

        if (StringUtils.isBlank(notifyId)) {
            logger.warn("NotifyId still not set for user: {}, ignore to send notification.", userInfo.getUid());

            PushResult pushResult = new PushResult();
            pushResult.setCode(ResultCode.PUSH_INFO_MISSED.getCode());
            pushResult.setDesc("Push info missed");

            return pushResult;
        }

        return sendNotificationToDevice(OsType.findByValue(userInfo.getOs()), notifySystem, notifyId, message);
    }

    private PushResult sendNotificationToDevice(OsType osType, PushSysType notifySysType, String notifyId,
                                                PushMessage message) {
        if (osType == OsType.Android) {
            if (notifySysType == PushSysType.MiPush) {
                // MiPush
                miPushProcessor.putMessage(osType, Lists.newArrayList(notifyId), message);
                return new PushResult(ResultCode.SUCCESS.getCode(), "Success");
            }
        } else {
            if (notifySysType == PushSysType.MiPush) {
                // MiPush
                miPushProcessor.putMessage(osType, Lists.newArrayList(notifyId), message);
                return new PushResult(ResultCode.SUCCESS.getCode(), "Success");
            } else if (notifySysType == PushSysType.APNs) {
                // APNs
                iosProcessor.putMessage(Lists.newArrayList(notifyId), message);
                return new PushResult(ResultCode.SUCCESS.getCode(), "Success");
            }
        }

        PushResult result = new PushResult();
        result.setCode(ResultCode.PUSH_SYS_UNSUPPORT.getCode());
        result.setDesc("Unsupported push system.");

        return result;
    }

    private PushResult sendPushToDevice(UserPushInfo userInfo, PushMessage message) {
        PushSysType pushSystem = PushSysType.findByValue(userInfo.getPushSysType());
        String pushId = userInfo.getPushId();

        if (StringUtils.isBlank(pushId)) {
            logger.warn("PushId still not set for user: {}, ignore to send push.", userInfo.getUid());

            PushResult pushResult = new PushResult();
            pushResult.setCode(ResultCode.PUSH_INFO_MISSED.getCode());
            pushResult.setDesc("Push info missed");

            return pushResult;
        }

        return sendPushToDevice(OsType.findByValue(userInfo.getOs()), pushSystem, pushId, message);
    }

    private PushResult sendPushToDevice(OsType osType, PushSysType pushSysType, String pushId, PushMessage message) {
        if (osType == OsType.Android) {
            if (pushSysType == PushSysType.MiPush) {
                // MiPush
                miPushProcessor.putMessage(osType, Lists.newArrayList(pushId), message);
                return new PushResult(ResultCode.SUCCESS.getCode(), "Success");
            }
        } else {
            if (pushSysType == PushSysType.MiPush) {
                // MiPush
                miPushProcessor.putMessage(osType, Lists.newArrayList(pushId), message);
                return new PushResult(ResultCode.SUCCESS.getCode(), "Success");
            } else if (pushSysType == PushSysType.APNs) {
                // APNs
                iosProcessor.putMessage(Lists.newArrayList(pushId), message);
                return new PushResult(ResultCode.SUCCESS.getCode(), "Success");
            }
        }

        PushResult result = new PushResult();
        result.setCode(ResultCode.PUSH_SYS_UNSUPPORT.getCode());
        result.setDesc("Unsupported push system.");

        return result;
    }

    public PushResult sendToUsers(List<String> userList, PushMessage message) {
        List<UserPushInfo> userInfoList = null;

        try {
            userInfoList = userInfoDao.findByUids(userList);
        } catch (Exception e) {
            logger.error("Fail to find users.", e);

            PushResult pushResult = new PushResult();
            pushResult.setCode(ResultCode.SERVER_ERROR.getCode());
            pushResult.setDesc(e.getMessage());

            return pushResult;
        }

        if (userInfoList == null || userInfoList.isEmpty()) {
            logger.warn("Cannot found users, ignore to send message.");

            PushResult pushResult = new PushResult();
            pushResult.setCode(ResultCode.USER_NOT_EXISTS.getCode());
            pushResult.setDesc("Users not found");

            return pushResult;
        }

        Map<PushSysType, List<String>> androidNotifyMap = Maps.newHashMap();
        Map<PushSysType, List<String>> iosNotifyMap = Maps.newHashMap();

        Map<PushSysType, List<String>> androidPushMap = Maps.newHashMap();
        Map<PushSysType, List<String>> iosPushMap = Maps.newHashMap();

        for (UserPushInfo userInfo : userInfoList) {
            if (userInfo.getOs() == OsType.Android.getValue()) {
                setPushMap(androidNotifyMap, userInfo.getNotifySysType(), userInfo.getNotifyId());

                setPushMap(androidPushMap, userInfo.getPushSysType(), userInfo.getPushId());
            } else {
                setPushMap(iosNotifyMap, userInfo.getNotifySysType(), userInfo.getNotifyId());

                setPushMap(iosPushMap, userInfo.getPushSysType(), userInfo.getPushId());
            }
        }

        if (message.getMsgType() == MessageType.All) {
            sendNotificationToDevices(copyMessage(message, MessageType.Notification), androidNotifyMap, iosNotifyMap);

            sendPushToDevices(copyMessage(message, MessageType.PassThrough), androidPushMap, iosPushMap);
        } else if (message.getMsgType() == MessageType.Notification) {
            // 通知栏消息
            sendNotificationToDevices(message, androidNotifyMap, iosNotifyMap);
        } else {
            // 长连接透传消息
            sendPushToDevices(message, androidPushMap, iosPushMap);
        }

        return new PushResult(ResultCode.SUCCESS.getCode(), "Success");
    }

    private void setPushMap(Map<PushSysType, List<String>> pushMap, int pushSysType, String pushId) {
        if (StringUtils.isBlank(pushId)) {
            return;
        }

        PushSysType notifySysType = PushSysType.findByValue(pushSysType);
        if (pushMap.containsKey(notifySysType)) {
            pushMap.get(notifySysType).add(pushId);
        } else {
            List<String> pushIdList = Lists.newArrayList(pushId);
            pushMap.put(notifySysType, pushIdList);
        }

    }

    private void sendNotificationToDevices(PushMessage message, Map<PushSysType, List<String>> androidNotifyMap,
                                           Map<PushSysType, List<String>> iosNotifyMap) {
        // 发送android通知栏消息
        for (Map.Entry<PushSysType, List<String>> entry : androidNotifyMap.entrySet()) {
            if (entry.getKey() == PushSysType.MiPush) {
                miPushProcessor.putMessage(OsType.Android, entry.getValue(), message);
            }
        }

        // 发送ios通知栏消息
        for (Map.Entry<PushSysType, List<String>> entry : iosNotifyMap.entrySet()) {
            if (entry.getKey() == PushSysType.MiPush) {
                miPushProcessor.putMessage(OsType.iOS, entry.getValue(), message);
            } else if (entry.getKey() == PushSysType.APNs) {
                iosProcessor.putMessage(entry.getValue(), message);
            }
        }
    }

    private void sendPushToDevices(PushMessage message, Map<PushSysType, List<String>> androidPushMap,
                                   Map<PushSysType, List<String>> iosPushMap) {
        // 发送android透传消息
        for (Map.Entry<PushSysType, List<String>> entry : androidPushMap.entrySet()) {
            if (entry.getKey() == PushSysType.MiPush) {
                miPushProcessor.putMessage(OsType.Android, entry.getValue(), message);
            }
        }

        // 发送ios透传消息
        for (Map.Entry<PushSysType, List<String>> entry : iosPushMap.entrySet()) {
            if (entry.getKey() == PushSysType.MiPush) {
                miPushProcessor.putMessage(OsType.iOS, entry.getValue(), message);
            } else if (entry.getKey() == PushSysType.APNs) {
                iosProcessor.putMessage(entry.getValue(), message);
            }
        }
    }

    public PushResult broadcast(String topic, PushMessage message) {
        if (message.getMsgType() == MessageType.All) {
            broadcastNotification(topic, copyMessage(message, MessageType.Notification));

            broadcastPush(topic, copyMessage(message, MessageType.PassThrough));
        } else if (message.getMsgType() == MessageType.Notification) {
            // 通知栏消息
            broadcastNotification(topic, message);
        } else {
            // 长连接透传消息
            broadcastPush(topic, message);
        }

        return new PushResult(ResultCode.SUCCESS.getCode(), "Success");
    }

    private void broadcastPush(String topic, PushMessage message) {
        // APNs
         iosProcessor.putMulticast(topic, message);

        // MiPush
         miPushProcessor.putMulticast(OsType.Android, topic, message);
        miPushProcessor.putMulticast(OsType.iOS, topic, message);
    }

    private void broadcastNotification(String topic, PushMessage message) {
        // APNs
         iosProcessor.putMulticast(topic, message);

        // MiPush
        miPushProcessor.putMulticast(OsType.Android, topic, message);
        miPushProcessor.putMulticast(OsType.iOS, topic, message);
    }

    public PushResult broadcastAll(PushMessage message) {
        if (message.getMsgType() == MessageType.All) {
            broadcastAllNotification(copyMessage(message, MessageType.Notification));

            broadcastAllPush(copyMessage(message, MessageType.PassThrough));
        } else if (message.getMsgType() == MessageType.Notification) {
            // 通知栏消息
            broadcastAllNotification(message);
        } else {
            // 长连接透传消息
            broadcastAllPush(message);
        }

        return new PushResult(ResultCode.SUCCESS.getCode(), "Success");
    }

    private void broadcastAllPush(PushMessage message) {
        // APNs
         iosProcessor.putBroadcast(message);

        // MiPush
         miPushProcessor.putBroadcast(OsType.Android, message);
        miPushProcessor.putBroadcast(OsType.iOS, message);
    }

    private void broadcastAllNotification(PushMessage message) {
        // APNs
         iosProcessor.putBroadcast(message);

        // MiPush
        miPushProcessor.putBroadcast(OsType.Android, message);
        miPushProcessor.putBroadcast(OsType.iOS, message);
    }

    private PushMessage copyMessage(PushMessage message, MessageType toMsgType) {
        PushMessage toMsg = new PushMessage();
        toMsg.setTitle(message.getTitle());
        toMsg.setDesc(message.getDesc());
        toMsg.setMsgType(toMsgType);
        toMsg.setData(message.getData());
        toMsg.setExpiry(message.getExpiry());

        return toMsg;
    }

}
