package com.ep.mqtt.server.deal;

import com.ep.mqtt.server.db.dao.ClientDao;
import com.ep.mqtt.server.db.dao.ClientSubscribeDao;
import com.ep.mqtt.server.db.dao.ReceiveQos2MessageDao;
import com.ep.mqtt.server.db.dto.ClientDto;
import com.ep.mqtt.server.db.dto.ClientSubscribeDto;
import com.ep.mqtt.server.db.dto.SendMessageDto;
import com.ep.mqtt.server.job.DispatchMessageParam;
import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.metadata.DisconnectReason;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.YesOrNo;
import com.ep.mqtt.server.queue.InsertSendMessageQueue;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.store.TopicFilterStore;
import com.ep.mqtt.server.util.ModelUtil;
import com.ep.mqtt.server.util.TransactionUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zbz
 * @date 2025/3/8 14:00
 */
@Slf4j
@Component
public class CommonDeal {

    @Resource
    private ClientDao clientDao;

    @Resource
    private ClientSubscribeDao clientSubscribeDao;

    @Resource
    private ReceiveQos2MessageDao receiveQos2MessageDao;

    @Resource
    private ClientSubscribeDao subscribeDao;

    @Resource
    private TransactionUtil transactionUtil;

    @Transactional(rollbackFor = Exception.class)
    public void clearClientData(String clientId) {
        clientDao.deleteByClientId(clientId);
        clientSubscribeDao.deleteByClientId(clientId);
        receiveQos2MessageDao.deleteByFromClientId(clientId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void afterDisconnect(DisconnectReason disconnectReason, Session session){
        if (!DisconnectReason.REPEAT_CONNECT.equals(disconnectReason)){
            if (session!= null && session.getIsCleanSession()){
                ClientDto clientDto = clientDao.lock(session.getClientId());
                if (clientDto == null){
                    throw new RuntimeException(String.format("client [%s], not exist", session.getClientId()));
                }

                clearClientData(clientDto.getClientId());
            }
        }

        if (!DisconnectReason.NORMAL.equals(disconnectReason) && session!= null && session.getWillMessage() != null){
            dispatchMessage(ModelUtil.buildDispatchMessageParam(
                    session.getWillMessage().getQos(),
                    session.getWillMessage().getTopic(),
                    -1,
                    session.getClientId(),
                    session.getWillMessage().getPayload(),
                    session.getWillMessage().getIsRetain() ? YesOrNo.YES : YesOrNo.NO
            ));

//            asyncJobManage.addJob(
//                    AsyncJobBusinessType.DISPATCH_MESSAGE.getBusinessId(UUID.randomUUID().toString()),
//                    AsyncJobBusinessType.DISPATCH_MESSAGE,
//                    ModelUtil.buildDispatchMessageParam(
//                            session.getWillMessage().getQos(),
//                            session.getWillMessage().getTopic(),
//                            -1,
//                            session.getClientId(),
//                            session.getWillMessage().getPayload(),
//                            session.getWillMessage().getIsRetain() ? YesOrNo.YES : YesOrNo.NO
//                    ),
//                    new Date());
        }
    }

    public void dispatchMessage(DispatchMessageParam dispatchMessageParam){
        Map<String, Qos> clientQosMap = match(dispatchMessageParam.getTopic());

        send(dispatchMessageParam, clientQosMap);
    }

    private Map<String, Qos> match(String topic){
        Map<String, Qos> clientQosMap = Maps.newHashMap();

        // 匹配topic filter
        List<String> matchTopicFilterList = TopicFilterStore.matchTopicFilter(topic);
        if (CollectionUtils.isEmpty(matchTopicFilterList)){
            return clientQosMap;
        }

        // 根据topic filter及id游标查询匹配的客户端，并计算qos
        Long cursor = 0L;
        int pageSize = 50000;
        while (true){
            List<ClientSubscribeDto> clientSubscribePage = subscribeDao.selectByCursor(Sets.newHashSet(matchTopicFilterList), cursor, pageSize);

            for (ClientSubscribeDto clientSubscribe : clientSubscribePage){
                Qos existQos = clientQosMap.get(clientSubscribe.getClientId());
                if (existQos == null){
                    clientQosMap.put(clientSubscribe.getClientId(), clientSubscribe.getQos());

                    continue;
                }

                if (existQos.getCode() >= clientSubscribe.getQos().getCode()){
                    continue;
                }

                clientQosMap.put(clientSubscribe.getClientId(), clientSubscribe.getQos());
            }

            if (CollectionUtils.isEmpty(clientSubscribePage) || clientSubscribePage.size() < pageSize){
                break;
            }

            cursor = clientSubscribePage.get(clientSubscribePage.size() - 1).getId();
        }

        return clientQosMap;
    }

    private void send(DispatchMessageParam dispatchMessageParam, Map<String, Qos> clientQosMap){
        if (CollectionUtils.isEmpty(clientQosMap)){
            return;
        }

        long now = System.currentTimeMillis();
        List<SendMessageDto> sendMessageDtoList = Lists.newArrayList();
        Map<String, Integer> sendMessageIdMap = Maps.newConcurrentMap();
        ThreadPoolExecutor messageIdThreadPool = new ThreadPoolExecutor(Constant.PROCESSOR_NUM, Constant.PROCESSOR_NUM * 2,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(clientQosMap.size()), new ThreadFactoryBuilder().setNameFormat("gen-message-id-%s").build());
        CountDownLatch countDownLatch = new CountDownLatch(clientQosMap.size());
        for (Map.Entry<String, Qos> clientQosEntry : clientQosMap.entrySet()){
            String toClientId = clientQosEntry.getKey();
            Qos sendQos = clientQosEntry.getValue().getCode() >= dispatchMessageParam.getReceiveQos().getCode() ?
                    dispatchMessageParam.getReceiveQos() : clientQosEntry.getValue();

            sendMessageDtoList.add(ModelUtil.buildSendMessageDto(dispatchMessageParam.getReceiveQos(), dispatchMessageParam.getReceivePacketId(),
                dispatchMessageParam.getFromClientId(), sendQos, dispatchMessageParam.getTopic(), null, toClientId, dispatchMessageParam.getPayload(),
                YesOrNo.NO, now + 1000L * 60 * 60 * 24 * 7, dispatchMessageParam.getIsRetain()));

            if (sendQos.equals(Qos.LEVEL_0)){
                continue;
            }

            messageIdThreadPool.submit(()->{
                try {
                    transactionUtil.transaction(() -> {
                        ClientDto clientDto = clientDao.lock(toClientId);
                        if (clientDto == null) {
                            return null;
                        }

                        long lastMessageIdProgress = clientDto.getMessageIdProgress() + 1L;
                        clientDao.updateMessageIdProgress(clientDto.getClientId(), lastMessageIdProgress);

                        sendMessageIdMap.put(clientQosEntry.getKey(), (int)(lastMessageIdProgress % 65535));
                        return null;
                    });
                }
                catch (Throwable e){
                    log.error("生成消息id失败", e);
                }
                finally {
                    countDownLatch.countDown();
                }
            });
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.warn("中断生成消息id", e);
        }

        messageIdThreadPool.shutdown();

        for (SendMessageDto sendMessageDto : sendMessageDtoList){
            if (!sendMessageDto.getSendQos().equals(Qos.LEVEL_0)){
                Integer sendPacketId = sendMessageIdMap.get(sendMessageDto.getToClientId());

                if (sendPacketId == null){
                    continue;
                }

                sendMessageDto.setSendPacketId(sendPacketId);
            }

            InsertSendMessageQueue.QUEUE.add(sendMessageDto);
        }
    }
}
