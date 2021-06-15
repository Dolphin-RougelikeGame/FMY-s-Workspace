package com.example.dolphin.bussiness;

import android.graphics.Canvas;

import com.example.dolphin.bussiness.parts.Shape;

import java.util.Random;

public class RoomLimit implements Shape {
    private float RoomSizeParam;
    private float RoomShapeParam;
    private float RoomRejectParam;
    private int RoomSize;
    private int RoomShape;
    private int RoomReject;
    private Random random = new Random();

    public RoomLimit(float RoomSizeParam, float RoomShapeParam, float RoomRejectParam) {
        this.RoomSizeParam = RoomSizeParam;
        this.RoomShapeParam = RoomShapeParam;
        this.RoomRejectParam = RoomRejectParam;
    }

    public int RoomSizeFeature() {
        int random1 = random.nextInt(2);
        this.RoomSize = (int) (RoomSizeParam * random1) + 8;
        return 9;
        //return RoomSize;
    }

    public boolean RoomShapeFeature() {
        int random2 = random.nextInt(10);
        return random2 - 5 >= 0;
    }

    public int RoomRejectFeature() {
        int random3 = random.nextInt(4);
        this.RoomReject = (int) (RoomRejectParam * (float) random3) + 2;
        return RoomReject;
    }

    @Override
    public void draw(Canvas canvas) {
    }
}
