package arko.chatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        //adjustFontScale(getResources().getConfiguration());

        SharedPreferences.Editor ed = getSharedPreferences("sinch_service", MODE_PRIVATE).edit();
        ed.putBoolean("isLogin", false);
        ed.apply();

        Thread thread = new Thread(){

            @Override
            public void run() {

                try {

                    sleep(2000);
                }
                catch (Exception e){

                    e.printStackTrace();
                }
                finally {

                    Intent mainIntent = new Intent(SplashActivity.this, StartActivity.class);
                    startActivity(mainIntent);
                }
            }
        };
        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    /*public void adjustFontScale(Configuration configuration) {

        if (configuration != null) {

            Log.d("TAG", "adjustDisplayScale: " + configuration.densityDpi);
            if (configuration.densityDpi >= 485) //for 6 inch device OR for 538 ppi
                configuration.densityDpi = 600; //decrease "display size" by ~30

            else if (configuration.densityDpi >= 300) //for 5.5 inch device OR for 432 ppi
                configuration.densityDpi = 600; //decrease "display size" by ~30

            else if (configuration.densityDpi >= 100) //for 4 inch device OR for 233 ppi
                configuration.densityDpi = 200; //decrease "display size" by ~30

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
            metrics.scaledDensity = configuration.densityDpi * metrics.density;
            this.getResources().updateConfiguration(configuration, metrics);
        }
    }*/
}