package com.admirheric.parkapp;

import android.content.SharedPreferences;
import android.support.multidex.MultiDexApplication;

import com.parkbob.ParkbobManager;

/**
 * Created by admirheric on 24/07/2017.
 */

public class MyApplication extends MultiDexApplication {

    public static final String PREFERENCES_NAME = "parkapp_prefs";
    public static final String PREFRENCES_IS_FIRST_RUN = "is_first_run";

    private static MyApplication sInstance;

    SharedPreferences mPref;

    public static MyApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        mPref = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);

        ParkbobManager.initInstance(this);

    }

    public boolean isFirstRun() {
        // return true if the app is running for the first time
        return mPref.getBoolean(PREFRENCES_IS_FIRST_RUN, true);
    }

    public void setRunned() {
        // after a successful run, call this method to set first run false
        SharedPreferences.Editor edit = mPref.edit();
        edit.putBoolean(PREFRENCES_IS_FIRST_RUN, false);
        edit.commit();
    }

}
