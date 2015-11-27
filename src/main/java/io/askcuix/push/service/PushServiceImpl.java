package io.askcuix.push.service;

import io.askcuix.push.thrift.*;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Chris on 15/11/27.
 */
@Service
public class PushServiceImpl implements PushService.Iface {
    private static final Logger logger = LoggerFactory.getLogger(PushServiceImpl.class);

    @Override
    public boolean registerPush(UserInfo userInfo) throws TException {
        return false;
    }

    @Override
    public boolean unregisterPush(UserInfo userInfo) throws TException {
        return false;
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
