package com.instamenu.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.instamenu.R;
import com.instamenu.util.LogWrapper;


public class SplashActivity extends Activity {

    private int duration = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    LogWrapper.e("Splash", e.getMessage());
                }

                //startActivity(new Intent(SplashActivity.this, CameraActivity.class));
                startActivity(new Intent(SplashActivity.this, MainActivity.class));

                finish();
            }
        }).start();
    }
}
