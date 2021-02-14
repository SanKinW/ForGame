package com.sankin.forgame.ContentThread;

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
                int dis = random.nextInt() % 3 - 1;
                int dir = random.nextInt() % 3;
                //做移动操作
                if (dir == 0) imageView.setX(imageView.getX() + dis);
                else if (dir == 1) imageView.setY(imageView.getY() + dis);
                else imageView.setZ(imageView.getZ() + dis);

            }
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
