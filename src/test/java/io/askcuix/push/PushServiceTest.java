package io.askcuix.push;

import com.google.common.collect.Lists;
import io.askcuix.push.entity.ResultCode;
import io.askcuix.push.thrift.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Chris on 15/12/10.
 */
//@Ignore
public class PushServiceTest {

    private static TTransport transport;

    private static PushService.Client client;

    @BeforeClass
    public static void setUp() throws Exception {
        transport = new TFramedTransport(new TSocket("127.0.0.1", 13579));
        transport.open();

        TProtocol protocol = new TBinaryProtocol(transport);
        client = new PushService.Client(protocol);
    }

    @AfterClass
    public static void destory() {
        if (transport != null && transport.isOpen()) {
            transport.close();
        }
    }

    @Test
    public void testRegisterPush() {
        try {
            UserInfo userInfo = new UserInfo();
            userInfo.setUid("10001");
            userInfo.setOsType(OsType.Android);
            userInfo.setNotifySysType(PushSysType.MiPush);
            userInfo.setNotifyId("d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=");
            userInfo.setPushSysType(PushSysType.MiPush);
            userInfo.setPushId("d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=");

            boolean result = client.registerPush(userInfo);
            assertTrue(result);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Register push failed");
        }
    }

    @Test
    public void testSendToDevice() {
        try {
            PushMessage message = new PushMessage();
            message.setTitle("Push Service");
            message.setDesc("Send to device test");
            message.setMsgType(MessageType.Notification);
            message.setData("{\"name\" : \"Chris\"}");

            // android
            String device = "d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=";

            PushResult result = client.sendToDevice(OsType.Android, PushSysType.MiPush, device, message);

            System.out.println("testSendToDevice result: " + result);

            assertTrue(result.getCode() == ResultCode.SUCCESS.getCode());
        } catch (Exception e) {
            e.printStackTrace();
            fail("send to regId failed.");
        }
    }

    @Test
    public void testSendToUser() {
        PushMessage message = new PushMessage();
        message.setTitle("Push Service");
        message.setDesc("Send to user test");
        message.setMsgType(MessageType.Notification);
        message.setData("{\"name\" : \"Chris\"}");

        try {
            PushResult result = client.sendToUser("10001", message);

            System.out.println("testSendToUser result: " + result);

            assertTrue(result.getCode() == ResultCode.SUCCESS.getCode());
        } catch (Exception e) {
            e.printStackTrace();
            fail("send to user failed.");
        }
    }

    @Test
    public void testSendToUsers() {
        PushMessage message = new PushMessage();
        message.setTitle("Push Service");
        message.setDesc("Send to users test");
        message.setMsgType(MessageType.Notification);
        message.setData("{\"name\" : \"Chris\"}");

        try {
            PushResult result = client.sendToUsers(Lists.newArrayList("10001"), message);

            System.out.println("testSendToUsers result: " + result);

            assertTrue(result.getCode() == ResultCode.SUCCESS.getCode());
        } catch (Exception e) {
            e.printStackTrace();
            fail("send to users failed.");
        }
    }

    @Test
    public void testSubscribe() {
        try {
            boolean result = client.subscribe("Fans", "10001");
            assertTrue(result);
        } catch (Exception e) {
            e.printStackTrace();
            fail("subscribe failed.");
        }
    }

    @Test
    public void testBroadcast() {
        PushMessage message = new PushMessage();
        message.setTitle("Push Service");
        message.setDesc("Broadcast to topic test");
        message.setMsgType(MessageType.Notification);
        message.setData("{\"name\" : \"Chris\"}");

        try {
            PushResult result = client.broadcast("Fans", message);

            System.out.println("testBroadcast result: " + result);

            assertTrue(result.getCode() == ResultCode.SUCCESS.getCode());
        } catch (Exception e) {
            e.printStackTrace();
            fail("broadcast to topic failed.");
        }
    }

    @Test
    public void testUnsubscribe() {
        try {
            boolean result = client.unsubscribe("Fans", "10001");
            assertTrue(result);
        } catch (Exception e) {
            e.printStackTrace();
            fail("unsubscribe failed.");
        }
    }

    @Test
    public void testBroadcastAll() {
        PushMessage message = new PushMessage();
        message.setTitle("Push Service");
        message.setDesc("Broadcast to all test");
        message.setMsgType(MessageType.Notification);
        message.setData("{\"name\" : \"Chris\"}");

        try {
            PushResult result = client.broadcastAll(message);

            System.out.println("testBroadcastAll result: " + result);

            assertTrue(result.getCode() == ResultCode.SUCCESS.getCode());
        } catch (Exception e) {
            e.printStackTrace();
            fail("broadcast to all failed.");
        }
    }

    @Test
    public void testUnregisterPush() {
        try {
            UserInfo userInfo = new UserInfo();
            userInfo.setUid("10001");
            userInfo.setOsType(OsType.Android);
            userInfo.setNotifySysType(PushSysType.MiPush);
            userInfo.setNotifyId("d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=");
            userInfo.setPushSysType(PushSysType.MiPush);
            userInfo.setPushId("d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=");

            boolean result = client.unregisterPush(userInfo);
            assertTrue(result);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unregister push failed");
        }
    }
}
