package io.askcuix.push.util;

/**
 * 记录请求来源的IP地址。
 *
 * Created by Chris on 15/11/27.
 */
public class ThreadHelper {

    private static ThreadLocal<String> requestorIp = new ThreadLocal<String>();

    public static void setRequestorIp(String ip) {
        requestorIp.set(ip);
    }

    public static void cleanupRequestorIp() {
        requestorIp.remove();
    }

    public static String getRequestorIp() {
        return requestorIp.get();
    }
}
