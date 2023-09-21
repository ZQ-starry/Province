package com.sx.server0.util;


import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author zhangQi
 * @date 2022/5/13 - 19:43
 */
public class DateUtils {
    public String timelineUtil(int count) {
        Calendar c = Calendar.getInstance();
        //月份从0算起
        c.set(2022, 0, 1, 1, 0);
        c.add(Calendar.MINUTE, 15 * count);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd  HH:mm");
        String time = format.format(c.getTime());
        return time;
    }
}
