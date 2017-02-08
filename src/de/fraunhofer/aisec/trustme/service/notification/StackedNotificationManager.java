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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import android.util.Log;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;


public class StackedNotificationManager {
    private static final String TAG = "TrustmeService";

    private String pkgName;
    private StatusBarNotification[] activeNotifications;
    private NotificationListenerService.RankingMap rankingMap;

    /*
     * Class for managing stacked notifications in a multi-container android
     * environment.
     * <P>
     * The gmail or email notification managment is not fully visible to the
     * {@link NotificationListener} and therefore notification stacking cannot
     * be recognized. This manager basically determines stacked gmail
     * notification as a work around solution.
     *
     * @param activeNotification Array containing the currently visible
     *      notifications.
     * @param rankingMap Map which contains the ranking order of all active
     *      notifcations.
     * @param pkgName packe name of app which produces stacked notifications
     *      e.g., com.google.android.gm for gmail or com.android.email for email
     * */
    public StackedNotificationManager(StatusBarNotification[] activeNotifications,
            NotificationListenerService.RankingMap rankingMap, String pkgName) {
        this.activeNotifications = activeNotifications;
        this.rankingMap = rankingMap;
        this.pkgName = pkgName;
    }

    /*
     * Checks if the given {@link StatusBarNotification} is active, i.e visible
     * in the UI.
     *
     * @param sbn Notification whose activity status is checked
     * */
    private boolean isActive(StatusBarNotification sbn) {
        if (this.activeNotifications == null){

            return true;
        }

        for (int i = 0; i < this.activeNotifications.length; i++) {
            if (sbn.getKey().equals(this.activeNotifications[i].getKey()))
                    return true;
        }

        return false;
    }

    /*
     * Determines wether a stacked notification should be broadcasted to other
     * containers.
     * <P>
     * The method returns true iff the notification is not active or
     * iff it has the highest rank among all notifications belonging to
     * the corresponding package name set during construction.
     * <P>
     * This should result in only broadcasting the
     * stacked mail notification iff there are more then one email notification.
     *
     * @param sbn Notification which is checked for being the stacked mail
     *      notification.
     * */
    public boolean shouldBroadcastNotification(StatusBarNotification sbn) {
        Log.d(TAG, "Should broadcast notification with key " + sbn.getKey());

        if (sbn == null || !sbn.getPackageName().equals(this.pkgName)) {
            return false;
        }
        if (!this.isActive(sbn) || this.rankingMap == null) {
            return true;
        }

        ArrayList<String> gmKeys = new ArrayList<String>();
        for (int i = 0; i < this.activeNotifications.length; i++) {
            if (this.activeNotifications[i].getPackageName().equals(this.pkgName)) {
                gmKeys.add(this.activeNotifications[i].getKey());
            }
        }

        LinkedList<String> orderedGmKeys = new LinkedList<String>();
        for (int i = 0; i < rankingMap.getOrderedKeys().length; i++) {
            for (String gmKey : gmKeys) {
                if (this.rankingMap.getOrderedKeys()[i].equals(gmKey)) {
                    orderedGmKeys.add(gmKey);
                }
            }
        }

        return orderedGmKeys.isEmpty() || orderedGmKeys.getFirst().equals(sbn.getKey());
    }
}
