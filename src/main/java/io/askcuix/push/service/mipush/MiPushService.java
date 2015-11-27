package io.askcuix.push.service.mipush;

import com.xiaomi.xmpush.server.Sender;
import io.askcuix.push.common.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Chris on 15/11/27.
 */
public class MiPushService {
    private static final Logger logger = LoggerFactory.getLogger(MiPushService.class);
    private static final Logger pushMessageLogger = LoggerFactory.getLogger(Constant.LOG_PUSH_MESSAGE);

    private Sender sender;

    public MiPushService(Sender sender) {
        this.sender = sender;
    }
}
