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

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import android.app.PendingIntent;

import de.fraunhofer.aisec.trustme.service.R;

public abstract class TrustmeNotificationBuilder {
    protected static final int TRUSTME_STATUSBAR_ICON = R.drawable.ic_dialog_info;

    protected Context context;
    protected RemoteViews remoteView;
    protected Notification.Builder builder;

    public TrustmeNotificationBuilder(Context context, int layoutId) {
        this.context = context;
        remoteView = new RemoteViews(
                "de.fraunhofer.aisec.trustme.service",
                layoutId);
        builder = new Notification.Builder(context)
                .setSmallIcon(TRUSTME_STATUSBAR_ICON)
                .setContent(remoteView)
                .setVibrate(null); // Note that .setDefaults(DEFAULT_VIBRATE) overrides this setting.
    }

    public void setDogearColor(int dogearColor) {
        DogearView dogearView = new DogearView(context, dogearColor);
        Bitmap dogearBitmap = dogearView.toBitmap();
        remoteView.setImageViewBitmap(R.id.dogear, dogearBitmap);
    }

    public void setOngoing(boolean isOngoing) {
        builder.setOngoing(isOngoing);
    }

    public void setContentIntent(PendingIntent pi) {
        builder.setContentIntent(pi);
    }

    public void setSmallIcon(int icon) {
        builder.setSmallIcon(icon);
    }
}
