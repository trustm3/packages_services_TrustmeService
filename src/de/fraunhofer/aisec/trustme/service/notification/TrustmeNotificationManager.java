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

import java.lang.CharSequence;
import java.lang.Exception;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;

import android.graphics.Canvas;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.SystemProperties;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import de.fraunhofer.aisec.trustme.service.R;
import de.fraunhofer.aisec.trustme.CNotification.ContainerNotification;
import de.fraunhofer.aisec.trustme.service.TrustmeActionReceiver;

public class TrustmeNotificationManager implements ITrustmeNotificationManager {
    private static final String TAG = "TrustmeService";

    private static final String INTENT_EXTRA =
            "de.fraunhofer.aisec.trustme.service.extra.";

    private static final String POST_CODE = INTENT_EXTRA + "post_notification";
    private static final String CANCEL_CODE = INTENT_EXTRA + "cancel_notification";

    private static final String ID_KEY = INTENT_EXTRA + "notification_id";
    private static final String TAG_KEY = INTENT_EXTRA + "notification_tag";
    private static final String PKG_NAME_KEY = INTENT_EXTRA + "pkg_name";
    private static final String CODE_KEY = INTENT_EXTRA + "notification_code";
    private static final String TIMESTAMP = INTENT_EXTRA + "timestamp";

    private static final String NO_CLEAR = INTENT_EXTRA + "no_clear";

    private static final String CONTENT_TITLE_KEY = INTENT_EXTRA + "content_title";
    private static final String CONTENT_TEXT_KEY = INTENT_EXTRA + "content_text";

    private static final String ORIGINAL_ICON_KEY = INTENT_EXTRA + "original_icon";
    private static final String ORIGINAL_ICON_WIDTH = INTENT_EXTRA + "original_icon_width";
    private static final String ORIGINAL_ICON_HEIGHT = INTENT_EXTRA + "original_icon_height";

    private static final String CUSTOM_NOTIFICATION = INTENT_EXTRA + "custom_notification";
    private static final String CUSTOM_NOTIFICATION_WIDTH = INTENT_EXTRA + "custom_notification_width";
    private static final String CUSTOM_NOTIFICATION_HEIGHT = INTENT_EXTRA + "custom_notification_height";

    private static HashMap<String, Integer> uniqueRequestCode = new HashMap<String, Integer>();
    private static int uniqueRequestCodeCounter = 0;

    private static HashSet<String> progressNotifications = new HashSet<String>();

    private final boolean allowCustomNotifications;

    private Context context;

    public TrustmeNotificationManager() {
        this(null);
    }

    public TrustmeNotificationManager(Context context) {
        this.context = context;

        this.allowCustomNotifications = SystemProperties.getBoolean("to.trustme.customnotification", false);
    }

