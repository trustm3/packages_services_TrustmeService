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

import java.util.Arrays;

import android.app.Notification;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcel;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;


public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "TrustmeService";
    private static final String TRUSTME_SERVICE_PACKAGE_NAME = "de.fraunhofer.aisec.trustme.service";

    public static final String GM_PKG_NAME = "com.google.android.gm";
    public static final String EMAIL_PKG_NAME = "com.android.email";
    public static final String K9_PKG_NAME = "com.fsck.k9";
    public static final String THREEMA_PKG_NAME = "ch.threema.app";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationListener started");
    }

    private void broadcastNotificationPosted(StatusBarNotification sbn,
            NotificationListenerService.RankingMap rankingMap) {

        if (!sbn.getPackageName().equals(TRUSTME_SERVICE_PACKAGE_NAME)) { // ignore own trustme notifications
            TrustmeNotificationManager tnm = new TrustmeNotificationManager(this); // pass context
            Intent trustmeServiceIntent = tnm.createIntentFromStatusBarNotification(sbn);
            if (trustmeServiceIntent != null)
                sendBroadcast(trustmeServiceIntent); // TrustmeActionReceiver will handle this intent
        }
    }

    private void broadcastNotificationRemoved(StatusBarNotification sbn,
            NotificationListenerService.RankingMap rankingMap) {

        if (!sbn.getPackageName().equals(TRUSTME_SERVICE_PACKAGE_NAME)) { // ignore own trustme notifications
            TrustmeNotificationManager tnm = new TrustmeNotificationManager(this); // pass context
            Intent trustmeServiceIntent = tnm.createIntentFromStatusBarNotification(sbn, true);
            sendBroadcast(trustmeServiceIntent); // TrustmeActionReceiver will handle this intent
        }
    }

    /*
     * This notifier is only called if
     * {@link #onNotificationPosted(StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap)}
     * is not implemented in {@link NotificationListener}.
     * <P>
     * Therefore it might be obsolete.
     *
     */
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "Notification posted by " + sbn.getPackageName());

        this.broadcastNotificationPosted(sbn, null);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn,
            NotificationListenerService.RankingMap rankingMap) {
        String pkgName = sbn.getPackageName();

        Log.d(TAG,"Notification posted with given rank by " + pkgName);

        if (pkgName.equals(TRUSTME_SERVICE_PACKAGE_NAME)) {
            Log.d(TAG,"Notification originating from different context, abort");
            return;
        }
        
        // TODO find general solution for this!!!
        Notification n = sbn.getNotification();
        if (n != null) {
            if (pkgName.equals(THREEMA_PKG_NAME) && n.extras.getCharSequence("android.template") != null) {
                // throw away threema's (redundant) BigTextStyle notifications and only use the regular ones instead
                Log.d(TAG, "Posted threema notification uses template BigTextStyle, abort");
                return;
            }

            if (Notification.CATEGORY_TRANSPORT.equals(n.category)) {
                // throw away transport notifications, i.e. audio player
                Log.d(TAG, "Posted notification has category transport, abort");
                return;
            }
            else if (Notification.CATEGORY_PROMO.equals(n.category)) {
                // throw away ad notifications
                Log.d(TAG, "Posted notification has category promo, abort");
                return;
            }
        }

        if (pkgName.equals(EMAIL_PKG_NAME)
                || pkgName.equals(GM_PKG_NAME)
                || pkgName.equals(K9_PKG_NAME)) {
            // necessary to handle invisble stacked notification management
            Log.d(TAG, "Stacked notification has been recognized as " + pkgName + " originated");

            StackedNotificationManager snManager = new StackedNotificationManager(
                this.getActiveNotifications(), rankingMap, pkgName);

            if (snManager.shouldBroadcastNotification(sbn)) {
                this.broadcastNotificationPosted(sbn, rankingMap);
                return;
            } else {
                Log.d(TAG, "Stacked notification has low rank, abort");
                return;
            }
        }


        this.broadcastNotificationPosted(sbn, rankingMap);
    }

    @Override
    public void onNotificationRankingUpdate(NotificationListenerService.RankingMap rankingMap) {
        Log.d(TAG, "Notification ranks have been updated");
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification removed by " + sbn.getPackageName());

        this.broadcastNotificationRemoved(sbn, null);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn,
            NotificationListenerService.RankingMap rankingMap) {
        String pkgName = sbn.getPackageName();

        Log.d(TAG, "Notification removed with given rank by " + pkgName);

        if (pkgName.equals(TRUSTME_SERVICE_PACKAGE_NAME)) {
            Log.d(TAG,"Notification originating from different context, abort");
            return;
        }

        this.broadcastNotificationRemoved(sbn, rankingMap);
    }
}
