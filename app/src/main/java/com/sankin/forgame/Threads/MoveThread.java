package com.sankin.forgame.Threads;

import android.widget.ImageView;

import java.util.Random;

public class MoveThread extends Thread{
    private ImageView imageView;
    private boolean flag;

    public MoveThread(ImageView imageView) {
        this.imageView = imageView;
        flag = true;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    @Override
    public void run() {
        while (flag) {
            synchronized (imageView) {
                //随机移动
                Random random = new Random();
                float[] point = new float[3];
                point[0] = random.nextInt(100) + 200;
                point[1] = random.nextInt(100) + 200;
                point[2] = random.nextInt(100) + 200;
                //做移动操作
                imageView.setX(point[0]);
                imageView.setY(point[1]);
                imageView.setZ(point[2]);
            }
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
