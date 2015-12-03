package io.askcuix.push.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Created by Chris on 15/12/3.
 */
public class UserPushInfo {
    private String uid;
    private int os;
    private int notifySysType;
    private String notifyId;
    private int pushSysType;
    private String pushId;

    public UserPushInfo() {
        // do nothing
    }

    public UserPushInfo(String uid, int os, int notifySysType, String notifyId, int pushSysType, String pushId) {
        this.uid = uid;
        this.os = os;
        this.notifySysType = notifySysType;
        this.notifyId = notifyId;
        this.pushSysType = pushSysType;
        this.pushId = pushId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getOs() {
        return os;
    }

    public void setOs(int os) {
        this.os = os;
    }

    public int getNotifySysType() {
        return notifySysType;
    }

    public void setNotifySysType(int notifySysType) {
        this.notifySysType = notifySysType;
    }

    public String getNotifyId() {
        return notifyId;
    }

    public void setNotifyId(String notifyId) {
        this.notifyId = notifyId;
    }

    public int getPushSysType() {
        return pushSysType;
    }

    public void setPushSysType(int pushSysType) {
        this.pushSysType = pushSysType;
    }

    public String getPushId() {
        return pushId;
    }

    public void setPushId(String pushId) {
        this.pushId = pushId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
