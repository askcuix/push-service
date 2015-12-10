package io.askcuix.push.service.mipush;

import com.google.common.collect.Lists;
import io.askcuix.push.service.SubscribeService;
import io.askcuix.push.thrift.MessageType;
import io.askcuix.push.thrift.PushMessage;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import static org.junit.Assert.*;

/**
 * Created by Chris on 15/12/10.
 */
@Ignore
@ContextConfiguration(locations = {"/appContext-test.xml"})
public class MiPushServiceTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    @Qualifier("androidMiBundle")
    private String androidBundle;

    @Autowired
    @Qualifier("miPushAndroidService")
    private MiPushService miPushService;

    @Autowired
    private SubscribeService subscribeService;

    @Test
    public void testSendToDevice() {
        PushMessage message = new PushMessage();
        message.setTitle("Push Service");
        message.setDesc("Send to device test");
        message.setMsgType(MessageType.Notification);
        message.setData("{\"name\" : \"Chris\"}");

        // android
        String device = "d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=";

        miPushService.sendToDevice(device, MiPushPayload.buildAndroidMessage(androidBundle, message));
    }

    @Test
    public void testSendToDevices() {
        PushMessage message = new PushMessage();
        message.setTitle("Push Service");
        message.setDesc("Send to devices test");
        message.setMsgType(MessageType.Notification);
        message.setData("{\"name\" : \"Chris\"}");

        // android
        String device = "d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=";

        miPushService.sendToDevices(Lists.newArrayList(device), MiPushPayload.buildAndroidMessage(androidBundle, message));
    }

    @Test
    public void testBroadcast() {
        String topic = "fans";
        String uid = "10001";

        boolean result = subscribeService.subscribe(topic, uid);
        assertTrue(result);

        PushMessage message = new PushMessage();
        message.setTitle("Push Service");
        message.setDesc("Broadcast topic to device test");
        message.setMsgType(MessageType.Notification);
        message.setData("{\"name\" : \"Chris\"}");

        miPushService.broadcast(topic, MiPushPayload.buildAndroidMessage(androidBundle, message));

        result = subscribeService.unsubscribe(topic, uid);
        assertTrue(result);
    }

    @Test
    public void testBroadcastAll() {
        PushMessage message = new PushMessage();
        message.setTitle("Push Service");
        message.setDesc("Broadcast to all device test");
        message.setMsgType(MessageType.Notification);
        message.setData("{\"name\" : \"Chris\"}");

        miPushService.broadcastAll(MiPushPayload.buildAndroidMessage(androidBundle, message));
    }
}
