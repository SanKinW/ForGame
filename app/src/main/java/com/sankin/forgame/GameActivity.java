package com.sankin.forgame;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ImageView;

import com.sankin.forgame.ContentThread.MoveThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity implements SensorEventListener {
    private float[] angel = new float[3];  //陀螺仪数据
    private SensorManager sensorManager = null; //传感器管理
    private Sensor gyroSensor = null;   //陀螺仪
    private List<MoveThread> list = new ArrayList<>(); //敌人序列
    private final static int MAX_ATTACKER = 10; //最大敌人数量
    private final static double STANDARD = Math.acos(Math.PI/18.0);


    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        //获取传感器信息
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

    }

    @Override
    protected void onPause() {
        super.onPause();
        //解除传感器监听
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //为传感器注册监听器
        sensorManager.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        //产生敌人
        new Thread(){
            @Override
            public void run(){
                while(true){
                    try{
                        Thread.sleep(20000);
                        generateAttacker();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    //传感器变化
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            synchronized (list) {
                angel[0] = event.values[0]; //x
                angel[1] = event.values[1]; //y
                angel[2] = event.values[2]; //z
                checkDirection();
            }
        }
    }

    /**
     * 检查夹角
     */
    public void checkDirection() {
        for (MoveThread move : list) {
            ImageView image = move.getImageView();
            //检查角度，夹角小于10°认为瞄准，杀死该敌人
            if (CalculationAngle(image)) {
                move.setFlag(false);
                list.remove(move);
            }
        }
    }

    /*
        计算夹角
     */

    public boolean CalculationAngle(ImageView imageView) {
        synchronized (imageView) {
            float[] point = new float[3];
            point[0] = imageView.getX();
            point[1] = imageView.getY();
            point[2] = imageView.getZ();
            double dis1 = Math.sqrt(point[0] * point[0] + point[1] * point[1] + point[2] * point[2]);
            double dis2 = Math.sqrt(angel[0] * angel[0] + angel[1] * angel[1] + angel[2] * angel[2]);
            double multi = point[0] * angel[0] + point[1] * angel[1] + point[2] * angel[2];
            double includedAngle = Math.acos(multi / (dis1 * dis2));
            if (Double.compare(includedAngle, STANDARD) < 0) return true;
            else {
                //在一定范围内音效变化




                return false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //产生敌人、此时双方攻击均无效
    public void generateAttacker() {
        synchronized (list) {
            while (list.size() + 1 > MAX_ATTACKER) {
                try {
                    list.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //随机生成敌人位置
            ImageView attacker = new ImageView(this);
            int[] point = new int[3];
            for (int i = 0; i < 3; ++i) {
                point[i] = new Random().nextInt();
                point[i] %= 6;
            }
            attacker.setX(point[0]);
            attacker.setY(point[1]);
            attacker.setZ(point[2]);
            MoveThread move = new MoveThread(attacker);
            move.start();
            list.add(move);
            list.notifyAll();
        }
    }
}