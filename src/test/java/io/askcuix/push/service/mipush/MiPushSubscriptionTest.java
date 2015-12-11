package io.askcuix.push.service.mipush;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import static org.junit.Assert.*;

/**
 * Created by Chris on 15/12/11.
 */
@Ignore
@ContextConfiguration(locations = { "/appContext-test.xml" })
public class MiPushSubscriptionTest extends AbstractJUnit4SpringContextTests {

    @Qualifier("miPushAndroidSubscription")
    @Autowired
    private MiPushSubscription miPushAndroidSubscription;

    @Test
    public void testSubscribe()  {
        String topic = "fans";
        String uid = "10001";

        boolean result = miPushAndroidSubscription.subscribeToMiPush(topic, uid);
        assertTrue(result);

        result = miPushAndroidSubscription.unsubscribeToMiPush(topic, uid);
        assertTrue(result);
    }
}
