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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.fraunhofer.aisec.trustme.service.notification.NotificationListener;
import de.fraunhofer.aisec.trustme.service.TrustmeActionReceiver;

import com.kcoppock.broadcasttilesupport.BroadcastTileIntentBuilder;

/**
 * The TrustmeBroadcastReceiver starts the TrustmeService upon receiving
 * the BOOT_COMPLETED broadcast message (as defined in AndroidManifest.xml).
 */
public class TrustmeBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "TrustmeService";

    private static final String TRUSTME_SHUTDOWN_TILE_INTENT = "de.fraunhofer.aisec.trustme.service.intent.tile.shutdown";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "BroadcastReceiver is going to start TrustmeService");
        Intent startServiceIntent = new Intent(context, TrustmeService.class);
        startServiceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(startServiceIntent);

        Log.d(TAG, "BroadcastReceiver is going to start NotificationListener");
        Intent startNLIntent = new Intent(context, NotificationListener.class);
        startNLIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(startNLIntent);

        Log.d(TAG, "BroadcastReceiver is going to setup ShutdownTile");
        Intent shutdownTileConfigurationIntent = new BroadcastTileIntentBuilder(context, TRUSTME_SHUTDOWN_TILE_INTENT)
            .setVisible(true)
            .setLabel("Shutdown")
            .setIconResource(R.drawable.ic_lock_power_off_alpha)
            .setOnClickBroadcast(new Intent(TrustmeActionReceiver.TRUSTME_SHUTDOWN_INTENT))
            .build();

        context.sendBroadcast(shutdownTileConfigurationIntent);
    }
}
