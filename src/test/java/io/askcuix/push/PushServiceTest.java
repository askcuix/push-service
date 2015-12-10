package io.askcuix.push;

import io.askcuix.push.thrift.PushService;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 * Created by Chris on 15/12/10.
 */
@Ignore
public class PushServiceTest {

    private static TTransport transport;

    private static PushService.Client client;

    @BeforeClass
    public static void setUp() throws Exception {
        transport = new TFramedTransport(new TSocket("127.0.0.1", 13579));
        transport.open();

        TProtocol protocol = new TBinaryProtocol(transport);
        client = new PushService.Client(protocol);
    }

    @AfterClass
    public static void destory() {
        if (transport != null && transport.isOpen()) {
            transport.close();
        }
    }
}
