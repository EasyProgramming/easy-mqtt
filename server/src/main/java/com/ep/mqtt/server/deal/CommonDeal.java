package com.ep.mqtt.server.deal;

import com.ep.mqtt.server.db.dao.ClientDao;
import com.ep.mqtt.server.db.dao.ClientSubscribeDao;
import com.ep.mqtt.server.db.dao.ReceiveQos2MessageDao;
import com.ep.mqtt.server.db.dao.SendMessageDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.db.dto.ClientDto;
import com.ep.mqtt.server.db.dto.ClientSubscribeDto;
import com.ep.mqtt.server.db.dto.SendMessageDto;
import com.ep.mqtt.server.job.AsyncJobManage;
import com.ep.mqtt.server.job.DispatchMessageParam;
import com.ep.mqtt.server.metadata.DisconnectReason;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.YesOrNo;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.store.TopicFilterStore;
import com.ep.mqtt.server.util.ModelUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

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

        Long validTime = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7;
        List<SendMessageDto> qos0MessageDtoList = Lists.newArrayList();
        List<SendMessageDto> otherMessageDtoList = Lists.newArrayList();
        for (Map.Entry<String, Qos> clientQosEntry : clientQosMap.entrySet()){
            SendMessageDto sendMessageDto = ModelUtil.buildSendMessageDto(
                    dispatchMessageParam.getReceiveQos(),
                    dispatchMessageParam.getReceivePacketId(),
                    dispatchMessageParam.getFromClientId(),
                    clientQosEntry.getValue().getCode() >= dispatchMessageParam.getReceiveQos().getCode() ? dispatchMessageParam.getReceiveQos() :
                            clientQosEntry.getValue(),
                    dispatchMessageParam.getTopic(),
                    null,
                    clientQosEntry.getKey(),
                    dispatchMessageParam.getPayload(),
                    YesOrNo.NO,
                    validTime,
                    dispatchMessageParam.getIsRetain()
            );

            if (sendMessageDto.getSendQos() == Qos.LEVEL_0) {
                qos0MessageDtoList.add(sendMessageDto);
            } else {
                otherMessageDtoList.add(sendMessageDto);
            }
        }
        sendMessageDao.insert(otherMessageDtoList);

        Long now = System.currentTimeMillis();
        List<AsyncJobDto> genMessageIdAsyncJobDtoList = Lists.newArrayList();
        for (SendMessageDto sendMessageDto : qos0MessageDtoList) {
            genMessageIdAsyncJobDtoList.add(ModelUtil.buildGenMessageIdAsyncJobDto(sendMessageDto, now));
        }
        for (SendMessageDto sendMessageDto : otherMessageDtoList) {
            genMessageIdAsyncJobDtoList.add(ModelUtil.buildGenMessageIdAsyncJobDto(sendMessageDto, now));
        }
        asyncJobManage.addJob(genMessageIdAsyncJobDtoList);
    }

}
