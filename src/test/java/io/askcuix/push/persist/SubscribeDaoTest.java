package io.askcuix.push.persist;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Chris on 15/12/7.
 */
@Ignore
@ContextConfiguration(locations = { "/appContext-test.xml" })
public class SubscribeDaoTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private SubscribeDao subscribeDao;

    @Test
    public void testSubscribe() throws Exception {
        String topic = "fans";
        String uid = "10001";

        boolean result = subscribeDao.subscribe(topic, uid);
        assertTrue(result);

        List<String> uidList = subscribeDao.getUsersByTopic(topic, null, 10);
        assertNotNull(uidList);
        assertTrue(uidList.size() == 1);
        assertEquals(uidList.get(0), uid);

        result = subscribeDao.unsubscribe(topic, uid);
        assertTrue(result);
    }
}
