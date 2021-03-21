package com.sankin.forgame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import javax.security.auth.callback.Callback;

public class MySurfaceView extends SurfaceView implements Callback, Runnable {
    private SurfaceHolder sfh;
    private Paint paint;
    private Thread th;
    private boolean flag;
    private Canvas canvas;
    private int screenW, screenH;

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public void setW1(int w1) {
        this.w1 = w1;
    }

    public void setH1(int h1) {
        this.h1 = h1;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    public void setW2(int w2) {
        this.w2 = w2;
    }

    public void setH2(int h2) {
        this.h2 = h2;
    }

    //定义两个矩形的宽高坐标
    private int x1 = 0, y1 = 0, w1 = 0, h1 = 0;
    private int x2 = 0, y2 = 0, w2 = 0, h2 = 0;

    public boolean isCollsion() {
        return isCollsion;
    }

    //便于观察是否发生了碰撞设置一个标识位
    private boolean isCollsion;


    /**
     * SurfaceView初始化函数
     */
    public MySurfaceView(Context context) {
        super(context);
        sfh = this.getHolder();
        sfh.addCallback((SurfaceHolder.Callback) this);
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        setFocusable(true);
    }

    /**
     * SurfaceView视图创建，响应此函数
     */
    public void surfaceCreated(SurfaceHolder holder) {
        screenW = this.getWidth();
        screenH = this.getHeight();
        flag = true;
        //实例线程
        th = new Thread(this);
        //启动线程
        th.start();
    }

    /**
     * 游戏绘图
     */
    public void myDraw() {
        try {
            canvas = sfh.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.BLACK);
                if (isCollsion) {
                    paint.setColor(Color.RED);
                    paint.setTextSize(20);
                    canvas.drawText("Collision！", 0, 30, paint);
                } else {
                    paint.setColor(Color.WHITE);
                }
                //绘制两个矩形
                canvas.drawRect(x1, y1, x1 + w1, y1 + h1, paint);
                canvas.drawRect(x2, y2, x2 + w2, y2 + h2, paint);
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            if (canvas != null)
                sfh.unlockCanvasAndPost(canvas);
        }
    }

//    /**
//     * 触屏事件监听
//     */
//    public boolean onTouchEvent(MotionEvent event) {
//        //让矩形1随着触屏位置移动
//        x1 = (int) event.getX() - w1 / 2;
//        y1 = (int) event.getY() - h1 / 2;
//        if (isCollsionWithRect(x1, y1, w1, h1, x2, y2, w2, h2)) {
//            isCollsion = true;
//        } else {
//            isCollsion = false;
//        }
//        return true;
//    }

    /**
     * 按键事件监听
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 游戏逻辑
     */
    private void logic() {

    }

    public boolean isCollsionWithRect(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        if (x1 >= x2 && x1 >= x2 + w2) {
            return false;
        } else if (x1 <= x2 && x1 + w1 <= x2) {
            return false;
        } else if (y1 >= y2 && y1 >= y2 + h2) {
            return false;
        } else if (y1 <= y2 && y1 + h1 <= y2) {
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        while (flag) {
            long start = System.currentTimeMillis();
            myDraw();
            logic();
            long end = System.currentTimeMillis();
            try {
                if (end - start < 50) {
                    Thread.sleep(50 - (end - start));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * SurfaceView视图状态发生改变，响应此函数
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    /**
     * SurfaceView视图消亡时，响应此函数
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        flag = false;
    }

    public boolean onTouchEvent(ImageView mIvGun) {
        //让矩形1随着触屏位置移动
        x1 = (int) mIvGun.getX() - w1 / 2;
        y1 = (int) mIvGun.getY() - h1 / 2;
        if (isCollsionWithRect(x1, y1, w1, h1, x2, y2, w2, h2)) {
            isCollsion = true;
        } else {
            isCollsion = false;
        }
        return true;
    }
}