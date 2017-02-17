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
import java.io.StringWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.HashMap;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.os.IPowerManager;
import android.os.Parcel;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import de.fraunhofer.aisec.trustme.service.notification.TrustmeNotificationManager;

import de.fraunhofer.aisec.trustme.cmlcom.Receiver;
import de.fraunhofer.aisec.trustme.cmlcom.Sender;

import com.google.protobuf.nano.MessageNano;
import de.fraunhofer.aisec.trustme.service.CService.CmldToServiceMessage;
//import de.fraunhofer.aisec.trustme.service.CService.CmldToServiceMessage.ContainerNotificationMessage;

import de.fraunhofer.aisec.trustme.service.CService.ServiceToCmldMessage;
import de.fraunhofer.aisec.trustme.Container;
import de.fraunhofer.aisec.trustme.CNotification.ContainerNotification;

import de.fraunhofer.aisec.trustme.service.R;

/**
 * This class handles protobuf messages received from cmld.
 */
public class ServiceReceiver extends Receiver {
    private static final String TAG = "TrustmeService.ServiceReceiver";
    private final Context context;
    private final PowerManager powerManager;
    private final NotificationManager notificationManager;
    private final Sender sender;
    //private final WallpaperHandler wallpaperHandler;
    private final WifiManager wifiManager;
    private HashMap<String, Long> notificationTimestamps;

    public ServiceReceiver(
        final Context context,
        InputStream socketInputStream,
        final PowerManager powerManager,
        final NotificationManager notificationManager,
        //final WallpaperHandler wallpaperHandler,
        Sender sender) {
            super(socketInputStream);
            this.context = context;
            this.powerManager = powerManager;
            this.notificationManager = notificationManager;
            this.sender = sender;
            //this.wallpaperHandler = wallpaperHandler;
            this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
            this.notificationTimestamps = new HashMap<String, Long>();
    }

    @Override
    protected void handleMessage(byte[] encodedMessage) throws IOException {
        CService.CmldToServiceMessage message = CService.CmldToServiceMessage.parseFrom(encodedMessage);
        // Logging message.toString() leads to blocking behaviour in case of
        // large byte fields
        Log.d(TAG, "Handling received message");

        switch (message.code) {
            case CmldToServiceMessage.SUSPEND:
                /*
                Log.d(TAG, "Triggering the sending of current wallpaper to cmld (if necessary)");
                wallpaperHandler.sendWallpaper();
                */
                Log.d(TAG, "Calling pm.goToSleep()");
                powerManager.goToSleep(SystemClock.uptimeMillis(), PowerManager.GO_TO_SLEEP_REASON_POWER_BUTTON, PowerManager.GO_TO_SLEEP_FLAG_NO_DOZE);
                break;

            case CmldToServiceMessage.RESUME:
                Log.d(TAG, "Calling pm.wakeUp()");
                powerManager.wakeUp(SystemClock.uptimeMillis());
                // Note: we currently do not send a RESUME_COMPLETED
                break;

            case CmldToServiceMessage.SHUTDOWN:
                Log.d(TAG, "Calling powerManager.shutdown()");
                IPowerManager powerManager = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE));
                try {
                    powerManager.shutdown(false, "Container Manager requested Shutdown", false);
                }
                catch (RemoteException e) {
                    Log.d(TAG, "Container shutdown failed: " + e.getMessage());
                }
                break;

            /*
            case CmldToServiceMessage.WALLPAPER:
                Log.d(TAG, "Triggering the forced sending of current wallpaper to cmld");
                wallpaperHandler.sendWallpaper(true);
                break;
            */

            case CmldToServiceMessage.NOTIFICATION:
                Log.d(TAG, "Received notification from cmld");

                ContainerNotification cn = message.notification;
                if (cn == null) {
                    Log.d(TAG, "Notification data is empty (null); ignoring");
                    break;
                }
                if (cn.code != ContainerNotification.CANCEL_NOTIFICATION
                        && cn.customNotificationHeight == 0
                        && (cn.originalIcon.length == 0 || cn.originalIconWidth == 0 || cn.originalIconHeight == 0)) {
                    if (cn.isBase) {
                        Log.d(TAG, "Incoming notification has cmld notification");
                    }
                    else {
                        Log.d(TAG, "Notification's original icon length or width or height is 0, and notification has no base; ignoring");
                        break;
                    }
                }

