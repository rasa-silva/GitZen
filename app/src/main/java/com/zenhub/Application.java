package com.zenhub;

import android.annotation.SuppressLint;
import android.content.Context;

import com.squareup.picasso.Picasso;

@SuppressLint("StaticFieldLeak")
public class Application extends android.app.Application {

    public static Context context;
    public static Picasso picasso;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        picasso = Picasso.with(context);
        picasso.setIndicatorsEnabled(true);
    }
}
