package com.ep.mqtt.server.deal;

import com.ep.mqtt.server.db.dao.ClientDao;
import com.ep.mqtt.server.db.dao.ClientSubscribeDao;
import com.ep.mqtt.server.db.dao.ReceiveQos2MessageDao;
import com.ep.mqtt.server.db.dao.SendMessageDao;
import com.ep.mqtt.server.db.dto.ClientDto;
import com.ep.mqtt.server.db.dto.ClientSubscribeDto;
import com.ep.mqtt.server.job.AsyncJobManage;
import com.ep.mqtt.server.job.DispatchMessageParam;
import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.metadata.DisconnectReason;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.YesOrNo;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.store.TopicFilterStore;
import com.ep.mqtt.server.util.ModelUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
@Component
public class CommonDeal {

    @Resource
    private ClientDao clientDao;

    @Resource
    private ClientSubscribeDao clientSubscribeDao;

    @Resource
    private ReceiveQos2MessageDao receiveQos2MessageDao;

    @Resource
    private SendMessageDao sendMessageDao;

    @Resource
    private AsyncJobManage asyncJobManage;

    @Resource
    private ClientSubscribeDao subscribeDao;

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
        // 匹配topic filter
        List<String> matchTopicFilterList = TopicFilterStore.matchTopicFilter(dispatchMessageParam.getTopic());
        if (CollectionUtils.isEmpty(matchTopicFilterList)){
            return;
        }

        // 根据topic filter及id游标查询匹配的客户端，并计算qos
        Map<String, Qos> clientQosMap = Maps.newHashMap();
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

        if (CollectionUtils.isEmpty(clientQosMap)){
            return;
        }

        /*
            创建临时线程池
            使用栅栏控制批量生成消息id
            关闭线程池
            加入本地队列
         */

        // TODO: 2025/4/3 多线程生成消息id
        Map<String, Integer> messageIdMap = Maps.newConcurrentMap();
        ThreadPoolExecutor messageIdThreadPool = new ThreadPoolExecutor(Constant.PROCESSOR_NUM, Constant.PROCESSOR_NUM * 2,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(clientQosMap.size()),
                new ThreadFactoryBuilder().setNameFormat("gen-message-id-%s").build());
        CountDownLatch countDownLatch = new CountDownLatch(clientQosMap.size());
        for (Map.Entry<String, Qos> clientQosEntry : clientQosMap.entrySet()){
            messageIdThreadPool.submit(()->{
                try {
                    messageIdMap.put(clientQosEntry.getKey(), 1);
                }
                catch (Throwable e){

                }
                finally {
                    countDownLatch.countDown();
                }
            });
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {

        }
        messageIdThreadPool.shutdown();;

        // TODO: 2025/4/3 构建dto，加入本地队列
        for (Map.Entry<String, Qos> clientQosEntry : clientQosMap.entrySet()){
            ModelUtil.buildGenMessageIdParam(
                    dispatchMessageParam.getReceiveQos(),
                    dispatchMessageParam.getReceivePacketId(),
                    dispatchMessageParam.getFromClientId(),
                    clientQosEntry.getValue().getCode() >= dispatchMessageParam.getReceiveQos().getCode() ?
                            dispatchMessageParam.getReceiveQos() : clientQosEntry.getValue(),
                    dispatchMessageParam.getTopic(),
                    clientQosEntry.getKey(),
                    dispatchMessageParam.getPayload(),
                    YesOrNo.NO,
                    dispatchMessageParam.getIsRetain()
            );



        }




    }

}
