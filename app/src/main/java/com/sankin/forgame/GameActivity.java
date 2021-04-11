package com.sankin.forgame;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sankin.forgame.Util.Constant;

import java.util.Random;

public class GameActivity extends AppCompatActivity implements SensorEventListener {
    private ImageView user, attacker; //用户
    private View whole;  //整个view

    private float[] angel = new float[3];  //陀螺仪数据
    private SensorManager sensorManager = null; //传感器管理
    private Sensor gyroSensor = null;   //陀螺仪
    private boolean is_defense = false;  //是否防御
    private static int count = 0, imageCount = 0;
    private static long firClick, secClick;
    private int image[] = new int[]{
            R.drawable.ic_gun,
            R.drawable.ic_pao,
    };
    private int height;//屏幕高度
    private int width;//屏幕宽度
    private int duration;//动画时间
    private int left;//距离左边界的距离
    private Bitmap bitmap, at;//鲜花的对象、敌人的对象
    private ImageView imageView;

    //定义两个矩形的宽高坐标
    private float x1 = 0, y1 = 0, w1 = 0, h1 = 0;
    private float x2 = 0, y2 = 0, w2 = 0, h2 = 0;

    //插入背景乐
    private MediaPlayer mediaPlayer;
    private MediaPlayer hint;
    private MediaPlayer mp1, mp2;

    Random random = new Random();

