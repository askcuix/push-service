package io.askcuix.push.entity;

/**
 * Created by Chris on 15/12/10.
 */
public enum ResultCode {
    SUCCESS(0), // 成功
    UNKNOWN_ERROR(-1), // 未知错误
    SERVER_ERROR(-2), // 服务器异常
    PARAM_ERROR(-100), // 参数错误
    USER_NOT_EXISTS(-101), // 用户信息不存在
    MSG_ERROR(-102), // 创建push消息错误
    PUSH_SYS_UNSUPPORT(-103), // 不支持的push系统
    PUSH_INFO_MISSED(-104), // push信息不全
    SEND_ERROR(-105), // 发送消息失败
    ;

    int code;

    ResultCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
