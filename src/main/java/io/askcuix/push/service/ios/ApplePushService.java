package io.askcuix.push.service.ios;

import io.askcuix.push.common.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.io.File;

/**
 * Created by Chris on 15/11/27.
 */
public class ApplePushService {
    private static final Logger logger = LoggerFactory.getLogger(ApplePushService.class);
    private static final Logger pushMessageLogger = LoggerFactory.getLogger(Constant.LOG_PUSH_MESSAGE);

    private static ResourceLoader resourceLoader = new DefaultResourceLoader();

    private String password;
    private File certFile;

    public ApplePushService(String certFilePath, String password) {
        this.password = password;

        try {
            certFile = resourceLoader.getResource(certFilePath).getFile();
        } catch (Exception e) {
            logger.error("Load cert file error.", e);
            throw new IllegalArgumentException("Cert file not exits!");
        }
    }
}
