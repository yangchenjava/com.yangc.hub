com.yangc.hub
=============

### 基于Netty的初级推送服务器实现
Netty - 强大的网络应用程序框架，封装了java nio，基于事件驱动的异步api。<br />
ActiveMQ - apache的一款消息服务器，这里主要用于多台推送服务器间进行消息传送，达到负载均衡的作用。<br />
Protocol Buffer - google的一种数据交换的格式。<br />
实现两种协议方式（通过配置文件进行协议切换，均可达到通信目的）：
> 1. 自定义协议，协议内容如下；<br />
> 2. 通过protobuf实现，项目中有定义好的proto文件；

在resources文件夹中，有jmeter测试最大连接数的测试用例。

### 1.RESULT
每个request的发生都要返回对应的response，又称之为响应报文。<br />
> 协议：<br />
0x68 [contentType(0x00)] [uuid] [fromLength] [toLength] [dataLength] [from] [to] 0x68 [success] [data] [crc] 0x16

### 2.LOGIN
在首次消息发送前，必须先登录，如果多次验证失败，将强制关闭连接；如果绕开登录直接发送消息，也将强制关闭连接。登录成功后，会收到之前未读的离线文本，或者未收到的离线文件。<br />
> 协议：<br />
0x68 [contentType(0x01)] [uuid] 0x68 [usernameLength] [passwordLength] [username] [password] [crc] 0x16

### 3.CHAT
发送文本，如果接收方没有在线，将转为离线文本，待下次登录时，会接收到。<br />
> 协议：<br />
0x68 [contentType(0x02)] [uuid] [fromLength] [toLength] [dataLength] [from] [to] 0x68 [data] [crc] 0x16

### 4.READY_FILE
发送文件之前要询问对方是否接收此文件，同时会携带要发送文件的属性信息。如果接收方返回接收，则开始发送文件；如果接收方返回拒绝，则不发送文件。<br />
> 协议：<br />
0x68 [contentType(0x03)] [uuid] [fromLength] [toLength] [from] [to] 0x68 [fileNameLength] [fileName] [fileSize] [crc] 0x16

### 5.TRANSMIT_FILE
发送文件，如果接收方没有在线，将转为离线文件，待下次登录时，将离线文件推送给接收方。<br />
> 协议：<br />
0x68 [contentType(0x04)] [uuid] [fromLength] [toLength] [dataLength] [from] [to] 0x68 [fileNameLength] [fileName] [fileSize] [fileMd5] [offset] [data] [crc] 0x16

### 6.HEART
发送心跳，保证连接可用。<br />
> 协议：<br />
0x68 [contentType(0x55)] 0x68 [crc] 0x16

### Tips：
    1.这里说的消息包括：文本、文件。
    2.是否支持离线文本，可以配置。
    3.支持离线文件，但是不支持断点续传。
    4.支持黑名单，白名单的设置。
    5.通过MQ可以达到Netty集群的作用。（负载均衡，非灾难转移）