    Handler mhandler = new Handler();

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.P)
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        user = findViewById(R.id.iv_gun);
        whole = findViewById(R.id.v_bg);
        imageView = findViewById(R.id.iv_health);
        attacker = findViewById(R.id.iv_attacker);
        user.setImageResource(image[imageCount]);
        //DoubleClick 盾牌
        whole.setOnTouchListener(new onDoubleClick());
        //Click 射击
        whole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("我方", "射击");
                mediaPlayer.start();
                checkDirection();
            }
        });

        //LongClick 暂停


        Point p = new Point();
        //获取窗口管理器
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(p);
        width = p.x;
        height = p.y;
        Log.d("height", ""+height);


        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_health_value);
        at = BitmapFactory.decodeResource(getResources(), R.drawable.ic_attacker);
        mhandler.post(runnable);//start

        //获取传感器信息
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //加速计

        mediaPlayer = MediaPlayer.create(this, R.raw.shoot);
        mp1 = MediaPlayer.create(this, R.raw.sound_pao);
        mp2 = MediaPlayer.create(this, R.raw.sound_health);
        hint = MediaPlayer.create(this, R.raw.hint);
        hint.setLooping(true);
        generateAttacker();
    }


    @Override
    protected void onPause() {
        super.onPause();
        //解除传感器监听
        sensorManager.unregisterListener(this);
        hint.pause();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        //为传感器注册监听器
        sensorManager.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        hint.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaPlayer over = MediaPlayer.create(this, R.raw.over);
        over.start();
    }

    //检查玩家是否被敌人的子弹命中


    //传感器变化
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            synchronized (angel) {
                angel[0] = event.values[0]; //x
                angel[1] = event.values[1]; //y
                angel[2] = event.values[2]; //z
                synchronized (attacker) {
                    CalculationAngle();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 检查夹角
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkDirection() {
        synchronized (attacker) {
            MediaPlayer shoot = MediaPlayer.create(this, R.raw.great);
            //检查角度，夹角小于5°认为瞄准，杀死该敌人
            if (CalculationAngle()) {
                Log.d("我方", "击中敌人");
                Vibrator vibrator = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);
                vibrator.vibrate(100);
                shoot.start();
                attacker.setVisibility(View.INVISIBLE);
                generateAttacker();
            }
        }
    }

    /*
        计算夹角
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean CalculationAngle() {
        float[] point = new float[3];
        point[0] = attacker.getX();
        point[1] = attacker.getY();
        point[2] = attacker.getZ();
        Log.d("敌人坐标 ",  point[0] + " " + point[1] + " " + point[2]);
        Log.d("传感器坐标" ,angel[0] + " " + angel[1] + " " + angel[2]);
        double dis1 = Math.sqrt(point[0] * point[0] + point[1] * point[1] + point[2] * point[2]);
        double dis2 = Math.sqrt(angel[0] * angel[0] + angel[1] * angel[1] + angel[2] * angel[2]);
        double multi = point[0] * angel[0] + point[1] * angel[1] + point[2] * angel[2];
        double includedAngle = Math.acos(multi / (dis1 * dis2));
        if (Double.compare(includedAngle, Constant.STANDARD) < 0 && Double.compare(includedAngle, -Constant.STANDARD) > 0) {
            hint.pause();
            hint.getPlaybackParams().setSpeed(5f);
            hint.start();
            return true;
        }
        changeMusicFrequency(includedAngle);
        return false;
    }

    //改变音乐频率
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void changeMusicFrequency(double angle) {
        hint.pause();
        if (Double.compare(angle, Constant.STANDARD_30) < 0) {
            Log.d("角度","0-30----------");
            hint.getPlaybackParams().setSpeed(3f);
        }
        else if (Double.compare(angle, Constant.STANDARD_60) < 0) {
            Log.d("角度","30-60----------");
            hint.getPlaybackParams().setSpeed(2f);
        }else {
            Log.d("角度","60-90----------");
            hint.getPlaybackParams().setSpeed(1f);
        }
        hint.start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //产生敌人、此时双方攻击均无效
    public void generateAttacker() {
        synchronized (attacker) {
            Log.d("序号","-------generate attacker");
            //随机生成敌人位置
            attacker.setImageBitmap(at);
            left = random.nextInt(width - 200) + 100;
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
            params.setMargins(left, 0, 0, 0);
            attacker.setLayoutParams(params);
            attacker.setVisibility(View.VISIBLE);
        }
    }

    private void randomFlower() {
        imageView.setImageBitmap(bitmap);
        duration = new Random().nextInt(2000) + 2000;
        left = random.nextInt(width - 200) + 100;

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.setMargins(left, 0, 0, 0);
        imageView.setLayoutParams(params);

        imageView.setVisibility(View.VISIBLE);
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(imageView, "translationY", 0, 6000);
        objectAnimator.setDuration(6000).start();
        y2 = imageView.getTop();

    }

    protected void myDestroy(){
        y2 = imageView.getTop();
        boolean ic = isCollsion();
        Log.d("是否碰撞", ic+"");
        if(ic || imageView.getTranslationY() >= height){
            imageView.setVisibility(View.INVISIBLE);
        }
    }

    protected boolean isCollsion(){
        // 飞机的位置和长宽
        y1 = user.getTop();
        x1 = user.getLeft();
        w1 = user.getWidth();
        h1 = user.getHeight();
        Log.d("飞机", "位置：x="+x1+",y="+y1+"   大小：w="+w1+",h="+h1);

        // 生命值的位置和长宽
        y2 = imageView.getTop();
        x2 = imageView.getLeft();
        w2 = imageView.getWidth();
        h2 = imageView.getHeight();
        Log.d("生命值", "位置：x="+x2+",y="+y2+"   大小：w="+w2+",h="+h2);

        // 获取屏幕的长宽
        float scr_w = getScreenWidth(this);
        float src_h = getScreenHeight(this);
        if(x2+w2 >= x1 && x2+w2 <= w1+x1){
            mp2.start();
            return true;
        }

        return false;
    }

    class onDoubleClick implements View.OnTouchListener{
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(MotionEvent.ACTION_DOWN == event.getAction()){
                count++;
                if(count == 1){
                    firClick = System.currentTimeMillis();

                } else if (count == 2){
                    secClick = System.currentTimeMillis();
                    if(secClick - firClick < 1000){
                        //双击事件
                        if(++imageCount>=image.length){
                            imageCount=0;
                        }
                        user.setImageResource(image[imageCount]);
                        Log.d("count", count+"");
                        Log.d("imageCount", imageCount+"");
                        Log.d("image[imageCount]", image[imageCount]+"");
                    }
                    count = 0;
                    firClick = 0;
                    secClick = 0;
                }
            }
//            isCollsion();
            mp1.start();
            return false;
        }
    }

    // 获得屏幕的宽度
    public static int getScreenWidth(Context ctx) {
        // 从系统服务中获取窗口管理器
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        // 从默认显示器中获取显示参数保存到dm对象中
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels; // 返回屏幕的宽度数值
    }

    // 获得屏幕的高度
    public static int getScreenHeight(Context ctx) {
        // 从系统服务中获取窗口管理器
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        // 从默认显示器中获取显示参数保存到dm对象中
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels; // 返回屏幕的高度数值
    }

}