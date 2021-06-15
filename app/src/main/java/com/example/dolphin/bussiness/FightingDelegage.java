package com.example.dolphin.bussiness;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;

import com.example.dolphin.bussiness.parts.EnemyTank;
import com.example.dolphin.bussiness.parts.Explode;
import com.example.dolphin.bussiness.parts.Missile;
import com.example.dolphin.bussiness.parts.OursTank;
import com.example.dolphin.bussiness.parts.UnbreakableWall;
import com.example.dolphin.bussiness.parts.Wall;
import com.example.dolphin.view.FireButton;
import com.example.dolphin.view.SteeringWheelView;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FightingDelegage implements ITankHitHandler, IMissileHitHandler,
        SteeringWheelView.IDirectionListener, FireButton.IFireListener{
    private static final String TAG = "FightingDelegage";

    Context context;

    /**
     * canvas宽度和高度
     */
    private int mWidth;
    private int mHeight;

    /**
     * Room类随机生成房间的
     */
    private RoomLimit roomLimit;
    /**
     * 缓冲
     */
    private Canvas mBufferCanvas;
    private Bitmap mBufferBitmap;

    Paint clearPaint = new Paint();

    /**
     * 刷新互斥锁, 刷新界面时缓冲区不能刷新，刷新缓冲区时界面不能刷新
     */
    private Mutex mMutex = new Mutex();
    private boolean mIsRunning;

    /**
     * 敌方子弹
     */
    private List<Missile> enemyMissiles;
    private List<Missile> oursMissiles;
    private List<EnemyTank> enemyTanks;
    private List<Explode> explodes;
    private List<Wall> walls;
    private List<UnbreakableWall> UnbreakableWalls;
    private OursTank oursTank;
    /**
     * 控制消息
     */
    private Queue<Message> mq = new ArrayDeque<>(10);

    /**
     * 同样的方向只放一次
     */
    private Message currentMessage;
    private Object driveLock = new Object();
    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    public FightingDelegage(int width, int height, Context context) {
        this.context = context;
        this.mWidth = width;
        this.mHeight = height;
        mBufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mBufferCanvas = new Canvas(mBufferBitmap);

        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        this.enemyMissiles = new LinkedList<>();
        this.oursMissiles = new LinkedList<>();
        this.enemyTanks = new LinkedList<>();
        this.walls = new LinkedList<>();
        this.UnbreakableWalls = new LinkedList<>();
        this.explodes = new LinkedList<>();

        test();

        mIsRunning = true;
        executorService.submit(new BufferDrawer());
        executorService.submit(new MissileDriver());
        executorService.submit(new TankDriver());
    }

    /**
     * 结束时停止线程池
     */
    public void gameOver() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            Log.i("zhoukai", "gameOver: executorService end");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示到界面, 将缓冲区中画好的图形绘制到surfaceview的canvas中
     * @param canvas
     */
    public void draw(Canvas canvas, Context context) {
        if (null != canvas) {
            if (null != mBufferBitmap) {
                try {
                    mMutex.acquire(Thread.currentThread());

                    clearCanvas(canvas);
                    canvas.drawBitmap(mBufferBitmap, 0, 0, null);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mMutex.release(Thread.currentThread());
                }
            }
        }
    }

    private void clearCanvas(Canvas canvas) {
        canvas.drawPaint(clearPaint);
    }

    public void setmIsRunning(boolean mIsRunning) {
        this.mIsRunning = mIsRunning;
        gameOver();
    }

    /**
     * 绘制缓冲区的线程
     */

    class BufferDrawer implements Runnable {
        @Override
        public void run() {
            while (mIsRunning) {

                try {
                    mMutex.acquire(Thread.currentThread());

                    //绘制前先清除
                    clearCanvas(mBufferCanvas);

                    //绘制开始
                    for (int i = 0; i < UnbreakableWalls.size(); i++) {
                        UnbreakableWalls.get(i).draw(mBufferCanvas);
                    }

                    //敌方坦克
                    for (int i = 0; i < enemyTanks.size(); i++) {
                        enemyTanks.get(i).draw(mBufferCanvas);
                    }

                    //我方坦克
                    oursTank.draw(mBufferCanvas);

                    //我方子弹
                    for (int i = 0; i < oursMissiles.size(); i++) {
                        oursMissiles.get(i).draw(mBufferCanvas);
                    }

                    //子弹
                    for (int i = 0; i < enemyMissiles.size(); i++) {
                        enemyMissiles.get(i).draw(mBufferCanvas);
                    }

                    //爆炸效果
                    for (int i = 0; i < explodes.size(); i++) {
                        explodes.get(i).draw(mBufferCanvas);
                    }
                    explodes.clear();
                    //绘制结束

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    mMutex.release(Thread.currentThread());
                }

                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Log.i("zhoukai", "BufferDrawer: end");
        }
    }

    /**
     * 控制敌方坦克移动, 子弹移动, 碰撞检测
     */
    class MissileDriver implements Runnable {
        @Override
        public void run() {
            while (mIsRunning) {
                try {
                    mMutex.acquire(Thread.currentThread());

                    //控制敌方坦克移动, 子弹移动,碰撞检测
                    for (int i = 0; i < enemyTanks.size(); i++) {
                        enemyTanks.get(i).fireAtWill();
                        enemyTanks.get(i).moveAtWill(enemyTanks, oursTank, walls);
                    }

                    for (int i = 0; i < enemyMissiles.size(); i++) {
                        enemyMissiles.get(i).move();
                    }
                    for (int i = 0; i < oursMissiles.size(); i++) {
                        oursMissiles.get(i).move();
                    }

                    //已检查是否出界, 还需要加入是否打到我方坦克和打到墙
                    for (int i = 0; i < enemyMissiles.size(); i++) {
                        if (!enemyMissiles.get(i).isOut()) {
                            enemyMissiles.get(i).hitTank(oursTank);
                        }
                    }

                    for (int i = 0; i < oursMissiles.size(); i++) {
                        if (!oursMissiles.get(i).isOut()) {
                            oursMissiles.get(i).hitTanks(enemyTanks);
                        }
                    }
                    //控制敌方坦克移动, 子弹移动,碰撞检测


                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mMutex.release(Thread.currentThread());
                }


                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private float translateX;
    private float translateY;

    /**
     * 控制我方坦克移动和开火
     */
    class TankDriver implements Runnable {
        private boolean isDriving;
        private Direction currentDirection;

        @Override
        public void run() {
            while (mIsRunning) {
                Message message = mq.poll();
                if (null != message || isDriving) {
                    if (null != message) {
                        switch (message) {
                            case UP:
                                isDriving = true;
                                currentDirection = Direction.UP;
                                break;
                            case DOWN:
                                isDriving = true;
                                currentDirection = Direction.DOWN;
                                break;
                            case LEFT:
                                isDriving = true;
                                currentDirection = Direction.LEFT;
                                break;
                            case RIGHT:
                                isDriving = true;
                                currentDirection = Direction.RIGHT;
                                break;
                            case STOP:
                                isDriving = false;
                                currentDirection = null;
                                break;
                            default:
                                break;
                        }
                    }

                    if (null != currentDirection && isDriving) {
                        oursTank.moveWithDirection(currentDirection, enemyTanks, walls);
                    }
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } else {
                    try {
                        synchronized (driveLock) {
                            driveLock.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    /**
     * 互斥锁
     *
     */
    static class Mutex {
        private Thread owner;

        public Mutex() {
        }

        /**
         * 进入锁
         * @param thread
         * @throws InterruptedException
         */
        public synchronized void acquire(Thread thread) throws InterruptedException {
            if (owner != null) {
                wait();
            }

            owner = thread;
        }

        /**
         * 释放锁
         * @param thread
         * @throws InterruptedException
         */
        public synchronized void release(Thread thread) {
            if (owner == thread) {
                owner = null;
                notify();
            }
        }

    }

    enum  Message {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        STOP;
    }

    @Override
    public void onEnemyTankDestroy(EnemyTank destroy) {
        enemyTanks.remove(destroy);
    }

    @Override
    public void onOursTankDestroy(OursTank oursTank) {

    }

    @Override
    public void onEnemyFire(Missile missile) {
        missile.setMissileHitHandler(this);
        enemyMissiles.add(missile);
    }

    @Override
    public void onOursFire(Missile missile) {
        missile.setMissileHitHandler(this);
        oursMissiles.add(missile);
    }

    @Override
    public void onHit(Missile missile) {
        enemyMissiles.remove(missile);
        oursMissiles.remove(missile);
        explodes.add(new Explode(missile.getX(), missile.getY()));
    }

    @Override
    public void onUp() {
        Log.w(TAG, "onUp: ");
        if (currentMessage == Message.UP) {
            return;
        }

        mq.offer(Message.UP);
        synchronized (driveLock) {
            driveLock.notifyAll();
        }
    }

    @Override
    public void onDown() {
        Log.i(TAG, "onDown: ");
        if (currentMessage == Message.DOWN) {
            return;
        }

        mq.offer(Message.DOWN);
        synchronized (driveLock) {
            driveLock.notifyAll();
        }
    }

    @Override
    public void onLeft() {
        Log.i(TAG, "onLeft: ");
        if (currentMessage == Message.LEFT) {
            return;
        }

        mq.offer(Message.LEFT);
        synchronized (driveLock) {
            driveLock.notifyAll();
        }
    }

    @Override
    public void onRight() {
        Log.i(TAG, "onRight: ");
        if (currentMessage == Message.RIGHT) {
            return;
        }

        mq.offer(Message.RIGHT);
        synchronized (driveLock) {
            driveLock.notifyAll();
        }
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop: ");
        mq.offer(Message.STOP);

        currentMessage = null;
        synchronized (driveLock) {
            driveLock.notifyAll();
        }
    }

    @Override
    public void onFire() {
        Log.i(TAG, "onFire: ");
        oursTank.fire();
    }

// 定义地图数组
    private static int[][] MapArray = new int[108][60];
    private static int[] RoomArray = new int[16];
    private static int roomNum = 6;
    private static int[] RoomSite = new int[6];
    private Random random = new Random();

    public void makeRoom(int roomX_init, int roomY_init, int roomX_len, int roomY_len) {
        int limit_x = roomX_len + roomX_init;
        int limit_y = roomY_len + roomY_init;
        for (int i = roomX_init; i < limit_x; i++) {
            for (int j = roomY_init; j < limit_y; j++) {
                MapArray[i][j] = 1;
            }
        }
    }

    public void makeWay_x(int roomX_init, int roomY_init, int roomX_len, int roomY_len, int way_len) {
        int center_x = roomX_len + roomX_init;
        int center_y = roomY_len / 2 + roomY_init;
        for (int i = 0; i < way_len; i++) {
            MapArray[center_x + i][center_y] = 1;
        }
    }

    public void makeWay_y(int roomX_init, int roomY_init, int roomX_len, int roomY_len, int way_len) {
        int center_x = roomX_len / 2 + roomX_init;
        int center_y = roomY_len + roomY_init;
        for (int i = 0; i < way_len; i++) {
            MapArray[center_x][center_y + i] = 1;
        }
    }

    public void test() {
        // 初始化一个房间列表
        for (int i = 0; i < 16; i++) {
            RoomArray[i] = 0;
        }

        int temp;
        int last = -1;
        while (roomNum != 0) {
            temp = random.nextInt(16);
            if (last == -1 || (RoomArray[temp] == 0 && (temp % 4 == last % 4 || temp / 4 == last / 4))) {
                RoomArray[temp] = 1;
                last = temp;
                roomNum--;
            }
        }

        // 初始化地图数组
        for (int i = 0; i < 108; i++)
            for (int j = 0; j < 60; j++)
                MapArray[i][j] = 0;

        // 房间宽高、格子宽高
        int room_width = 17, room_height = 9;
        int grid_width = 27, grid_height = 15;
        // 房间到边界的距离
        int border_x = (grid_width - room_width) / 2;
        int border_y = (grid_height - room_height) / 2;;

        // 画各个房间
        int column = 0, row = 0;
        int site_x = 0, site_y = 0;
        int room_No = 0;
        for (int i = 0; i < 16; i++) {
            if (RoomArray[i] == 1) {
                RoomSite[room_No] = i;
                room_No++;
                column = i % 4;
                row = i / 4;
                site_x = column * grid_width + border_x;
                site_y = row * grid_height + border_y;
                // 调用makeRoom制作地图
                makeRoom(site_x, site_y, room_width, room_height);
            }
        }

        // 画地图之间的连通
        int way_len = 0;
        for (int i = 0; i < 5; i++) {
            if (RoomSite[i] / 4 == RoomSite[i + 1] / 4) {
                way_len = border_x * 2 + grid_width * (RoomSite[i + 1] - RoomSite[i] - 1);
                column = RoomSite[i] % 4;
                row = RoomSite[i] / 4;
                site_x = column * grid_width + border_x;
                site_y = row * grid_height + border_y;
                makeWay_x(site_x, site_y, room_width, room_height, way_len);
            }
        }

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (RoomSite[i] % 4 == RoomSite[j + 1] % 4) {
                    way_len = border_y * 2 + grid_height * (RoomSite[j + 1] / 4 - RoomSite[i] / 4 - 1);
                    column = RoomSite[i] % 4;
                    row = RoomSite[i] / 4;
                    site_x = column * grid_width + border_x;
                    site_y = row * grid_height + border_y;
                    makeWay_y(site_x, site_y, room_width, room_height, way_len);
                }
            }
        }

        // 将存在MapArray中的地图画出来
        int floorSize = 80;
        for (int i = 0; i < 108; i++)
            for (int j = 0; j < 60; j++)
                if (MapArray[i][j] != 0)
                    UnbreakableWalls.add(new UnbreakableWall(floorSize * i, floorSize * j, context));

        translateX = 0;
        translateY = 0;
    }
}
