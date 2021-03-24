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

import com.sankin.forgame.Threads.MoveThread;
import com.sankin.forgame.Util.Constant;
import com.sankin.forgame.Util.Format;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity implements SensorEventListener {
    private ImageView user; //用户
    private View whole;  //整个view
    //private RelativeLayout main_container;//容器 xml也就这一个控件
    //private int lastX; //手指最后的位置

    private float[] angel = new float[3];  //陀螺仪数据
    private SensorManager sensorManager = null; //传感器管理
    private Sensor gyroSensor = null;   //陀螺仪
    private List<MoveThread> list = new LinkedList<>(); //敌人序列
    private boolean is_defense = false;  //是否防御
    private int lifeValue = 666; //初始生命值
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
    //private GameActivity activity;
    private Bitmap bitmap;//鲜花的对象
    private ImageView imageView;
    //private MySurfaceView mySurfaceView;

    //定义两个矩形的宽高坐标
    private float x1 = 0, y1 = 0, w1 = 0, h1 = 0;
    private float x2 = 0, y2 = 0, w2 = 0, h2 = 0;

    //插入背景乐
    private MediaPlayer mediaPlayer;
    private MediaPlayer mp1, mp2;

    Random random = new Random();

    Handler mhandler = new Handler();

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mhandler.postDelayed(this, 5000);
            randomFlower();
            myDestroy();
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        user = findViewById(R.id.iv_gun);
        whole = findViewById(R.id.v_bg);
        imageView = findViewById(R.id.iv_health);
        //main_container = findViewById(R.id.main_container);
        user.setImageResource(image[imageCount]);
        whole.setOnTouchListener(new onDoubleClick());
        whole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                is_defense = true;
                try {
                    Thread.sleep(1000);
                }catch (Exception e) {
                    e.printStackTrace();
                }
                is_defense = false;
            }
        });

        whole.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                synchronized (list) {
                    synchronized (angel) {
                        int count = 5;
                        while (count-- > 0) {
                            Log.d("我方", "射击");
                            checkDirection();
                            mediaPlayer.start();
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return true;
            }
        });
        //activity = this;

        Point p = new Point();
        //获取窗口管理器
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(p);
        width = p.x;
        height = p.y;
        Log.d("height", ""+height);


        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_health_value);
        mhandler.post(runnable);//start

        //获取传感器信息
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mediaPlayer = MediaPlayer.create(this, R.raw.shoot);
        mp1 =MediaPlayer.create(this, R.raw.sound_pao);
        mp2 =MediaPlayer.create(this, R.raw.sound_health);

        //产生敌人
        new Thread(){
            @Override
            public void run(){
                while(true){
                    try{
                        generateAttacker();
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        //是否被敌人击中
        new Thread(){
            @Override
            public void run(){
                while(true){
                    try{
                        isBeShoot();
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }


    @Override
    protected void onPause() {
        super.onPause();
        //解除传感器监听
        sensorManager.unregisterListener(this);
        mediaPlayer.pause();
        mp1.pause();
        mp2.pause();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        //为传感器注册监听器
        sensorManager.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        y2 = imageView.getTop();
        boolean ic = isCollsion();
        Log.d("是否碰撞", ic+"");
        if(ic || imageView.getTranslationY() >= height){
            mhandler.removeCallbacks(runnable);
            mhandler = null;
            bitmap.recycle();
        }
        MediaPlayer over = MediaPlayer.create(this, R.raw.over);
        over.start();
        finish();

    }

    //检查玩家是否被敌人的子弹命中
    public void isBeShoot() {
        synchronized (list) {
            if (!is_defense) {
                MediaPlayer shoot_down = MediaPlayer.create(this, R.raw.shooted);
                for (MoveThread move : list) {
                    ImageView image = move.getImageView();
                    //检查角度，夹角小于10°认为瞄准，杀死该敌人
                    if (image.getX() - user.getX() > -5.0 && image.getX() - user.getX() < 5.0) {
                        Log.d("敌人射击", "被敌人击中");
                        shoot_down.start();
                        lifeValue = lifeValue - 3;
                    }
                    if (lifeValue <= 0) onDestroy();
                }
            }
        }
    }

    //传感器变化
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            synchronized (angel) {
                angel[0] = event.values[0]; //x
                angel[1] = event.values[1]; //y
                angel[2] = event.values[2]; //z
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
        MediaPlayer shoot = MediaPlayer.create(this, R.raw.kill);
        MoveThread temp = null;
        for (MoveThread move : list) {
            ImageView image = move.getImageView();
            //检查角度，夹角小于5°认为瞄准，杀死该敌人
            if (CalculationAngle(image)) {
                Log.d("我方", "击中敌人");
                Vibrator vibrator = (Vibrator)this.getSystemService(this.VIBRATOR_SERVICE);
                vibrator.vibrate(100);
                shoot.start();
                move.setFlag(false);
                temp = move;
                list.notifyAll();
                break;
            }
        }
        if (temp != null) list.remove(temp);
    }

    /*
        计算夹角
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean CalculationAngle(ImageView imageView) {
        synchronized (imageView) {
            float[] point = new float[3];
            point[0] = imageView.getX();
            point[1] = imageView.getY();
            point[2] = imageView.getZ();
            Log.d("敌人坐标 ",  point[0] + " " + point[1] + " " + point[2]);
            Log.d("传感器坐标" ,angel[0] + " " + angel[1] + " " + angel[2]);
            double dis1 = Math.sqrt(point[0] * point[0] + point[1] * point[1] + point[2] * point[2]);
            double dis2 = Math.sqrt(angel[0] * angel[0] + angel[1] * angel[1] + angel[2] * angel[2]);
            double multi = point[0] * angel[0] + point[1] * angel[1] + point[2] * angel[2];
            double includedAngle = Math.acos(multi / (dis1 * dis2));
            if (Double.compare(includedAngle, Constant.STANDARD) < 0 && Double.compare(includedAngle, -Constant.STANDARD) > 0) {
                mediaPlayer.getPlaybackParams().setSpeed(6f);
                return true;
            }
            else {
                //在一定范围内音效变化
                changeMusicFrequency(includedAngle);
                return false;
            }
        }
    }

    //改变音乐频率
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void changeMusicFrequency(double angle) {
        if (Double.compare(angle, Constant.STANDARD_30) < 0) {
            Log.d("角度","0-30----------");
            mediaPlayer.getPlaybackParams().setSpeed(5f);
        }
        else if (Double.compare(angle, Constant.STANDARD_60) < 0) {
            Log.d("角度","30-60----------");
            mediaPlayer.getPlaybackParams().setSpeed(2.5f);
        }else {
            Log.d("角度","60-90----------");
            mediaPlayer.getPlaybackParams().setSpeed(1f);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //产生敌人、此时双方攻击均无效
    public void generateAttacker() {
        synchronized (list) {
            while (list.size() + 1 > Constant.MAX_ATTACKER) {
                try {
                    list.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d("序号","-------generate attacker"+list.size());
            //随机生成敌人位置
            ImageView attacker = new ImageView(GameActivity.this);
            int[] point = new int[3];
            for (int i = 0; i < 3; ++i) {
                point[i] = new Random().nextInt(100) + 200;
            }
            attacker.setX(point[0]);
            attacker.setY(point[1]);
            attacker.setZ(point[2]);
            MoveThread move = new MoveThread(attacker);
            move.start();
            list.add(move);
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
            lifeValue += 20;
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