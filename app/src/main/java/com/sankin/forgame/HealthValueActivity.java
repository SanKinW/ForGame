package com.sankin.forgame;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.iigo.library.XAnimator;

import androidx.appcompat.app.AppCompatActivity;


import android.os.Handler;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;


import java.util.Random;

public class HealthValueActivity extends AppCompatActivity {

    private RelativeLayout main_container;//容器 xml也就这一个控件
    private int height;//屏幕高度
    private int width;//屏幕宽度
    private int duration;//动画时间
    private int left;//距离左边界的距离
    private HealthValueActivity activity;
    private Bitmap bitmap;//鲜花的对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(XAnimator.initLayout(this, R.layout.activity_health_value));
        activity = this;

        main_container = (RelativeLayout) findViewById(R.id.main_container);

        height = getResources().getDisplayMetrics().heightPixels;
        width = getResources().getDisplayMetrics().widthPixels;

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_health_value);

        mhandler.post(runnable);//start
    }

    Random random = new Random();

    Handler mhandler = new Handler();

    Runnable runnable = new Runnable() {
        @Override
        public void run() {



            randomFlower();

            mhandler.postDelayed(this, 300);
        }
    };

    private void randomFlower() {
        final ImageView imageView = new ImageView(activity);
        main_container.addView(imageView);
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        duration = new Random().nextInt(2000) + 2000;
        left = random.nextInt(width - 200) + 100;

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams
                (100, 100);
        params.setMargins(left, 0, 0, 0);
        imageView.setLayoutParams(params);

//        XanimAnimator.from(imageView)
//                .anim(duration, new XanimMaker() {
//                    @Override
//                    public void onAnim(Xanim anim) {
//                        anim.translateY(height);
//                    }
//                }).animInterpolator(new LinearInterpolator())
//                .subscribe(new OnSubscribe<View>() {
//                    @Override
//                    public void onNext(View view) {
//
//                    }
//
//                    @Override
//                    public void onComplete() {
//                        main_container.removeView(imageView);
//                    }
//
//                    @Override
//                    public void onError(Throwable t) {
//
//                    }
//                });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mhandler.removeCallbacks(runnable);
        mhandler = null;
        bitmap.recycle();
    }


}