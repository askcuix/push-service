package io.askcuix.push.service.mipush;

import com.xiaomi.xmpush.server.Message;
import io.askcuix.push.common.Constant;
import io.askcuix.push.payload.PayloadMsgType;
import io.askcuix.push.payload.PushPayload;
import io.askcuix.push.thrift.MessageType;
import io.askcuix.push.thrift.OsType;
import io.askcuix.push.thrift.PushMessage;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Chris on 15/12/10.
 */
public class MiPushPayload extends PushPayload {
    private int osType;
    private String bundle;
    private Message message;

    public int getOsType() {
        return osType;
    }

    public void setOsType(int osType) {
        this.osType = osType;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void setPushMessage(PushMessage pushMsg) {
        this.msgType = PayloadMsgType.findByValue(pushMsg.getMsgType().getValue());
        if (this.osType == OsType.Android.getValue()) {
            this.message = buildAndroidMessage(bundle, pushMsg);
        } else if (this.osType == OsType.iOS.getValue()){
            this.message = buildiOsMessage(pushMsg);
        }
    }

    /**
     * 构造android消息。
     *
     * @param pushMessage
     * @return android message
     */
    public static Message buildAndroidMessage(String bundle, PushMessage pushMessage) {
        if (pushMessage == null) {
            return null;
        }

        Map<String, String> dataMap = new HashMap<String, String>();
        dataMap.put(Constant.MESSAGE_DATA_KEY, pushMessage.getData());

        JSONObject dataJson = new JSONObject(dataMap);
        String payload = dataJson.toJSONString();

        Message.Builder builder = new Message.Builder().payload(payload) // 数据
                .restrictedPackageName(bundle) // 包名
                .passThrough(pushMessage.getMsgType().getValue()); // 消息通知方式

        if (pushMessage.getMsgType() == MessageType.Notification) {
            builder.title(pushMessage.getTitle()); // 通知栏标题
            builder.description(pushMessage.getDesc()); // 通知栏描述
            builder.notifyType(-1); // 使用全部提示
        }

        if (pushMessage.getExpiry() > 0L) {
            builder.timeToLive(pushMessage.getExpiry());
        }

        return builder.build();
    }

    /**
     * 构造android消息。
     *
     * @param pushMessage
     * @return ios message
     */
    public static Message buildiOsMessage(PushMessage pushMessage) {
        if (pushMessage == null) {
            return null;
        }

        Message.IOSBuilder builder = new Message.IOSBuilder();
        builder.description(pushMessage.getDesc());
        builder.extra(Constant.MESSAGE_DATA_KEY, pushMessage.getData());

        if (pushMessage.getExpiry() > 0L) {
            builder.timeToLive(pushMessage.getExpiry());
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
