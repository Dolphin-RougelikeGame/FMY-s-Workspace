package com.example.dolphin.bussiness.parts;

import android.graphics.Canvas;

/**
 * Created by THINK on 2018/8/13.
 */

public class BreakableWall extends Wall {

    public BreakableWall(float x, float y) {
        super(x, y);
    }

    public BreakableWall(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {

    }
}
