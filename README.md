English | [简体中文](./README-CN.md)

# 💎 Easy MQTT
A simple, practical, and high-performance `MQTT` broker

## Goals
Keep everything simple

## 💪 Advantages
- Supports standalone and cluster deployments
- Supports data persistence (no more worries about losing subscription relationships, messages, and other data due to server crashes)
- One-click startup

## 🚩 Features
- [x] Supports MQTT v3.1.1 protocol
- [x] Supports WebSocket MQTT protocol
- [x] Data persistence
- [x] Full implementation of QoS (Quality of Service) levels
- [x] Supports MQTT retained messages
- [x] Supports authentication through external interfaces when establishing MQTT connections
- [x] Supports establishing TCP/WebSocket connections using SSL
- [ ] Will messages
- [ ] Provides an API for sending messages

## 🚀 Quick Start
### 1. Download and Install Easy MQTT
Click [here](https://github.com/EasyProgramming/easy-mqtt/releases) to download the latest compiled Easy MQTT package, and extract it.
### 2. Start Easy MQTT
#### Standalone Mode
```shell
sh bin/start.sh -c conf/conf.yml
```
#### Cluster Mode
```shell
sh bin/start.sh -c conf/conf.yml
```

## 🔧 Configuration Options
| Name                                      | Description                                                                             | Default Value               |
| ----------------------------------------- | --------------------------------------------------------------------------------------- | --------------------------- |
| mqtt.server.is-open-ssl                   | Whether to enable SSL                                                                   | false                       |
| mqtt.server.ssl-certificate-password      | Password for the SSL key file                                                           |                             |
| mqtt.server.ssl-certificate-path          | Absolute path of the SSL certificate file (only supports certificates in pfx format)    |                             |
| mqtt.server.tcp-port                      | TCP port (MQTT protocol port)	                                                          | 8081                        |
| mqtt.server.websocket-port                | WebSocket port	                                                                      | 8082                        |
| mqtt.server.api-port                      | API port	                                                                              |  8083                       |
| mqtt.server.websocket-path                | WebSocket connection path	                                                              | /websocket                  |
| mqtt.server.authentication-url            | Authentication interface URL (if null or empty, authentication will not be performed)	  |                             |

## Open Source License
See [LICENSE](./LICENSE)