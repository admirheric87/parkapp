package com.admirheric.parkapp;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by admirheric on 03/08/2017.
 */

public class Utils {

    public static final String LOG_TAG = "ParkApp";

    public static void log(String s){
        Log.d(LOG_TAG, s);
    }

    public static String getHoursMinutes(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        String time = "";
        if(hour>9){
            time = time + hour + ":";
        } else {
            time = time + "0" + hour + ":";
        }
        if(minute>9){
            time = time + minute;
        } else {
            time = time + "0" + minute;
        }
        return time;
    }
}