                String globalId = message.sourceId + "." + cn.pkgName + "." + cn.tag;
                long lastTimestamp = notificationTimestamps.containsKey(globalId) ? notificationTimestamps.get(globalId) : 0;
                if (cn.timestamp <= lastTimestamp) {
                    Log.d(TAG, "Incoming notification ignored since its timestamp"
                            + " (" + cn.timestamp + ") "
                            + "is older than timestamp"
                            + " (" + lastTimestamp + ") "
                            + "of last notify/cancel operation done for this globalId."
                            + "Ignored notification: "
                            + "{tag:" + globalId
                            + ", id:" + cn.id
                            + ", timestamp: " + cn.timestamp
                            + "}");
                    break;
                }
                else { // update list of most recent timestamps to timestamp of current notification
                    notificationTimestamps.put(globalId, cn.timestamp);
                }

                if (cn.code == ContainerNotification.POST_NOTIFICATION) {
                    if (cn.customNotificationHeight == 0) {
                        Log.d(TAG, "Posting standard trustme notification "
                                + "{tag:" + globalId
                                + ", sourceId:" + message.sourceId
                                + ", id:" + cn.id
                                + ", timestamp: " + cn.timestamp
                                + ", title:" + cn.title
                                + ", text:" + cn.text
                                + ", icon size:" + cn.originalIcon.length
                                + ", icon width:" + cn.originalIconWidth
                                + ", icon height:" + cn.originalIconHeight
                                + "}");
                    }
                    else {
                        Log.d(TAG, "Posting custom trustme notification "
                                + "{tag:" + globalId
                                + ", sourceId:" + message.sourceId
                                + ", id:" + cn.id
                                + ", timestamp: " + cn.timestamp
                                + ", custom notification width:" + cn.customNotificationWidth
                                + ", custom notification height:" + cn.customNotificationHeight
                                + "}");
                    }
                    TrustmeNotificationManager tnm = new TrustmeNotificationManager(context);
                    Notification tn = tnm.createNotificationFromMessage(message.sourceId, message.sourceColor, cn);

                    // We first post a dummy notification with the same tag (globalId) and id (cn.id) as
                    // the real trustme notification (tn) in order to "reset" the trustme notification
                    // to a "proper state". Without this trick, Android does not seem to be able to handle
                    // updated trustme notifications correctly (likely because we use manually altered
                    // RemoteViews) and wrongly overlays the updated notifications. Note that this dummy
                    // notification is replaced so quickly by the real trustme notification that it is
                    // not really visible to the user.
                    Notification dummyNotification = new Notification.Builder(context)
                            .setContentTitle(".")
                            .setContentText(".")
                            .setSmallIcon(R.drawable.ic_dialog_info)
                            .build();
                    notificationManager.notify(globalId, cn.id, dummyNotification);

                    // Finally, post the actual trustme notification (replacing the previously posted dummy notification).
                    notificationManager.notify(globalId, cn.id, tn);
                }
                else if (cn.code == ContainerNotification.CANCEL_NOTIFICATION) {
                    Log.d(TAG, "Canceling trustme notification "
                            + "{tag:" + globalId
                            + ", id:" + cn.id
                            + ", timestamp: " + cn.timestamp
                            + "}");
                    notificationManager.cancel(globalId, cn.id);
                }
                break;

            case CmldToServiceMessage.AIRPLANE_MODE_CHANGED:
                Log.d(TAG, "Trying to set Airplane mode to " + message.airplaneMode);
                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, message.airplaneMode ? 1 : 0);
                intent.putExtra("state", message.airplaneMode);
                context.sendBroadcastAsUser(intent, UserHandle.ALL);
                break;

            case CmldToServiceMessage.WIFI_USER_ENABLED_CHANGED:
                Log.d(TAG, "Trying to set wifi mode to " + message.wifiUserEnabled);
                wifiManager.setWifiEnabled(message.wifiUserEnabled);
                break;

            default:
                Log.d(TAG, "Don't know how to handle message: " + message);
        }
    }

    @Override
    protected void exceptionHandler(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        Log.e(TAG, "The Receiver's run loop threw an exception: " + exceptionAsString);
        // We don't exit here and let the Receiver proceed.
    }
}
