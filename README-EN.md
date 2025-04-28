English | [简体中文](./README.md)

# 💎 Easy MQTT
A simple and practical `MQTT` server(broker)

## 🎯 Goals
Keep everything simple

## 💪 Features
- [x] **Minimal startup method and configuration**
- [x] **Supports standalone/cluster mode**
- [x] **Supports data persistence**
- [x] Full compliance with MQTT v3.1.1 protocol
- [x] Supports WebSocket MQTT subprotocol
- [x] Supports external authentication via API during MQTT connection establishment
- [x] Supports SSL for TCP/WebSocket connections

## 🚀 Quick Start
### 1. Download and Install Easy MQTT
Click [here](https://github.com/EasyProgramming/easy-mqtt/releases) to download the latest compiled Easy MQTT package, and extract it.
### 2. Start Easy MQTT
```shell
sh bin/start.sh -c conf/conf.yml
```

## 📖文档
- [Essential Parameters Explanation](./doc/必要参数说明.md)
- [Cluster Deployment Example](./doc/集群部署示例.md)
- [Enabling Authentication](./doc/开启鉴权.md)
- [Enabling SSL](./doc/开启ssl.md)
- [Enabling WebSocket](./doc/开启websocket.md)

## Open Source License
See [LICENSE](./LICENSE)