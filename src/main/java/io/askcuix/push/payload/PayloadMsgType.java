package io.askcuix.push.payload;

/**
 * Created by Chris on 15/12/9.
 */
public enum PayloadMsgType {
    Notification(0), // 通知栏消息
    PassThrough(1), // 透传消息
    ;

    int value;

    private PayloadMsgType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PayloadMsgType findByValue(int value) {
        switch (value) {
            case 0:
                return Notification;
            case 1:
                return PassThrough;
            default:
                return null;
        }
    }
}
