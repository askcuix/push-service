#!/usr/local/bin/thrift --java --php --py

namespace java io.askcuix.push.thrift

#
# data structures
#

# 手机系统类型
enum OsType {
  Android = 0, // android os
  iOS = 1,     // iOS
}

# 消息类型
enum MessageType {
  Notification = 0, // 通知栏消息
  PassThrough = 1,  // 透传消息，不显示在通知栏
  All = 2,          // 同时发送通知栏消息和透传消息
}

# 消息推送系统类型
enum PushSysType {
  MiPush = 0,  // MiPush
  APNs = 1,    // iOS APNs
}

# 用户信息
struct UserInfo {
  1: required string uid,         // 用户UID，MiPush中作为设备的alias
  2: OsType osType,               // 手机系统
  3: PushSysType notifySysType,   // 通知栏消息推送系统类型
  4: string notifyId,             // 通知栏消息系统ID，MiPush使用regId, iOS使用deviceToken
  5: PushSysType pushSysType,     // 应用内长连接消息推送系统类型
  6: string pushId,               // 长连接消息推送系统ID，MiPush使用regId, iOS使用deviceToken
}

# push消息
struct PushMessage {
  1: string title,                   // 通知栏标题，仅对android有效，通知栏消息必填
  2: string desc,                    // 通知栏消息描述，通知栏消息必填
  3: required MessageType msgType,   // 消息类型
  4: required string data,           // 消息数据，json格式
  5: i64 expiry = 0,                 // 消息的有效时间。单位：ms。MiPush默认最长保留两周，APNs默认不保留
}

# push消息返回结果
struct PushResult {
  1: required i32 code,       // 等于0时,请求成功
  2: required string desc,    // 错误信息描述
}

#
# service api
#
service PushService {
  
  # 注册用户push信息
  bool registerPush(1:required UserInfo userInfo),
  
  # 取消用户push信息
  bool unregisterPush(1:required UserInfo userInfo),
  
  # 发送给特定设备，不支持MessageType=All
  PushResult sendToDevice(1:required OsType osType, 2:required PushSysType pushSysType, 3:required string pushId, 4:required PushMessage message),
  
  # 发送给指定用户
  PushResult sendToUser(1:required string uid, 2:required PushMessage message),
  
  # 发送给一组用户，最多1000个用户
  PushResult sendToUsers(1:required list<string> uids, 2:required PushMessage message),
  
  # 订阅topic
  bool subscribe(1:required string topic, 2:required string uid),
  
  # 取消订阅topic
  bool unsubscribe(1:required string topic, 2:required string uid),
  
  # 广播给订阅用户
  PushResult broadcast(1:required string topic, 2:required PushMessage message),
  
  # 广播给所有用户
  PushResult broadcastAll(1:required PushMessage message),
  
}