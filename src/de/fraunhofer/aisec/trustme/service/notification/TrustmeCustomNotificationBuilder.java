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
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;

import de.fraunhofer.aisec.trustme.service.R;

public class TrustmeCustomNotificationBuilder extends TrustmeNotificationBuilder {
    private Notification originalNotification;

    public TrustmeCustomNotificationBuilder(Context context, int dogearColor, Bitmap originalNotification, boolean isOngoing) {
        this(context, dogearColor, originalNotification, isOngoing, null);
    }

    public TrustmeCustomNotificationBuilder(Context context, int dogearColor, Bitmap originalNotification, boolean isOngoing, PendingIntent pendingIntent) {
        super(context, R.layout.trustme_notification_custom);
        setDogearColor(dogearColor);
        remoteView.setImageViewBitmap(R.id.originalNotification, originalNotification);
        setOngoing(isOngoing);
        if (pendingIntent != null)
            setContentIntent(pendingIntent);
    }

    public Notification getNotification() {
        return builder.build();
    }
}
