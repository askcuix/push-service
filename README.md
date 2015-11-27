# push-service

移动端消息推送服务，对外暴露thrift接口。

提供用户终端信息上报，数据存储在MongoDB中。

Android端集成MiPush，iOS端集成MiPush和APNs，发送渠道以用户终端上报的信息为准。

提供通知栏消息和应用内长连接消息两种推送，支持单用户、多用户发送，按订阅的topic发送，以及广播给所有用户。
