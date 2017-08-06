package com.pashkobohdan.fastreadinglite;

import android.app.Application;

import com.google.android.gms.ads.MobileAds;

/**
 * Created by bohdan on 06.08.17.
 */

public class FastReadingApplication extends Application {
    private static final String ADMOB_APP_ID = "ca-app-pub-5629629863028147~9936438114";

    @Override
    public void onCreate() {
        super.onCreate();

        MobileAds.initialize(this, ADMOB_APP_ID );
    }
}
