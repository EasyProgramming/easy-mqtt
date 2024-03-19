package com.ep.mqtt.server.aliyun.vo;

import lombok.Data;

/**
 * @author zbz
 * @date 2023/12/6 9:54
 */
@Data
public class TokenVo {

    /**
     * 资源名称，参考链接的Resources字段
     * <p>https://help.aliyun.com/document_detail/2603935.html?spm=a2c4g.2603932.0.0.b821426ey4oiha</p>
     */
    private String resources;

    /**
     * 客户端id
     */
    private String clientId;

    /**
     * 失效的毫秒时间戳，参考链接的ExpireTime字段
     * <p>https://help.aliyun.com/document_detail/2603935.html?spm=a2c4g.2603932.0.0.b821426ey4oiha</p>
     */
    private Long expireTime;

    /**
     * 权限类型，参考链接的Actions字段
     * <p>https://help.aliyun.com/document_detail/2603935.html?spm=a2c4g.2603932.0.0.b821426ey4oiha</p>
     */
    private String actions;

    /**
     * 创建的时间戳
     */
    private Long createTime;


    /**
     * 最近一次刷新的时间戳
     */
    private Long lastRefreshTime;

}
