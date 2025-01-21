English | [简体中文](./README-CN.md)

# 💎 Easy MQTT
一款简单、实用、高性能的`MQTT`服务器

## 🎯目标
让一切保持简单

## 💪 优势
- 支持单机和集群部署
- 支持数据持久化（从此不再担心服务器宕机导致的订阅关系、消息等数据的丢失）
- 一键启动

## 🚩 功能
- [x] 支持MQTT v3.1.1协议
- [x] 支持WebSocket MQTT子协议
- [x] 数据的持久化
- [x] 完整的QOS服务质量等级实现
- [x] 支持MQTT保留消息
- [x] 支持在建立MQTT连接时通过外部接口进行认证
- [x] 支持以SSL的方式建立TCP/WebSocket连接
- [ ] 遗嘱消息
- [ ] 提供发送消息的api

## 🚀 快速开始
### 1.下载安装Easy MQTT
点击 [这里](https://github.com/EasyProgramming/easy-mqtt/releases) 下载最新的已编译的easy mqtt压缩包，并解压
### 2.启动Easy MQTT
#### 单机模式
```shell script
sh bin/start.sh -c conf/conf.yml
```
#### 集群模式
```shell script
sh bin/start.sh -c conf/conf.yml
```

## 🔧 配置项
| 名称                                       | 描述                                                                                     | 默认值                         |
| ----------------------------------------- | ---------------------------------------------------------------------------------------- | --------------------------- |
| mqtt.server.is-use-epoll                  | 是否开启Epoll模式, linux下建议开启                                                           | false                            |
| mqtt.server.is-open-ssl                   | 是否开启ssl                                                                               | false                         |
| mqtt.server.ssl-certificate-password      | SSL密钥文件密码                                                                             |                          |
| mqtt.server.ssl-certificate-path          | SSL证书文件的绝对路径，只支持pfx格式的证书                                                      |                          |
| mqtt.server.tcp-port                      | tcp端口（mqtt协议的端口）                                                                   | 8081                         |
| mqtt.server.websocket-port                | websocket端口                                                                            | 8082                         |
| mqtt.server.api-port                      | api端口                                                                                 |  8083                         |
| mqtt.server.websocket-path                | websocket连接地址                                                                         | /websocket                         |
| mqtt.server.authentication-url            | 认证接口地址，如果为null或空字符串则不鉴权                                                       |                          |
| mqtt.server.listener-pool-size                  | 监听器的线程池大小                                                                         | 核心数*2                         |
| mqtt.server.deal-message-thread-pool-size       | 处理消息线程池的大小                                                                              |  核心数*3                         |

## 开源许可
详见 [LICENSE](./LICENSE)