package io.askcuix.push.payload;

import java.util.List;

/**
 * Created by Chris on 15/12/9.
 */
public abstract class PushPayload {
    protected PayloadType type;
    protected List<String> deviceList;
    protected String topic;
    protected PayloadMsgType msgType;
    protected long expiry;

    public PayloadType getType() {
        return type;
    }

    public void setType(PayloadType type) {
        this.type = type;
    }

    public List<String> getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(List<String> deviceList) {
        this.deviceList = deviceList;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public PayloadMsgType getMsgType() {
        return msgType;
    }

    public void setMsgType(PayloadMsgType msgType) {
        this.msgType = msgType;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}
