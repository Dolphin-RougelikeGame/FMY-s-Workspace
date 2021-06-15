package com.example.dolphin.bussiness.parts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.example.dolphin.R;

public class UnbreakableWall extends Wall {
    private Paint UnbreakablePaint;

    private Bitmap bitmap;

    public UnbreakableWall(float x, float y, Context context) {
        super(x, y);
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.box).copy(Bitmap.Config.ARGB_8888, true);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) 80) / width;
        float scaleHeight = ((float) 80) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    public UnbreakableWall(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, getX(), getY(), new Paint());
    }

    @Override
    protected void onDraw(Canvas canvas) {
    }
}
