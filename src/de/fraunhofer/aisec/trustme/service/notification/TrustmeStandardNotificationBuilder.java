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
import android.widget.RemoteViews;
import android.graphics.Color;
import java.lang.reflect.*;

import de.fraunhofer.aisec.trustme.service.R;

public class TrustmeStandardNotificationBuilder extends TrustmeNotificationBuilder {
    private int dogearColor;
    private String customIcon; // An explicitly set icon resource, superseding originalIcon.
    private Bitmap originalIcon; // The original icon of the forwarded notification.
    private String title;
    private String text;

    public TrustmeStandardNotificationBuilder(Context context, int dogearColor, String customIcon, Bitmap originalIcon, String title, String text, boolean isOngoing) {
        this(context, dogearColor, customIcon, originalIcon, title, text, isOngoing, null);
    }

    public TrustmeStandardNotificationBuilder(Context context, int dogearColor, String customIcon, Bitmap originalIcon, String title, String text, boolean isOngoing, PendingIntent pendingIntent) {
        super(context, R.layout.trustme_notification_standard);
        this.dogearColor = dogearColor;
        setDogearColor(dogearColor);
        setCustomIcon(customIcon);
        setOriginalIcon(originalIcon);
        setTitle(title);
        setText(text);
        setOngoing(isOngoing);
        if (pendingIntent != null)
            setContentIntent(pendingIntent);
    }

    public void setCustomIcon(String customIcon) {
        this.customIcon = customIcon;
    }

    public int getCustomIconId(String customIcon) {
        if (customIcon == null || customIcon.equals(""))
            return 0;

        Class res = android.R.drawable.class;
        Field field = null;
        try {
            field = res.getField(customIcon);
        } catch (NoSuchFieldException e) {
            return 0;
        }
        int drawableId = 0;
        try {
            drawableId = field.getInt(null);
        } catch (IllegalAccessException e) {
            return 0;
        }
        return drawableId;
    }

    public void setOriginalIcon(Bitmap originalIcon) {
        this.originalIcon = originalIcon;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Notification getNotification() {
        // First, generate a standard notification containing the supplied data.
        Notification.Builder standardNotificationBuilder = new Notification.Builder(context);
        int customIconId = getCustomIconId(customIcon);
        standardNotificationBuilder.setSmallIcon(customIconId != 0 ? customIconId : TRUSTME_STATUSBAR_ICON)
                .setContentTitle(title)
                .setContentText(text);

        // Set original icon only if no custom icon was supplied.
        // Note that Android makes sure that both the small and large
        // icons will be displayed in the extended notification view.
        if (customIconId == 0 && originalIcon != null)
            standardNotificationBuilder.setLargeIcon(originalIcon);

        Notification standardNotification = standardNotificationBuilder.build();

        // Now extract contentView from standard notification and put it into our custom trustme notification.
        RemoteViews extractedContentRemoteView = standardNotification.contentView;
        remoteView.addView(R.id.trustme_notification_standard, extractedContentRemoteView);

        // Finally, generate combined "trustme dogear notification".
        if (customIconId != 0)
            setSmallIcon(customIconId);
        return builder.build();
    }
}
