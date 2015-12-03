package io.askcuix.push.transport;

import io.askcuix.push.thrift.PushService;
import io.askcuix.push.util.RequestThreadHelper;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.transport.TSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * 封装PushService的processor，设置请求的IP以方便做白名单控制。
 *
 * Created by Chris on 15/11/27.
 */
public class PushServiceProcessor extends PushService.Processor<PushService.Iface> implements TProcessor {
    private static final Logger logger = LoggerFactory.getLogger(PushServiceProcessor.class);

    public PushServiceProcessor(PushService.Iface iface) {
        super(iface);
    }

    /**
     * 获取请求的IP。
     * 需在创建thrift server时使用TFramedTransportWraper。
     *
     * @param iprot
     * @return 请求IP
     */
    private String getClientIp(TProtocol iprot) {
        InetAddress address = ((TSocket) ((TFramedTransportWraper) iprot.getTransport()).getTTransport()).getSocket()
                .getInetAddress();
        return address.getHostAddress();
    }

    /**
     * 设置请求的IP，供接口实现中使用。
     */
    @Override
    public boolean process(TProtocol iprot, TProtocol oprot) throws TException {
        String ip = getClientIp(iprot);

        RequestThreadHelper.setRequestorIp(ip);
        try {
            boolean result = super.process(iprot, oprot);
            return result;
        } catch (TProtocolException e) {
            logger.error("Exception occured when request from: {}. Error: {}", ip, e.getMessage());
            throw e;
        } finally {
            RequestThreadHelper.cleanupRequestorIp();
        }
    }
}
