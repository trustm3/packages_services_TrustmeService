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
import android.telephony.TelephonyManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Parcel;
import java.util.Properties;

import de.fraunhofer.aisec.trustme.service.notification.TrustmeNotificationManager;

import de.fraunhofer.aisec.trustme.cmlcom.Sender;
import de.fraunhofer.aisec.trustme.service.CService.ServiceToCmldMessage;
import de.fraunhofer.aisec.trustme.Container;
import de.fraunhofer.aisec.trustme.CNotification.ContainerNotification;

/**
 * Handles various Android intents.
 * Trustme-specific intents will be handled by TrustmeActionReceiver.
 */
public class ActionReceiver extends BroadcastReceiver {
    private static final String TAG = "TrustmeService";
    private final Sender sender;

    // Received when the device is to be wiped ("factory reset").
    private static final String MASTER_CLEAR_NOTIFICATION = "android.intent.action.MASTER_CLEAR_NOTIFICATION";

    public ActionReceiver(Sender sender) {
        super();
        this.sender = sender;
    }

    public static IntentFilter createFilter() {
        IntentFilter filter = new IntentFilter(MASTER_CLEAR_NOTIFICATION);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

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

        if (intentAction.equals(MASTER_CLEAR_NOTIFICATION)) {
            Log.d(TAG, "ActionReceiver is going to notify cmld about MASTER_CLEAR_NOTIFICATION");
            message.code = ServiceToCmldMessage.MASTER_CLEAR;
        }
        else if (intentAction.equals(Intent.ACTION_SHUTDOWN)) {
            Log.d(TAG, "ActionReceiver is going to notify cmld about ACTION_SHUTDOWN");
            message.code = ServiceToCmldMessage.SHUTDOWN;
        }
        else if (intentAction.equals(Intent.ACTION_SCREEN_ON)) {
            Log.d(TAG, "ActionReceiver is going to notify cmld about ACTION_SCREEN_ON");
            message.code = ServiceToCmldMessage.RESUME_COMPLETED;
        }
        else if (intentAction.equals(Intent.ACTION_SCREEN_OFF)) {
            Log.d(TAG, "ActionReceiver is going to notify cmld about ACTION_SCREEN_OFF");
            message.code = ServiceToCmldMessage.SUSPEND_COMPLETED;
        }
        else if (intentAction.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            Log.d(TAG, "ActionReceiver is going to notify cmld about ACTION_PHONE_STATE_CHANGED");
            TelephonyManager telephonyManager;
            telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int callState = telephonyManager.getCallState();
            switch (callState) {
                case TelephonyManager.CALL_STATE_IDLE:
                    message.code = ServiceToCmldMessage.CALL_HANGUP;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    message.code = ServiceToCmldMessage.CALL_ACTIVE;
                    break;
                // TODO do we need to check for other states (and possibly send them to cmld)?
                // TODO default case?
            }
        }
        else if (intentAction.equals(ConnectivityManager.CONNECTIVITY_ACTION)) { // only captured in a0; see createFilter()
            // Send the network connectivity state to cmld.
            message.code = ServiceToCmldMessage.CONNECTIVITY_CHANGE;
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo active = cm.getActiveNetworkInfo();

            message.connectivity = Container.OFFLINE;

            if (active != null && active.isConnected() ){
                if (active.getType() == ConnectivityManager.TYPE_MOBILE)
                    message.connectivity = Container.MOBILE_ONLY;
                if (active.getType() == ConnectivityManager.TYPE_WIFI)
                    message.connectivity = Container.WIFI_ONLY;
            }

            Log.d(TAG, "ActionReceiver is going to notify cmld about network connectivity: " + message.connectivity);
        }
        else if (intentAction.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            switch (intent.getIntExtra("wifi_state", (WifiManager.WIFI_STATE_UNKNOWN))) {
                case WifiManager.WIFI_STATE_ENABLED:
                    message.wifiUserEnabled = true;
                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                    message.wifiUserEnabled = false;
                    break;
                default:
                    return; // do not send anything
            }
            message.code = ServiceToCmldMessage.WIFI_USER_ENABLED_CHANGED;
            Log.d(TAG, "ActionReceiver is going to notify cmld about WIFI_USER_ENABLED__CHANGED: " + message.wifiUserEnabled);
        }
        else if (intentAction.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            message.airplaneMode = intent.getBooleanExtra("state", false);
            message.code = ServiceToCmldMessage.AIRPLANE_MODE_CHANGED;
            Log.d(TAG, "ActionReceiver is going to notify cmld about AIRPLANE_MODE_CHANGED: " + message.airplaneMode);
        }
        else {
            return; // we are not interested in any other intent actions.
        }

        sender.sendMessage(message);
    }
}

