package io.askcuix.push.persist;

import com.google.common.collect.Lists;
import io.askcuix.push.entity.UserPushInfo;
import io.askcuix.push.thrift.OsType;
import io.askcuix.push.thrift.PushSysType;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Chris on 15/12/3.
 */
@Ignore
@ContextConfiguration(locations = { "/appContext-test.xml" })
public class UserPushInfoDaoTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private UserPushInfoDao userPushInfoDao;

    @Test
    public void testCRUD() throws Exception {

        UserPushInfo pushInfo = new UserPushInfo();
        pushInfo.setUid("10001");
        pushInfo.setOs(OsType.Android.getValue());
        pushInfo.setNotifySysType(PushSysType.MiPush.getValue());
        pushInfo.setNotifyId("d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=");
        pushInfo.setPushSysType(PushSysType.MiPush.getValue());
        pushInfo.setPushId("d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=");

        //create
        userPushInfoDao.saveUserInfo(pushInfo);

        //query
        UserPushInfo record = userPushInfoDao.findByUid(pushInfo.getUid());
        assertNotNull(record);
        assertEquals(record.getOs(), pushInfo.getOs());
        assertEquals(record.getPushId(), pushInfo.getPushId());

        List<UserPushInfo> recordList = userPushInfoDao.findByUids(Lists.newArrayList(pushInfo.getUid()));
        assertNotNull(recordList);
        assertTrue(recordList.size() == 1);
        assertEquals(recordList.get(0).getOs(), pushInfo.getOs());
        assertEquals(recordList.get(0).getPushId(), pushInfo.getPushId());

        recordList = userPushInfoDao.findUsers(Lists.newArrayList(pushInfo.getUid()), OsType.Android.getValue(), false, PushSysType.MiPush.getValue());
        assertNotNull(recordList);
        assertTrue(recordList.size() == 1);
        assertEquals(recordList.get(0).getOs(), pushInfo.getOs());
        assertEquals(recordList.get(0).getPushId(), pushInfo.getPushId());

        recordList = userPushInfoDao.findUsers(null, OsType.Android.getValue(), false, PushSysType.MiPush.getValue(), 10);
        assertNotNull(recordList);
        assertTrue(recordList.size() == 1);
        assertEquals(recordList.get(0).getOs(), pushInfo.getOs());
        assertEquals(recordList.get(0).getPushId(), pushInfo.getPushId());

        // delete
        userPushInfoDao.removeUserInfo(pushInfo.getUid());

    }
}
