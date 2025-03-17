package com.ep.mqtt.server.db.dao.h2;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dao.AsyncJobDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
public interface AsyncJobH2Dao extends AsyncJobDao {

    /**
     * 对任务加锁
     * 
     * @param businessId
     *            业务id
     * @return 任务数据
     */
    @Override
    default AsyncJobDto lock(String businessId) {
        update(Wrappers.lambdaUpdate(AsyncJobDto.class).set(AsyncJobDto::getBusinessId, businessId)
            .eq(AsyncJobDto::getBusinessId, businessId));

        return selectOne(Wrappers.lambdaUpdate(AsyncJobDto.class).eq(AsyncJobDto::getBusinessId, businessId));
    }

}