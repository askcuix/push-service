package io.askcuix.push.payload;

/**
 * Created by Chris on 15/12/9.
 */
public enum PayloadType {
    SEND, // 发送消息
    MULTICAST, // 按topic多播消息
    BROADCAST, // 广播消息
    ;
}
