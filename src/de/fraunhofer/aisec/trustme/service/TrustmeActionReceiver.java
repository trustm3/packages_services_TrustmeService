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

package de.fraunhofer.aisec.trustme.service;

import java.lang.CharSequence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.os.Bundle;
import android.os.Parcel;
import java.util.Properties;

import de.fraunhofer.aisec.trustme.service.notification.TrustmeNotificationManager;

import de.fraunhofer.aisec.trustme.cmlcom.Sender;
import de.fraunhofer.aisec.trustme.service.CService.ServiceToCmldMessage;
import de.fraunhofer.aisec.trustme.Container;
import de.fraunhofer.aisec.trustme.CNotification.ContainerNotification;

/**
 * Handles trustme-specific intents.
 * Android intents will be handled by ActionReceiver.
 *
 * - TRUSTME_*_INTENT intents may be sent by broadcasters. These broadcasters must have the
 *   'broadcastPermission'-permission as defined below in order to prevent arbitrary
 *   apps from sending critical intents (e.g., switching container intents).
 */
public class TrustmeActionReceiver extends BroadcastReceiver {
    private static final String TAG = "TrustmeService";
    private final Sender sender;

    // The permission required by broadcasters to send the following intents to us.
    public static final String broadcastPermission = "de.fraunhofer.aisec.trustme.service.permission.SEND_INTENTS";

    // The following intent may be sent by:
    // - PopupActivity.java
    // - frameworks/base/packages/SystemUI/src/com/android/systemui/SearchPanelView.java
    //
    // Furthermore, the following class makes sure that trustme notifications will broadcast the intent when clicked:
    // - notification/TrustmeNotificationManager.java
    public static final String TRUSTME_CONTAINER_SWITCH_INTENT = "de.fraunhofer.aisec.trustme.service.intent.action.switch";

    // The following intent may be sent by:
    // - notification/NotificationListener.java
    public static final String TRUSTME_NOTIFICATION_INTENT = "de.fraunhofer.aisec.trustme.service.intent.action.notification";

    // The following intent may be sent by:
    // - device/fraunhofer/trustme_hammerhead_aX/overlay/frameworks/base/packages/SystemUI/res/values/config.xml
    public static final String TRUSTME_SHUTDOWN_INTENT = "de.fraunhofer.aisec.trustme.service.intent.action.shutdown";

    // Sent by TrustmeActionReceiver when the device/container is to be shut down.
    private static final String ACTION_REQUEST_SHUTDOWN = "android.intent.action.ACTION_REQUEST_SHUTDOWN";

    public TrustmeActionReceiver(Sender sender) {
        super();
        this.sender = sender;
    }

    public static IntentFilter createFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TRUSTME_NOTIFICATION_INTENT);
        filter.addAction(TRUSTME_CONTAINER_SWITCH_INTENT);
        filter.addAction(TRUSTME_SHUTDOWN_INTENT);

        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

        String intentAction = intent.getAction();
        if (intentAction == null)
            return;

        ServiceToCmldMessage message = new ServiceToCmldMessage();
        message.code = 0;

        //
        // TRUSTME_NOTIFICATION_INTENT
        //
        if (intentAction.equals(TRUSTME_NOTIFICATION_INTENT)) {
            Log.d(TAG, "TrustmeActionReceiver is going to notify cmld about a new notification");
            message.code = ServiceToCmldMessage.NOTIFICATION;

            TrustmeNotificationManager tnm = new TrustmeNotificationManager();
            message.notification = tnm.createMessageFromIntent(intent);
        }
        //
        // TRUSTME_CONTAINER_SWITCH_INTENT
        //
        else if (intentAction.equals(TRUSTME_CONTAINER_SWITCH_INTENT)) {
            String targetContainer = intent.getStringExtra("targetContainer");
            if (targetContainer == null)
                targetContainer = "00000000-0000-0000-0000-000000000000"; // a0
            Log.d(TAG, "TrustmeActionReceiver is going to send SWITCH_CONTAINER request to cmld with targetContainer: " + targetContainer);
            message.code = ServiceToCmldMessage.SWITCH_CONTAINER;
            message.targetContainer = targetContainer;
        }
        //
        // TRUSTME_SHUTDOWN_INTENT
        //
        else if (intentAction.equals(TRUSTME_SHUTDOWN_INTENT)) {
            Log.d(TAG, "TrustmeActionReceiver is going to show shutdown dialog.");
            Intent shutdownIntent = new Intent(ACTION_REQUEST_SHUTDOWN);
            shutdownIntent.putExtra("android.intent.extra.KEY_CONFIRM", true);
            shutdownIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(shutdownIntent);
            return;
        }
        else {
            // We are not interested in any other intent actions.
            return;
        }

        sender.sendMessage(message);
    }
}

