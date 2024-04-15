package com.ep.mqtt.server.raft.transfer;

import com.ep.mqtt.server.util.JsonUtil;

import lombok.Data;

/**
 * @author zbz
 * @date 2024/4/10 16:02
 */
@Data
public class TransferData {

    private String command;

    private String data;

    public TransferData(String command, String data) {
        this.command = command;
        this.data = data;
    }

    public static TransferData convert(String jsonStr) {
        return JsonUtil.string2Obj(jsonStr, TransferData.class);
    }
}
