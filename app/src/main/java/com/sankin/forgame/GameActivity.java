package com.sankin.forgame;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.sankin.forgame.Threads.MoveThread;
import com.sankin.forgame.Util.Constant;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity implements SensorEventListener {
    private ImageView user;
    private int lastX; //手指最后的位置

    private float[] angel = new float[3];  //陀螺仪数据
    private SensorManager sensorManager = null; //传感器管理
    private Sensor gyroSensor = null;   //陀螺仪
    private List<MoveThread> list = new LinkedList<>(); //敌人序列

    //插入背景乐
    private MediaPlayer mediaPlayer;

    private int lifeValue = 666; //初始生命值

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        user = findViewById(R.id.user_photo);
        user.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                user.setX(event.getX());
                user.setY(event.getY());
                user.invalidate();
                return true;
            }
        });

        //获取传感器信息
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mediaPlayer = MediaPlayer.create(this, R.raw.shoot);
        mediaPlayer.setLooping(true);
        mediaPlayer.getPlaybackParams().setSpeed(1f);
        mediaPlayer.start();

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

        new Thread(){
            @Override
            public void run(){
                while(true){
                    try{
                        isBeShoot();
                        Thread.sleep(100);
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
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        //为传感器注册监听器
        sensorManager.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        mediaPlayer.setLooping(true);
        mediaPlayer.getPlaybackParams().setSpeed(1f);
        mediaPlayer.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //回收资源
        mediaPlayer.pause();
    }

    //检查玩家是否被敌人的子弹命中
    public void isBeShoot() {
        synchronized (list) {
            for (MoveThread move : list) {
                ImageView image = move.getImageView();
                //检查角度，夹角小于10°认为瞄准，杀死该敌人
                if (image.getX() - user.getX() > -5.0 && image.getX() - user.getX() < 5.0) {
                    System.out.println("被击中了！---------------");
                    lifeValue = lifeValue - 3;
                }
                if (lifeValue <= 0) onStop();
            }
        }
    }

    //传感器变化
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            synchronized (list) {
                angel[0] = event.values[0]; //x
                angel[1] = event.values[1]; //y
                angel[2] = event.values[2]; //z
                checkDirection();
                try {
                    Thread.sleep(1000);
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
        for (MoveThread move : list) {
            ImageView image = move.getImageView();
            //检查角度，夹角小于5°认为瞄准，杀死该敌人
            if (CalculationAngle(image)) {
                System.out.println("杀死敌人！---------------");
                Vibrator vibrator = (Vibrator)this.getSystemService(this.VIBRATOR_SERVICE);
                vibrator.vibrate(100);
                move.setFlag(false);
                list.remove(move);
                list.notifyAll();
                break;
            }
        }
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
            System.out.println("敌人坐标" + point[0] + " " + point[1] + " " + point[2]);
            System.out.println("传感器坐标" + angel[0] + " " + angel[1] + " " + angel[2]);
            double dis1 = Math.sqrt(point[0] * point[0] + point[1] * point[1] + point[2] * point[2]);
            double dis2 = Math.sqrt(angel[0] * angel[0] + angel[1] * angel[1] + angel[2] * angel[2]);
            double multi = point[0] * angel[0] + point[1] * angel[1] + point[2] * angel[2];
            double includedAngle = Math.acos(multi / (dis1 * dis2));
            if (Double.compare(includedAngle, Constant.STANDARD) < 0 && Double.compare(includedAngle, -Constant.STANDARD) > 0) {
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
            System.out.println("0-30----------");
            mediaPlayer.getPlaybackParams().setSpeed(5f);
        }
        else if (Double.compare(angle, Constant.STANDARD_60) < 0) {
            System.out.println("30-60----------");
            mediaPlayer.getPlaybackParams().setSpeed(2.5f);
        }else {
            System.out.println("60-90----------");
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
            System.out.println("-------generate attacker"+list.size());
            //随机生成敌人位置
            ImageView attacker = new ImageView(GameActivity.this);
            attacker.setImageResource(R.drawable.ic_attacker);
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
}