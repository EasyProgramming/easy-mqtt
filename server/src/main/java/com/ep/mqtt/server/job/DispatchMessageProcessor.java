package com.ep.mqtt.server.job;

import com.ep.mqtt.server.db.dao.ClientSubscribeDao;
import com.ep.mqtt.server.db.dao.SendMessageDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.db.dto.ClientSubscribeDto;
import com.ep.mqtt.server.db.dto.SendMessageDto;
import com.ep.mqtt.server.metadata.*;
import com.ep.mqtt.server.store.TopicFilterStore;
import com.ep.mqtt.server.util.ModelUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
            SendMessageDto sendMessageDto = ModelUtil.buildSendMessageDto(
                    jobParam.getReceiveQos(),
                    jobParam.getReceivePacketId(),
                    jobParam.getFromClientId(),
                    clientQosEntry.getValue().getCode() >= jobParam.getReceiveQos().getCode() ? jobParam.getReceiveQos() :
                            clientQosEntry.getValue(),
                    jobParam.getTopic(),
                    null,
                    clientQosEntry.getKey(),
                    jobParam.getPayload(),
                    YesOrNo.NO,
                    validTime,
                    YesOrNo.NO
            );

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
            genMessageIdAsyncJobDtoList.add(ModelUtil.buildGenMessageIdAsyncJobDto(sendMessageDto, now));
        }
        for (SendMessageDto sendMessageDto : otherMessageDtoList) {
            genMessageIdAsyncJobDtoList.add(ModelUtil.buildGenMessageIdAsyncJobDto(sendMessageDto, now));
        }
        asyncJobManage.addJob(genMessageIdAsyncJobDtoList);

        return AsyncJobExecuteResult.SUCCESS;
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
