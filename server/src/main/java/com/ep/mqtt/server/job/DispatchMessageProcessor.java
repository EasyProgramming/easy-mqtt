package com.ep.mqtt.server.job;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ep.mqtt.server.db.dao.ClientSubscribeDao;
import com.ep.mqtt.server.db.dao.SendMessageDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.db.dto.ClientSubscribeDto;
import com.ep.mqtt.server.db.dto.SendMessageDto;
import com.ep.mqtt.server.metadata.*;
import com.ep.mqtt.server.store.TopicFilterStore;
import com.ep.mqtt.server.util.JsonUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author : zbz
 * @date : 2025/2/1
 */
@Component
public class DispatchMessageProcessor extends AbstractJobProcessor<DispatchMessageParam> {

    @Resource
    private ClientSubscribeDao subscribeDao;

    @Resource
    private SendMessageDao sendMessageDao;

    @Resource
    private AsyncJobManage asyncJobManage;

    public DispatchMessageProcessor() {
        super(new ThreadPoolExecutor(Constant.PROCESSOR_NUM * 2, Constant.PROCESSOR_NUM * 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("dispatch-message-%s").build()));
    }

    @Override
    public AsyncJobExecuteResult process(AsyncJobDto asyncJobDto, DispatchMessageParam jobParam) {
        // 匹配topic filter
        List<String> matchTopicFilterList = TopicFilterStore.matchTopicFilter(jobParam.getTopic());
        if (CollectionUtils.isEmpty(matchTopicFilterList)){
            return AsyncJobExecuteResult.SUCCESS;
        }

        // 根据topic filter及id游标查询匹配的客户端，并计算qos
        Map<String, Qos> clientQosMap = Maps.newHashMap();
        List<ClientSubscribeDto> clientSubscribePage;
        Long cursor = 0L;
        int pageSize = 50000;
        do {
            clientSubscribePage = subscribeDao.selectByCursor(Sets.newHashSet(matchTopicFilterList), cursor, pageSize);
            cursor = clientSubscribePage.get(clientSubscribePage.size() - 1).getId();

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
        }while (clientSubscribePage.size() >= pageSize);

        if (CollectionUtils.isEmpty(clientQosMap)){
            return AsyncJobExecuteResult.SUCCESS;
        }

        Long validTime = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7;
        List<SendMessageDto> qos0MessageDtoList = Lists.newArrayList();
        List<SendMessageDto> otherMessageDtoList = Lists.newArrayList();
        for (Map.Entry<String, Qos> clientQosEntry : clientQosMap.entrySet()){
            SendMessageDto sendMessageDto = new SendMessageDto();
            sendMessageDto.setReceiveQos(jobParam.getReceiveQos());
            sendMessageDto.setReceivePacketId(jobParam.getReceivePacketId());
            sendMessageDto.setFromClientId(jobParam.getFromClientId());
            sendMessageDto.setSendQos(clientQosEntry.getValue().getCode() >= jobParam.getReceiveQos().getCode() ? jobParam.getReceiveQos() :
                    clientQosEntry.getValue());
            sendMessageDto.setTopic(jobParam.getTopic());
            sendMessageDto.setToClientId(clientQosEntry.getKey());
            sendMessageDto.setPayload(jobParam.getPayload());
            sendMessageDto.setIsReceivePubRec(YesOrNo.NO);
            sendMessageDto.setValidTime(validTime);
            sendMessageDto.setIsRetain(YesOrNo.NO);

            if (sendMessageDto.getSendQos() == Qos.LEVEL_0) {
                qos0MessageDtoList.add(sendMessageDto);
            } else {
                otherMessageDtoList.add(sendMessageDto);
            }
        }
        sendMessageDao.insert(otherMessageDtoList, 10000);

        Long now = System.currentTimeMillis();
        List<AsyncJobDto> genMessageIdAsyncJobDtoList = Lists.newArrayList();
        for (SendMessageDto sendMessageDto : qos0MessageDtoList) {
            genMessageIdAsyncJobDtoList.add(buildGenMessageIdAsyncJobDto(sendMessageDto, now));
        }
        for (SendMessageDto sendMessageDto : otherMessageDtoList) {
            genMessageIdAsyncJobDtoList.add(buildGenMessageIdAsyncJobDto(sendMessageDto, now));
        }
        asyncJobManage.addJob(genMessageIdAsyncJobDtoList);

        return AsyncJobExecuteResult.SUCCESS;
    }

    private AsyncJobDto buildGenMessageIdAsyncJobDto(SendMessageDto sendMessageDto, Long now) {
        AsyncJobDto genMessageIdAsyncJobDto = new AsyncJobDto();

        if (sendMessageDto.getSendQos() == Qos.LEVEL_0) {
            genMessageIdAsyncJobDto.setBusinessId(AsyncJobBusinessType.GEN_MESSAGE_ID.getBusinessId(UUID.randomUUID().toString()));
        } else {
            genMessageIdAsyncJobDto.setBusinessId(AsyncJobBusinessType.GEN_MESSAGE_ID.getBusinessId(sendMessageDto.getId()));
        }

        GenMessageIdParam genMessageIdParam = new GenMessageIdParam();
        genMessageIdParam.setSendMessageId(sendMessageDto.getId());
        genMessageIdParam.setSendQos(sendMessageDto.getSendQos());
        genMessageIdParam.setTopic(sendMessageDto.getTopic());
        genMessageIdParam.setToClientId(sendMessageDto.getToClientId());
        genMessageIdParam.setPayload(sendMessageDto.getPayload());
        genMessageIdParam.setIsRetain(sendMessageDto.getIsRetain());
        genMessageIdAsyncJobDto.setJobParam(JsonUtil.obj2String(genMessageIdParam));

        genMessageIdAsyncJobDto.setBusinessType(AsyncJobBusinessType.GEN_MESSAGE_ID);
        genMessageIdAsyncJobDto.setExpectExecuteTime(now);
        genMessageIdAsyncJobDto.setExecuteNum(0);
        genMessageIdAsyncJobDto.setExecuteStatus(AsyncJobStatus.READY);

        return genMessageIdAsyncJobDto;
    }

    @NonNull
    @Override
    public AsyncJobBusinessType getBusinessType() {
        return AsyncJobBusinessType.DISPATCH_MESSAGE;
    }

    @NonNull
    @Override
    public Integer getRetryInterval() {
        return 60;
    }

    @Override
    public Integer getMaxRetryNum() {
        return null;
    }
}
