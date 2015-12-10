package io.askcuix.push.service.mipush;

import com.xiaomi.push.sdk.ErrorCode;
import com.xiaomi.xmpush.server.Constants;
import com.xiaomi.xmpush.server.Message;
import com.xiaomi.xmpush.server.Result;
import com.xiaomi.xmpush.server.Sender;
import io.askcuix.push.common.Constant;
import io.askcuix.push.exception.PushException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * Created by Chris on 15/11/27.
 */
public class MiPushService {
    private static final Logger logger = LoggerFactory.getLogger(MiPushService.class);
    private static final Logger pushMessageLogger = LoggerFactory.getLogger(Constant.LOG_PUSH_MESSAGE);

    private Sender sender;
    private String deviceType;

    @Value("${dev.mode}")
    private boolean devMode;

    public MiPushService(Sender sender, String deviceType) {
        this.sender = sender;
        this.deviceType = deviceType;
    }

    private void initEnv() {
        if (devMode && StringUtils.equalsIgnoreCase(deviceType, Constant.DEVICE_IOS)) {
            Constants.useSandbox();
        } else {
            Constants.useOfficial();
        }
    }

    public void sendToDevice(String deviceId, Message message) throws PushException {
        try {
            initEnv();

            Result result = sender.sendNoRetry(message, deviceId);
            if (result == null) {
                logger.error("[MiPush] Fail to send message to deviceId: {}", deviceId);
            } else if (result.getErrorCode() != ErrorCode.Success) {
                logger.error("[MiPush] Send to deviceId[{}] failed. Error code: {}, Reason: {}", deviceId,
                        result.getErrorCode().getFullDescription(), result.getReason());
            } else {
                pushMessageLogger.info("[MiPush] Send to device success. deviceId: {}, MsgID: {}, Message: {}",
                        deviceId, result.getMessageId(), message);

                logger.info("[MiPush] Send to deviceId[{}] success.", deviceId);
            }
        } catch (Exception e) {
            logger.error("[MiPush] Fail to push message to device: " + deviceId, e);

            throw new PushException("[MiPush] Send error: " + e.getMessage());
        }
    }

    public void sendToDevices(List<String> deviceList, Message message) throws PushException {
        try {
            initEnv();

            Result result = sender.send(message, deviceList, 0);
            if (result == null) {
                logger.error("[MiPush] Fail to send message to devices: {}", StringUtils.join(deviceList, ", "));
            } else if (result.getErrorCode() != ErrorCode.Success) {
                logger.error("[MiPush] Send to devices failed. Error code: {}, Reason: {}",
                        result.getErrorCode().getFullDescription(), result.getReason());
            } else {
                pushMessageLogger.info("[MiPush] Send to devices success. devices: [{}], MsgID: {}, Message: {}",
                        StringUtils.join(deviceList, ", "), result.getMessageId(), message);

                logger.info("[MiPush] Send to devices success.");
            }
        } catch (Exception e) {
            logger.error("[MiPush] Fail to push message to devices.", e);

            throw new PushException("[MiPush] Send error: " + e.getMessage());
        }
    }

    public void broadcast(String topic, Message message) throws PushException {
        try {
            initEnv();

            Result result = sender.broadcastNoRetry(message, topic);

            if (result == null) {
                logger.error("[MiPush] Fail to broadcast message to topic: {}", topic);
            } else if (result.getErrorCode() != ErrorCode.Success) {
                logger.error("[MiPush] Broadcast to topic[{}] failed. Error code: {}, Reason: {}", topic,
                        result.getErrorCode().getFullDescription(), result.getReason());
            } else {
                pushMessageLogger.info("[MiPush] Broadcast topic[{}] to devices: {} - {}", topic, result.getMessageId(),
                        message);

                logger.info("[MiPush] Broadcast to topic[{}] success. MsgID: {}", topic, result.getMessageId());
            }
        } catch (Exception e) {
            logger.error("[MiPush] Fail to broadcast message to topic: " + topic, e);

            throw new PushException("[MiPush] Multicast error: " + e.getMessage());
        }
    }

    public void broadcastAll(Message message) throws PushException {
        try {
            initEnv();

            Result result = sender.broadcastAllNoRetry(message);

            if (result == null) {
                logger.error("[MiPush] Fail to broadcast message to all");
            } else if (result.getErrorCode() != ErrorCode.Success) {
                logger.error("[MiPush] Broadcast to all failed. Error code: {}, Reason: {}",
                        result.getErrorCode().getFullDescription(), result.getReason());
            } else {
                pushMessageLogger.info("[MiPush] Broadcast to all devices: {} - {}", result.getMessageId(), message);

                logger.info("[MiPush] Broadcast to all success. MsgID: {}", result.getMessageId());
            }
        } catch (Exception e) {
            logger.error("[MiPush] Fail to broadcast message to all.", e);

            throw new PushException("[MiPush] Broadcast error: " + e.getMessage());
        }
    }
}
