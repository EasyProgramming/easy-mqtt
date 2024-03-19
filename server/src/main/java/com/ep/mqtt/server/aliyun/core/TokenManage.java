package com.ep.mqtt.server.aliyun.core;

import com.ep.mqtt.server.aliyun.vo.TokenVo;
import com.ep.mqtt.server.metadata.StoreKey;
import com.ep.mqtt.server.util.JsonUtil;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

/**
 * @author zbz
 * @date 2023/12/5 18:06
 */
public class TokenManage {

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    public Map<String, TokenVo> getToken(String clientId){
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        Map<String, String> remoteTokenMap = hashOperations.entries(StoreKey.ALIYUN_CLIENT_AUTH_TOKEN_KEY.formatKey(clientId));
        Map<String, TokenVo> tokenVoMap = Maps.newHashMap();
        for (Map.Entry<String, String> entry : remoteTokenMap.entrySet()){
            tokenVoMap.put(entry.getKey(), JsonUtil.string2Obj(entry.getValue(), TokenVo.class));
        }
        return tokenVoMap;
    }

}
