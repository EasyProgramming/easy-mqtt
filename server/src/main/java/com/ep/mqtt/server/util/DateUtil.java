package com.ep.mqtt.server.util;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

/**
 * @author : zbz
 * @date : 2025/3/2
 */
public class DateUtil {

    public static Date getMidnight() {
        Date now = new Date();
        DateUtils.setHours(now, 0);
        DateUtils.setMinutes(now, 0);
        DateUtils.setSeconds(now, 0);
        DateUtils.setMilliseconds(now, 0);

        return now;
    }

}
