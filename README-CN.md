English | [简体中文](./README-CN.md)

# 💎 Easy MQTT
一款简单、实用的`MQTT`服务器

## 🎯目标
让一切保持简单

## 💪 功能
- [x] **极简的启动方式及配置项**
- [x] **支持单机/集群**
- [x] **支持数据持久化**
- [x] 支持完整的MQTT v3.1.1协议
- [x] 支持WebSocket MQTT子协议
- [x] 支持在建立MQTT连接时通过外部接口进行认证
- [x] 支持以SSL的方式建立TCP/WebSocket连接

## 🚀 快速开始
### 1.下载安装Easy MQTT
点击 [这里](https://github.com/EasyProgramming/easy-mqtt/releases) 下载最新的已编译的easy mqtt压缩包，并解压
### 2.启动Easy MQTT
```shell script
sh bin/start.sh -c conf/conf.yml
```

## 📖文档
- [必要参数说明](./doc/必要参数说明.md)
- [集群部署示例](./doc/集群部署示例.md)
- [开启鉴权](./doc/开启鉴权.md)
- [开启ssl](./doc/开启ssl.md)
- [开启websocket](./doc/开启websocket.md)

## 开源许可
详见 [LICENSE](./LICENSE)