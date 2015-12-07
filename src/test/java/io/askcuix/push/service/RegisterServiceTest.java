package io.askcuix.push.service;

import io.askcuix.push.entity.UserPushInfo;
import io.askcuix.push.persist.UserPushInfoDao;
import io.askcuix.push.thrift.OsType;
import io.askcuix.push.thrift.PushSysType;
import io.askcuix.push.thrift.UserInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by Chris on 15/12/7.
 */
public class RegisterServiceTest {

    @InjectMocks
    private RegisterService registerService;

    @Mock
    private UserPushInfoDao userInfoDao;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRegister() throws Exception {
        UserInfo userInfo = new UserInfo();
        userInfo.setUid("10001");
        userInfo.setOsType(OsType.Android);
        userInfo.setNotifySysType(PushSysType.MiPush);
        userInfo.setNotifyId("d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=");
        userInfo.setPushSysType(PushSysType.MiPush);
        userInfo.setPushId("d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=");

        Mockito.when(userInfoDao.findByUid(userInfo.getUid())).thenReturn(null);

        boolean result = registerService.register(userInfo);

        Mockito.verify(userInfoDao, Mockito.times(1)).saveUserInfo(any(UserPushInfo.class));

        assertTrue(result);
    }

    @Test
    public void testUnregister() throws Exception {
        UserInfo userInfo = new UserInfo();
        userInfo.setUid("10001");
        userInfo.setOsType(OsType.Android);
        userInfo.setNotifySysType(PushSysType.MiPush);
        userInfo.setNotifyId("d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=");
        userInfo.setPushSysType(PushSysType.MiPush);
        userInfo.setPushId("d//igwEhgBGCI2TG6lWqlMgWOpgKQoV6aYFRwxXOHPXUi1Asbs4MDh8lVKTCU/sBMIdNjhHG7F228+v5mQWBJN9RJiTZy6ylPLyAoHvoF5k=");

        UserPushInfo pushInfo = new UserPushInfo();
        pushInfo.setUid(userInfo.getUid());
        pushInfo.setOs(userInfo.getOsType().getValue());
        pushInfo.setNotifySysType(userInfo.getNotifySysType().getValue());
        pushInfo.setNotifyId(userInfo.getNotifyId());
        pushInfo.setPushSysType(userInfo.getPushSysType().getValue());
        pushInfo.setPushId(userInfo.getPushId());

        Mockito.when(userInfoDao.findByUid(userInfo.getUid())).thenReturn(pushInfo);

        boolean result = registerService.unregisterPush(userInfo);

        Mockito.verify(userInfoDao, Mockito.times(1)).removeUserInfo(userInfo.getUid());

        assertTrue(result);
    }
}
