package io.askcuix.push.transport;

import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportFactory;

/**
 * Created by Chris on 15/11/27.
 */
public class TFramedTransportWraper extends TFramedTransport {

    private TTransport transport_ = null;

    public TFramedTransportWraper(TTransport transport) {
        super(transport);
        transport_ = transport;
    }

    public TFramedTransportWraper(TTransport transport, int maxLength) {
        super(transport, maxLength);
        transport_ = transport;
    }

    public TTransport getTTransport() {
        return transport_;
    }

    public static class Factory extends TTransportFactory {
        private int maxLength_;

        public Factory() {
            maxLength_ = TFramedTransport.DEFAULT_MAX_LENGTH;
        }

        public Factory(int maxLength) {
            maxLength_ = maxLength;
        }

        @Override
        public TTransport getTransport(TTransport base) {
            return new TFramedTransportWraper(base, maxLength_);
        }
    }
}
