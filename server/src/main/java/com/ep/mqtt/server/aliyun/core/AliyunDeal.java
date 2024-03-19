package com.ep.mqtt.server.aliyun.core;

import com.ep.mqtt.server.aliyun.vo.TokenVo;
import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.deal.Deal;
import com.ep.mqtt.server.metadata.AliyunAuthWay;
import com.ep.mqtt.server.metadata.RocketMqMessagePropertyKey;
import com.ep.mqtt.server.util.TopicUtil;
import com.ep.mqtt.server.vo.MessageVo;
import com.google.common.collect.Lists;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author zbz
 * @date 2023/11/9 14:59
 */
@Slf4j
public class AliyunDeal extends Deal {

    private static final Integer P2P_PARAMS_SIZE = 3;

    private static final String P2P_FLAG = "/p2p/";

    private static final Integer USERNAME_PARAM_NUM = 3;


    @Autowired
    private RocketMqProducer rocketMqProducer;

    @Autowired
    private TokenManage tokenManage;

    @Override
    public void sendMessage(MessageVo messageVo) {
        boolean isP2P = false;
        String[] splitResult = StringUtils.split(messageVo.getTopic(), P2P_FLAG);
        if (splitResult.length == P2P_PARAMS_SIZE){
            if (!StringUtils.containsAny(splitResult[0], TopicUtil.TOPIC_SPLIT_FLAG)){
                isP2P = true;
                messageVo.setTopic(splitResult[0]);
                messageVo.setToClientId(splitResult[2]);
            }
        }
        super.sendMessage(messageVo, isP2P);
    }

    @Override
    public void publish(MessageVo messageVo) {
        String outputTopic = getOutputTopic(messageVo.getTopic());
        if (StringUtils.isBlank(outputTopic)){
            super.publish(messageVo);
            return;
        }
        sendToMq(outputTopic, messageVo);
    }


    @Override
    public void pubRel(Integer messageId, String clientId){
        MessageVo messageVo = getRecMessage(clientId, messageId);
        if (messageVo == null) {
            return;
        }
        String outputTopic = getOutputTopic(messageVo.getTopic());
        if (StringUtils.isBlank(outputTopic)){
            sendMessage(messageVo);
        }
        else {
            sendToMq(outputTopic, messageVo);
        }
        delRecMessage(clientId, messageId);
    }

    private String getOutputTopic(String mqttTopic){
        if (mqttServerProperties.getAliyun().getDataTransfer() == null){
            return "";
        }
        if (CollectionUtils.isEmpty(mqttServerProperties.getAliyun().getDataTransfer().getOutputRuleList())){
            return "";
        }
        for (MqttServerProperties.Aliyun.TopicMapRule topicMapRule : mqttServerProperties.getAliyun().getDataTransfer().getOutputRuleList()){
            if (topicMapRule.getRocketMqTopic().getTopic().equals(mqttTopic)){
                return topicMapRule.getMqttTopic().getTopic();
            }
        }
        return "";
    }

    private void sendToMq(String outputTopic, MessageVo messageVo){
        Properties properties = new Properties();
        properties.setProperty(RocketMqMessagePropertyKey.QOS_LEVEL.getKey(), String.valueOf(messageVo.getFromQos()));
        // 因为rmq和mqtt的topic是全匹配，且rmq的topic不含有/，所以不会有子级的topic
        properties.setProperty(RocketMqMessagePropertyKey.MQTT_SECOND_TOPIC.getKey(), "");
        properties.setProperty(RocketMqMessagePropertyKey.CLIENT_ID.getKey(), messageVo.getFromClientId());
        rocketMqProducer.send(outputTopic, messageVo.getPayload(), properties);
    }

    @Override
    public boolean authentication(MqttConnectMessage mqttConnectMessage) {
        String userName = mqttConnectMessage.payload().userName();
        Client client = getClient(userName);
        if (client == null){
            return false;
        }
        // 暂时只支持token模块的鉴权
        if (!AliyunAuthWay.TOKEN.getAliyunKey().equals(client.getAuthWay())){
            return false;
        }

        // 校验token key、action是否与服务端的完全一致，校验服务端的token是否过期
        Map<String, TokenVo> remoteTokenMap = tokenManage.getToken(client.getClientId());
        String password = new String(mqttConnectMessage.payload().passwordInBytes());
        List<Token> tokenList = getTokenList(password);
        for (TokenVo remoteToken : remoteTokenMap.values()){
            if (remoteToken.getExpireTime() < System.currentTimeMillis()){
                return false;
            }

        }

        return true;
    }

    private Client getClient(String userName){
        String[] userNameParams = StringUtils.split(userName, "|");
        if (userNameParams.length != USERNAME_PARAM_NUM){
            return null;
        }
        Client client = new Client();
        client.setAuthWay(userNameParams[0]);
        client.setAccessKeyId(userNameParams[1]);
        client.setClientId(userNameParams[2]);
        return client;
    }

    private List<Token> getTokenList(String password){
        List<Token> tokenList = Lists.newArrayList();
        String[] passwordParams = StringUtils.split(password, "|");
        int index = 0;
        while (index < passwordParams.length){
            Token token = new Token();
            token.setAction(passwordParams[index++]);
            token.setSecretStr(passwordParams[index++]);
            tokenList.add(token);
        }
        return tokenList;
    }

    @Data
    public static class Token {

        private String action;

        private String secretStr;

    }

    @Data
    public static class Client {

        private String authWay;

        private String clientId;

        private String accessKeyId;

    }
}
