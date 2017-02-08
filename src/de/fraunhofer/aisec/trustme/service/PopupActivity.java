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

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.content.DialogInterface;

// This class may be invoked directly by sending an explicit intent from:
// - external/trustmelib/trustme.util/src/de/fraunhofer/aisec/trustme/util/Prefs.java
// - frameworks/base/packages/SystemUI/src/com/android/systemui/qs/QSTile.java
public class PopupActivity extends Activity{

    private AlertDialog switchDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.d("PopupActivity","onCreate");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You need to switch to the manager in order to change this setting.")
            .setTitle("Info");
        builder.setPositiveButton("Switch", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.d("PopupActivity","Switch");
                Intent switchIntent = new Intent();
                switchIntent.setAction("de.fraunhofer.aisec.trustme.service.intent.action.switch");
                dialog.dismiss();
                finish();
                PopupActivity.this.sendBroadcast(switchIntent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
               Log.d("PopupActivity","Cancel");
               dialog.dismiss();
               finish();
            }
        });
        switchDialog = builder.create();
        switchDialog.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (switchDialog != null) {
            switchDialog.dismiss();
        }
    }
}
