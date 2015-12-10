package io.askcuix.push.service.ios;

import io.askcuix.push.common.Constant;
import io.askcuix.push.payload.PayloadMsgType;
import io.askcuix.push.payload.PushPayload;
import io.askcuix.push.thrift.MessageType;
import io.askcuix.push.thrift.PushMessage;
import javapns.notification.Payload;
import javapns.notification.PushNotificationBigPayload;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Created by Chris on 15/12/9.
 */
public class APNsPayload extends PushPayload {
    private Payload message;

    public Payload getMessage() {
        return message;
    }

    public void setMessage(Payload message) {
        this.message = message;
    }

    public void setPushMessage(PushMessage pushMsg) throws Exception {
        this.msgType = PayloadMsgType.findByValue(pushMsg.getMsgType().getValue());
        this.message = buildMessage(pushMsg);
    }

    /**
     * 构造iOS通知消息。
     *
     * @param pushMessage
     * @return ios message
     */
    public static Payload buildMessage(PushMessage pushMessage) throws Exception {
        /* Build a blank payload to customize, max payload is 2048byte */
        PushNotificationBigPayload payload = PushNotificationBigPayload.complex();

        /* Customize the payload */
        if (pushMessage.getMsgType() == MessageType.Notification) {
            payload.addAlert(pushMessage.getDesc());
            payload.addSound("default");
        }

        payload.addCustomDictionary(Constant.MESSAGE_DATA_KEY, pushMessage.getData());

        if (pushMessage.getExpiry() > 0L) {
            payload.setExpiry(new Long(pushMessage.getExpiry() / 1000).intValue());
        }


        return payload;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
