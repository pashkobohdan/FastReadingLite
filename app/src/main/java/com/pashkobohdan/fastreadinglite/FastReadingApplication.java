package com.pashkobohdan.fastreadinglite;

import android.app.Application;

import com.google.android.gms.ads.MobileAds;
import com.pashkobohdan.fastreadinglite.data.database.BookDAOHolder;

/**
 * Created by bohdan on 06.08.17.
 */

public class FastReadingApplication extends Application {
    private static final String ADMOB_APP_ID = "ca-app-pub-5629629863028147~9936438114";

    @Override
    public void onCreate() {
        super.onCreate();
        BookDAOHolder.init(getApplicationContext());
        MobileAds.initialize(this, ADMOB_APP_ID );
    }
}
