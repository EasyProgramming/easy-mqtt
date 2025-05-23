package com.ep.mqtt.server.deal;

import com.ep.mqtt.server.db.dao.ClientDao;
import com.ep.mqtt.server.db.dto.ClientDto;
import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.metadata.LocalLock;
import com.ep.mqtt.server.util.TransactionUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author : zbz
 * @date : 2025/4/6
 */
@Slf4j
@Component
public class MessageIdDeal {

    private final static Cache<String, IdProgress> MESSAGE_ID_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(10000)
            .maximumSize(10000L * 100)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .concurrencyLevel(Constant.PROCESSOR_NUM)
            .recordStats()
            .build();


    @Resource
    private ClientDao clientDao;

    @Resource
    private TransactionUtil transactionUtil;

    /**
     * 生成消息id
     * @param clientId 客户端信息
     * @return 消息id
     */
    public Integer genMessageId(String clientId){
        synchronized (LocalLock.LOCK_CLIENT.getLocalLockName(clientId)){
            IdProgress idProgress = MESSAGE_ID_CACHE.getIfPresent(clientId);
            if (idProgress == null){
                idProgress = new IdProgress();
                idProgress.setCurrentId(0L);
                idProgress.setMaxId(0L);

                MESSAGE_ID_CACHE.put(clientId, idProgress);
            }

            if (idProgress.getCurrentId() >= idProgress.getMaxId()){
                Long maxId = idProgress.getMaxId();
                Integer size = idProgress.getSize();

                DefaultTransactionDefinition defaultTransactionDefinition = new DefaultTransactionDefinition(transactionUtil.getDefaultTransactionDefinition());
                defaultTransactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

                Long newMaxId = transactionUtil.transaction(defaultTransactionDefinition, ()->{
                    ClientDto clientDto = clientDao.lock(clientId);
                    if (clientDto == null) {
                        return null;
                    }

                    Long tempMaxId = maxId + size;

                    clientDao.updateMessageIdProgress(clientId, tempMaxId);

                    return tempMaxId;
                });

                if (newMaxId == null){
                    return null;
                }

                idProgress.setMaxId(newMaxId);
            }

            long newCurrentId = idProgress.getCurrentId() + 1;
            idProgress.setCurrentId(newCurrentId);

            return (int)(newCurrentId % 65535);
        }
    }

    public static void remove(String clientId){
        MESSAGE_ID_CACHE.invalidate(clientId);
    }

    @Data
    public static class IdProgress {

        private Long currentId;

        private Long maxId;

        private Integer size = 100;

    }
}
