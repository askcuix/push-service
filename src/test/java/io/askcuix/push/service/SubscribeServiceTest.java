package io.askcuix.push.service;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import static org.junit.Assert.*;

/**
 * Created by Chris on 15/12/10.
 */
@Ignore
@ContextConfiguration(locations = { "/appContext-test.xml" })
public class SubscribeServiceTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private SubscribeService subscribeService;

    @Test
    public void testSubscribe() throws Exception {
        String topic = "fans";
        String uid = "10001";

        boolean result = subscribeService.subscribe(topic, uid);
        assertTrue(result);

        result = subscribeService.unsubscribe(topic, uid);
        assertTrue(result);
    }
}
