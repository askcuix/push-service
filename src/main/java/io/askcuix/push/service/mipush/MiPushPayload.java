package io.askcuix.push.service.mipush;

import com.xiaomi.xmpush.server.Message;
import io.askcuix.push.payload.PushPayload;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Created by Chris on 15/12/10.
 */
public class MiPushPayload extends PushPayload {
    private Message message;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
