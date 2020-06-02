package com.ruijie.listenevent.utils;

import java.text.SimpleDateFormat;

public class DateUtils {

    public static String getCurrentTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");
        return simpleDateFormat.format(System.currentTimeMillis());
    }

}