    @Override
    public Intent createIntentFromStatusBarNotification(StatusBarNotification sbn, boolean cancel) {
        Intent i = new Intent(TrustmeActionReceiver.TRUSTME_NOTIFICATION_INTENT);

        i.putExtra(CODE_KEY, cancel ? CANCEL_CODE : POST_CODE);
        i.putExtra(ID_KEY, sbn.getId());
        i.putExtra(TAG_KEY, sbn.getTag());
        i.putExtra(PKG_NAME_KEY, sbn.getPackageName());
        i.putExtra(TIMESTAMP, System.nanoTime());

        if (cancel) {
            String id = sbn.getPackageName() + "." + sbn.getTag() + "." + sbn.getId();
            if (progressNotifications.contains(id))
                progressNotifications.remove(id);
        }
        else {
        //if (!cancel) {
            Notification n = sbn.getNotification();

            if ((n.flags & Notification.FLAG_NO_CLEAR) == Notification.FLAG_NO_CLEAR ||
                (n.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT)
                    i.putExtra(NO_CLEAR, true);
            else
                    i.putExtra(NO_CLEAR, false);

            if (isCustomNotification(n)) { // custom notification
                if (!this.allowCustomNotifications) {
                    Log.d(TAG, "custom notification broadcasting not allowed " + sbn.getPackageName());
                    return null; // custom notification not allowed by system property
                }
                Log.d(TAG, "Creating intent containing custom trustme notification from StatusBarNotification");
                Bitmap nAsBitmap = createBitmapFromNotification(n);
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                nAsBitmap.compress(Bitmap.CompressFormat.PNG, 100, bs); // compress b/c bitmap may be too large for intent
                i.putExtra(CUSTOM_NOTIFICATION, bs.toByteArray());
                i.putExtra(CUSTOM_NOTIFICATION_WIDTH, nAsBitmap.getWidth());
                i.putExtra(CUSTOM_NOTIFICATION_HEIGHT, nAsBitmap.getHeight());
            }
            else { // standard notification
                if (isProgressNotification(sbn) && isProgressChangeOnly(sbn))
                    return null;

                Log.d(TAG, "Creating intent containing standard trustme notification from StatusBarNotification");
                i.putExtra(CONTENT_TITLE_KEY, n.extras.getCharSequence(Notification.EXTRA_TITLE));
                i.putExtra(CONTENT_TEXT_KEY, n.extras.getCharSequence(Notification.EXTRA_TEXT));

                Bitmap icon = extractIcon(sbn);
                i.putExtra(ORIGINAL_ICON_KEY, bitmapToByteArray(icon));
                i.putExtra(ORIGINAL_ICON_WIDTH, icon == null ? 0 : icon.getWidth());
                i.putExtra(ORIGINAL_ICON_HEIGHT, icon == null ? 0 : icon.getHeight());
            }
        }

        return i;
    }

    public Intent createIntentFromStatusBarNotification(StatusBarNotification sbn) {
        return createIntentFromStatusBarNotification(sbn, false);
    }


    @Override
    public ContainerNotification createMessageFromIntent(Intent i) {
        if (i == null)
            return null;

        ContainerNotification cn = new ContainerNotification();

        CharSequence notificationCode = i.getCharSequenceExtra(CODE_KEY);
        if (notificationCode == null) {
            Log.e(TAG, "Malformed trustme intent: missing mandatory notification code");
            return null;
        }
        if (notificationCode.toString().equals(POST_CODE))
            cn.code = ContainerNotification.POST_NOTIFICATION;
        else if (notificationCode.toString().equals(CANCEL_CODE))
            cn.code = ContainerNotification.CANCEL_NOTIFICATION;
        else {
            Log.e(TAG, "Malformed trustme intent: unknown notification code: " + notificationCode.toString());
            return null;
        }

        if (!i.hasExtra(ID_KEY)) {
            Log.e(TAG, "Malformed trustme intent: missing mandatory notification id");
            return null;
        }
        cn.id = i.getIntExtra(ID_KEY, 0);

        CharSequence tag = i.getCharSequenceExtra(TAG_KEY);
        cn.tag = tag != null ? tag.toString() : "";

        CharSequence pkgName = i.getCharSequenceExtra(PKG_NAME_KEY);
        if (pkgName == null) {
            Log.e(TAG, "Malformed trustme intent: missing mandatory package name");
            return null;
        }
        cn.pkgName = pkgName.toString();

        cn.timestamp = i.getLongExtra(TIMESTAMP, 0);
        cn.noClear = i.getBooleanExtra(NO_CLEAR, false);

        // Include the following fields only for posting notifications but not for canceling them:
        if (cn.code == ContainerNotification.POST_NOTIFICATION) {
            if (i.getByteArrayExtra(CUSTOM_NOTIFICATION) != null) { // custom notification
                Log.d(TAG, "Creating message from intent (containing custom trustme notification data)");
                Bitmap b = BitmapFactory.decodeByteArray(i.getByteArrayExtra(CUSTOM_NOTIFICATION),
                        0,
                        i.getByteArrayExtra(CUSTOM_NOTIFICATION).length);
                cn.customNotification = bitmapToByteArray(b);
                cn.customNotificationWidth = i.getIntExtra(CUSTOM_NOTIFICATION_WIDTH, 0);
                cn.customNotificationHeight = i.getIntExtra(CUSTOM_NOTIFICATION_HEIGHT, 0);
            }
            else { // standard notification
                Log.d(TAG, "Creating message from intent (containing standard trustme notification data)");
                cn.title = i.getCharSequenceExtra(CONTENT_TITLE_KEY) != null
                        ? i.getCharSequenceExtra(CONTENT_TITLE_KEY).toString()
                        : "";
                cn.text = i.getCharSequenceExtra(CONTENT_TEXT_KEY) != null
                        ? i.getCharSequenceExtra(CONTENT_TEXT_KEY).toString()
                        : "";

                /* Note: if the intent does not have the original icon extra the "The Sender's run loop threw an exception:null"
                exception is thrown and the protobuf message is not send. Therefore, the field originalIcon of the protobuf
                message must not be set if the original icon extra is null. */
                if (i.getByteArrayExtra(ORIGINAL_ICON_KEY) != null)
                    cn.originalIcon = i.getByteArrayExtra(ORIGINAL_ICON_KEY);
                cn.originalIconWidth = i.getIntExtra(ORIGINAL_ICON_WIDTH, 0);
                cn.originalIconHeight = i.getIntExtra(ORIGINAL_ICON_HEIGHT, 0);
            }
        }

        if (cn.code == ContainerNotification.POST_NOTIFICATION || cn.code == ContainerNotification.CANCEL_NOTIFICATION)
            return cn;
        else
            return null;
    }

    @Override
    public Notification createNotificationFromMessage(String sourceContainer, String sourceContainerColor, ContainerNotification cn) {
        Intent switchIntent = new Intent(TrustmeActionReceiver.TRUSTME_CONTAINER_SWITCH_INTENT);
        switchIntent.putExtra("targetContainer", sourceContainer);

        // Note: the requestCode of a PendingIntent must be different in order to distinguish two intents
        // that only vary in their "extra" contents. Otherwise, if we post two or more trustme notifications,
        // all of them (when tapped) will switch to the targetContainer of the notification posted last.
        // For more information, read the class overview of PendingIntent:
        // http://developer.android.com/reference/android/app/PendingIntent.html
        PendingIntent pendingSwitchIntent = PendingIntent.getBroadcast(context, getUniqueRequestCode(sourceContainer), switchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (cn.isBase) {
            Log.d(TAG, "Creating cmld notification from received message");
            return createBaseNotificationFromMessage(sourceContainerColor, cn, pendingSwitchIntent);
        }
        else if (cn.customNotificationWidth != 0) { // custom notification
            Log.d(TAG, "Creating custom notification from received message");
            return createCustomNotificationFromMessage(sourceContainerColor, cn, pendingSwitchIntent);
        }
        else { // standard notification
            Log.d(TAG, "Creating standard notification from received message");
            return createStandardNotificationFromMessage(sourceContainerColor, cn, pendingSwitchIntent);
        }
    }

    private Notification createBaseNotificationFromMessage(String sourceContainerColor, ContainerNotification cn, PendingIntent pendingIntent) {
        TrustmeStandardNotificationBuilder trustmeStandardNotificationBuilder = new TrustmeStandardNotificationBuilder(
                context,
                Color.parseColor(sourceContainerColor),
                cn.customIcon,
                null, // original icon
                cn.title,
                cn.text,
                cn.noClear,
                pendingIntent);

        return trustmeStandardNotificationBuilder.getNotification();
    }

    private Notification createStandardNotificationFromMessage(String sourceContainerColor, ContainerNotification cn, PendingIntent pendingIntent) {
        if (cn.originalIcon.length == 0 || cn.originalIconWidth == 0 || cn.originalIconHeight == 0)
            return null;

        Bitmap originalIcon = Bitmap.createBitmap(cn.originalIconWidth, cn.originalIconHeight, Bitmap.Config.ARGB_8888);
        originalIcon.copyPixelsFromBuffer(ByteBuffer.wrap(cn.originalIcon));
        originalIcon = invertBitmap(originalIcon);

        TrustmeStandardNotificationBuilder trustmeStandardNotificationBuilder = new TrustmeStandardNotificationBuilder(
                context,
                Color.parseColor(sourceContainerColor),
                null, // custom icon
                originalIcon,
                cn.title,
                cn.text,
                cn.noClear,
                pendingIntent);

        return trustmeStandardNotificationBuilder.getNotification();
    }

    private Notification createCustomNotificationFromMessage(String sourceContainerColor, ContainerNotification cn, PendingIntent pendingIntent) {
        if (cn.customNotification == null)
            return null;

        Bitmap originalNotification = Bitmap.createBitmap(cn.customNotificationWidth, cn.customNotificationHeight, Bitmap.Config.ARGB_8888);
        originalNotification.copyPixelsFromBuffer(ByteBuffer.wrap(cn.customNotification));

        TrustmeCustomNotificationBuilder trustmeCustomNotificationBuilder = new TrustmeCustomNotificationBuilder(
                context,
                Color.parseColor(sourceContainerColor),
                originalNotification,
                cn.noClear,
                pendingIntent);

        return trustmeCustomNotificationBuilder.getNotification();
    }

    private Bitmap extractIcon(StatusBarNotification sbn) {
        int iconId = sbn.getNotification().extras.getInt(Notification.EXTRA_SMALL_ICON);

        PackageManager pm = context.getPackageManager();
        try {
            Context c = context.createPackageContext(sbn.getPackageName(), 0);
            Resources r = c.getResources();
            Drawable d = r.getDrawable(iconId);
            return drawableToBitmap(d);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap(); // otherwise, use fall-back code below.
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0)
            return null;

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        if (bitmap == null)
            return null;

        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getAllocationByteCount());
            bitmap.copyPixelsToBuffer(byteBuffer);
            return byteBuffer.array();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isCustomNotification(Notification n) {
        String title = n.extras.getString(Notification.EXTRA_TITLE, "");
        String text = n.extras.getString(Notification.EXTRA_TEXT, "");

        // Note: For Android custom notifications android.template is set to
        // foo.bar.Notification$BigTextStyle, foo.bar.Notification$InboxStyle, etc.
        // For standard notifications it is null. However, custom notifications like
        // the ones used by mp3 players also have android.template set to null.
        // Therefore, we also check if title or text is empty; this indicates that
        // it is likely to be a custom notification.
        return n.extras.getCharSequence("android.template") != null
                || "".equals(title)
                || "".equals(text);
    }

    private boolean isProgressNotification(StatusBarNotification sbn) {
        String id = sbn.getPackageName() + "." + sbn.getTag() + "." + sbn.getId();
        if (progressNotifications.contains(id)) // necessary in order to recognize "finished notifications" (setProgress(0, 0, false))
            return true;

        Notification n = sbn.getNotification();
        if (n.extras.getInt("android.progressMax") != 0 || n.extras.getBoolean("android.progressIndeterminate"))
            return true;

        return false;
    }

    /**
     * Returns true if we should ignore this notification because it only updates
     * the progress of an already posted notification.
     * Returns false if this notification hasn't been seen before or the progress
     * is set to "finished", that is, setProgress(0, 0, false) was called.
     */
    private boolean isProgressChangeOnly(StatusBarNotification sbn) {
        String id = sbn.getPackageName() + "." + sbn.getTag() + "." + sbn.getId();
        if (!progressNotifications.contains(id)) { // first notification of this "progress notification series"?
            progressNotifications.add(id);
            return false; // indicate to caller to display this notification
        }

        Notification n = sbn.getNotification();
        if (n.extras.getInt("android.progressMax") == 0 &&
            n.extras.getInt("android.progress") == 0 &&
            n.extras.getBoolean("android.progressIndeterminate") == false) {
                progressNotifications.remove(id); // progress is finished
                return false; // indicate to caller to display this notification
        }

        return true; // progress change; indicate to caller to ignore this notification
    }

    private Bitmap createBitmapFromNotification(Notification n) {
        if (n == null)
            return null;

        RemoteViews extractedContentRemoteView = n.contentView;
        View view = extractedContentRemoteView.apply(context, null);
        view.measure(0, 0); // required
        view.layout(0, 0, 1080 - 50, view.getMeasuredHeight()); // TODO how can we dynamically determine the 3rd parameter?
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        return view.getDrawingCache(true);
    }

    private Bitmap invertBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap bitmapInv = Bitmap.createBitmap(width, height, bitmap.getConfig());
        int a, r, g, b;

        // We invert each pixel individually.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = bitmap.getPixel(x, y);
                a = Color.alpha(p);
                r = Color.red(p);
                g = Color.green(p);
                b = Color.blue(p);
                bitmapInv.setPixel(x, y, Color.argb(a, 255 - r, 255 - g, 255 - b));
            }
        }
        return bitmapInv;
    }

    private int getUniqueRequestCode(String container) {
        if (!uniqueRequestCode.containsKey(container))
            uniqueRequestCode.put(container, ++uniqueRequestCodeCounter);
        return uniqueRequestCode.get(container);
    }
}
