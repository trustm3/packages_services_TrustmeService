/*
 * This file is part of trust|me
 * Copyright(c) 2013 - 2017 Fraunhofer AISEC
 * Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU General Public License,
 * version 2 (GPL 2), as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GPL 2 license for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses/>
 *
 * The full GNU General Public License is included in this distribution in
 * the file called "COPYING".
 *
 * Contact Information:
 * Fraunhofer AISEC <trustme@aisec.fraunhofer.de>
 */

package de.fraunhofer.aisec.trustme.service.notification;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.View;

public class DogearView extends View {
    private int dogearColor;

    // Note: the actual dimensions of the dogear are set in trustme_notification_*.xml
    // and the triangle will be scaled accordingly.
    private float internalTriangleSize = 200;

    public DogearView(Context context) {
        this(context, Color.BLACK);
    }

    public DogearView(Context context, int dogearColor) {
        super(context);
        setDogearColor(dogearColor);
    }

    public void setDogearColor(int dogearColor) {
        this.dogearColor = dogearColor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#eeeeee")); // this simulates the back side of the notification
        //paint.setAlpha(0); // transparent background
        canvas.drawPaint(paint);
        paint.setColor(dogearColor);
        paint.setAntiAlias(true);
        Path path = new Path();
        path.moveTo(internalTriangleSize, internalTriangleSize);
        path.lineTo(internalTriangleSize, 0);
        path.lineTo(0, internalTriangleSize);
        path.close();

        canvas.drawPath(path, paint);
    }

    public Bitmap toBitmap() {
        Bitmap b = Bitmap.createBitmap(
                (int) internalTriangleSize,
                (int) internalTriangleSize,
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Drawable bgDrawable = this.getBackground();
        if (bgDrawable != null)
            bgDrawable.draw(c);
        else
            c.drawColor(Color.WHITE);
        this.draw(c);
        return b;
    }
}
