package io.askcuix.push;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.askcuix.push.service.PushServiceImpl;
import io.askcuix.push.transport.PushServiceProcessor;
import io.askcuix.push.transport.TFramedTransportWraper;
import io.askcuix.push.util.ThreadUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by Chris on 15/11/27.
 */
public class PushServer {
    private final static Logger logger = LoggerFactory.getLogger(PushServer.class);

    private static final Integer DEFAULT_FRAMED_SIZE = 1024 * 1024 * 16;

    private ClassPathXmlApplicationContext context;
    private TThreadPoolServer server;
    private ExecutorService servingExecutor;

    public void start() {
        if (server != null && server.isServing()) {
            logger.error("Push service thrift server is running");
            return;
        }

        context = new ClassPathXmlApplicationContext("spring/appContext.xml");
        context.start();

        PushServiceImpl pushService = context.getBean(PushServiceImpl.class);
        String serverHost = context.getBean("serverHost", String.class);
        int serverPort = context.getBean("serverPort", Integer.class);

        if (StringUtils.isBlank(serverHost) || serverPort <= 0) {
            throw new IllegalArgumentException("Invalid thrift server ip[" + serverHost + "] or port[" + serverPort
                    + "].");
        }

        logger.info("Starting Push Service Thrift Server, bind to {}:{}.", serverHost, serverPort);

        // Transport
        TServerSocket tServerSocket;
        try {
            tServerSocket = new TServerSocket(new InetSocketAddress(serverHost, serverPort));
        } catch (TTransportException e) {
            throw new IllegalStateException("Fail to start push service thrift server.", e);
        }

        // Protocol factory
        TProtocolFactory protocolFactory = new TBinaryProtocol.Factory(true, true);

        TTransportFactory inTransportFactory = new TFramedTransportWraper.Factory(DEFAULT_FRAMED_SIZE);
        TTransportFactory outTransportFactory = new TFramedTransportWraper.Factory(DEFAULT_FRAMED_SIZE);

        TProcessor processor = new PushServiceProcessor(pushService);
        TThreadPoolServer.Args args = new TThreadPoolServer.Args(tServerSocket).minWorkerThreads(5).inputTransportFactory(inTransportFactory)
                .outputTransportFactory(outTransportFactory).inputProtocolFactory(protocolFactory)
                .outputProtocolFactory(protocolFactory).processor(processor);

        server = new TThreadPoolServer(args);

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Push-Thrift-Server").build();
        servingExecutor = Executors.newSingleThreadExecutor(threadFactory);

        /**
         * Start serving.
         */
        servingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                logger.info("Push service thrift server starting up...");
                server.serve();
            }
        });

        long timeAfterStart = System.currentTimeMillis();
        while (!server.isServing()) {
            try {
                if (System.currentTimeMillis() - timeAfterStart >= 10000) {
                    throw new RuntimeException("Push Service Thrift Server failed to start!");
                }

                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        logger.info("Push Service Thrift Server started.");
    }

    public void stop() {
        logger.info("Push Service Thrift Server Stopping");

        if (server == null || !server.isServing()) {
            logger.error("Push Service Thrift Server has stopped");

            return;
        }

        server.stop();

        ThreadUtil.gracefulShutdown(servingExecutor, 5000);

        context.stop();

        logger.info("Push Service Thrift Server stopped");
    }

    public static void main(String[] args) {
        final PushServer thriftServer = new PushServer();

        try {
            thriftServer.start();
        } catch (Exception e) {
            logger.error("Exception encountered during thrift server startup.", e);
            System.exit(3);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                thriftServer.stop();
            }
        }));
    }
}
