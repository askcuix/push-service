package io.askcuix.push.service.ios;

import io.askcuix.push.payload.PushPayload;
import javapns.notification.Payload;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Created by Chris on 15/12/9.
 */
public class APNsPayload extends PushPayload {
    private Payload message;

    public Payload getMessage() {
        return message;
    }

    public void setMessage(Payload message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
