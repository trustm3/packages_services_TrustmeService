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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.IntentService;
//import android.app.WallpaperManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import de.fraunhofer.aisec.trustme.service.notification.NotificationListener;

import de.fraunhofer.aisec.trustme.cmlcom.Sender;
import com.google.protobuf.nano.MessageNano;
import de.fraunhofer.aisec.trustme.service.CService.ServiceToCmldMessage;

/**
 * The TrustmeService gets started by TrustmeBroadcastReceiver. It sets up
 * the Unix Domain Socket communication and starts the Sender and the Receiver
 * threads which make use of this socket.
 */
public class TrustmeService extends IntentService {
    private static final String TAG = "TrustmeService";
    private static final String SOCK_ADDR = "/dev/socket/cml-service";
    private LocalSocket socket;
    private InputStream socketInputStream;
    private OutputStream socketOutputStream;
    private Sender sender;
    private ActionReceiver actionReceiver = null;
    private TrustmeActionReceiver trustmeActionReceiver = null;
    private NotificationListener notificationListener = null;
    private final int SOCKET_SEND_BUFFER_SIZE = 1024*1024;

    public TrustmeService() {
        super(TAG);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);

        // Make sure the ActivityManager will restart the TrustmeService in case it crashes.
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent _) {
        try {
            Log.d(TAG, "TrustmeService is starting");
            socket = new LocalSocket(LocalSocket.SOCKET_STREAM);
            Log.d(TAG, "Trying to connect to socket " + SOCK_ADDR);
            socket.connect(new LocalSocketAddress(SOCK_ADDR, LocalSocketAddress.Namespace.FILESYSTEM));
            Log.d(TAG, "Successfully connected to socket");

            // Set up input and output streams.
            socketInputStream = socket.getInputStream();
            socketOutputStream = socket.getOutputStream();

            // Increase socket send buffer size in order to send moderately large
            // messages (i.e. wallpaper) without blocking for several seconds.
            socket.setSendBufferSize(SOCKET_SEND_BUFFER_SIZE); // note: cmld has set the max socket send buffer size to 1 MB

            // Start sender thread.
            sender = new Sender(socketOutputStream) {
                @Override
                protected void exceptionHandler(Exception e) {
                    Log.e(TAG, "The Sender's run loop threw an exception: " + e.getMessage());
                    // We don't exit here and let the Sender proceed.
                }
            };
            new Thread(sender).start();

            // Let sender know about previously set socket send buffer size.
            sender.setSocketSendBufferSize(SOCKET_SEND_BUFFER_SIZE);

            // Send various information to cmld.
            sendBootCompleted();
            sendImeiMacPhoneno();
            sendAirplaneMode();
            sendWifiState();

            // Get handle for various system services.
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            //WallpaperManager wallpaperManager = (WallpaperManager) getSystemService(Context.WALLPAPER_SERVICE);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Start wallpaper handler thread.
            /*
            WallpaperHandler wallpaperHandler = new WallpaperHandler(wallpaperManager, sender);
            new Thread(wallpaperHandler).start();
            */

            // Start receiver thread.
            new Thread(new ServiceReceiver(
                    this, // we're a subclass of the Context class
                    socketInputStream,
                    powerManager,
                    notificationManager,
                    //wallpaperHandler,
                    sender)).start();

            // Register a receiver that will watch for relevant Android intents like
            // MASTER_CLEAR_NOTIFICATION, ACTION_SHUTDOWN, and phone state changes.
            // The receiver may notify cmld about received actions.
            if (actionReceiver == null) {
                actionReceiver = new ActionReceiver(sender);
                IntentFilter filter = ActionReceiver.createFilter();
                Intent result = registerReceiver(actionReceiver, filter);
                Log.d(TAG, "Registering ActionReceiver " + actionReceiver +
                           " through filter " + filter +
                           " with result " + result);
            } else {
                Log.w(TAG, "Trying to register ActionReceiver more than once");
            }

            // Register another receiver that will watch for trustme-specific intents
            // (like container switch requests) and other critical intents. The receiver
            // may notify cmld about received actions.
            if (trustmeActionReceiver == null) {
                trustmeActionReceiver = new TrustmeActionReceiver(sender);
                IntentFilter filter = TrustmeActionReceiver.createFilter();
                Intent result = registerReceiver(trustmeActionReceiver, filter, TrustmeActionReceiver.broadcastPermission, null);
                Log.d(TAG, "Registering TrustmeActionReceiver " + trustmeActionReceiver +
                           " through filter " + filter +
                           " with broadcast permission " + TrustmeActionReceiver.broadcastPermission +
                           " with result " + result);
            } else {
                Log.w(TAG, "Trying to register TrustmeActionReceiver more than once");
            }

            // Make sure our NotificationListener has access to the broadcasted notifications.
            enableNotificationListener();

            // Don't return here as otherwise we may be more likely to get killed by the lowmemkiller.
            Object waitIndef = new Object();
            synchronized(waitIndef) {
                for (;;) {
                    try {
                        waitIndef.wait(); // wait indefinitely
                    }
                    catch (InterruptedException e) {
                        // empty
                    }
                }
            }
        }
        catch (IOException e) {
            Log.d(TAG, "Exception while initializing TrustmeService: " + e.getMessage());
            return;
        }
    }

    private void sendBootCompleted() {
        ServiceToCmldMessage message_boot_complete = new ServiceToCmldMessage();
        message_boot_complete.code = ServiceToCmldMessage.BOOT_COMPLETED;
        sender.sendMessage(message_boot_complete);
    }

    private void sendImeiMacPhoneno() {
        ServiceToCmldMessage message = new ServiceToCmldMessage();
        message.code = ServiceToCmldMessage.IMEI_MAC_PHONENO;
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = null;
        String macAddress = null;
        String imei = null;
        String phoneNumber = null;
        if (wifiManager != null) {
            try {
                wifiInfo = wifiManager.getConnectionInfo();
            } catch (Exception e) {
                Log.w(TAG, "Something went wrong when trying to get wifiInfo");
            }
        }
        if (wifiInfo != null) {
            macAddress = wifiInfo.getMacAddress();
        }
        if (telephonyManager != null) {
            imei = telephonyManager.getDeviceId();
            phoneNumber = telephonyManager.getLine1Number();
        }
        if (imei != null) {
            Log.d(TAG, "IMEI: " + imei);
            message.imei = imei;
        }
        if (phoneNumber != null) {
            Log.d(TAG, "Phone No.: " + phoneNumber);
            message.phonenumber = phoneNumber;
        }
        if (macAddress != null) {
            Log.d(TAG, "MAC adress " + macAddress);
            message.mac = macAddress;
        }
        sender.sendMessage(message);
    }

    // Send Airplane mode state (needed if rebooted while in airplane mode)
    private void sendAirplaneMode() {
        ServiceToCmldMessage messageAirplaneMode = new ServiceToCmldMessage();
        messageAirplaneMode.code = ServiceToCmldMessage.AIRPLANE_MODE_CHANGED;
        messageAirplaneMode.airplaneMode = Settings.Global.getInt(
                getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        Log.d(TAG, "Sending initial Airplane mode state: " + messageAirplaneMode.airplaneMode);
        sender.sendMessage(messageAirplaneMode);
    }

    private void sendWifiState() {
        ServiceToCmldMessage messageWifi = new ServiceToCmldMessage();
        messageWifi.code = ServiceToCmldMessage.WIFI_USER_ENABLED_CHANGED;
        messageWifi.wifiUserEnabled = Settings.Global.getInt(
                getContentResolver(),
                Settings.Global.WIFI_ON, 0) == 1;
        Log.d(TAG, "Sending initial Wifi state: " + messageWifi.wifiUserEnabled);
        sender.sendMessage(messageWifi);
    }

    /**
     * Enable access to notifications. Note that normally the user must interactively
     * allow an app to have access to broadcasted notifications (via Settings / Security /
     * Notification access). However, as a system app with proper permissions, we are able
     * to do this programmatically.
     */
    private void enableNotificationListener() {
        String nl = new ComponentName(this, NotificationListener.class).flattenToString();
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");

        if (!TextUtils.isEmpty(enabledListeners)) {
            if (enabledListeners.contains(nl)) {
                return; // we're already enabled
            }
            else {
                enabledListeners += ":" + nl; // add NotificationListener to existing list
            }
        }
        else {
            enabledListeners = nl; // add NotificationListener to empty list
        }

        Settings.Secure.putString(getContentResolver(), "enabled_notification_listeners", enabledListeners);
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        unregisterReceiver(actionReceiver);
        unregisterReceiver(trustmeActionReceiver);
    }
}
