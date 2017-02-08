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

//package de.fraunhofer.aisec.trustme.service;
//
//import java.io.ByteArrayOutputStream;
//import java.lang.InterruptedException;
//import java.util.Arrays;
//
//import android.util.Log;
//import android.app.WallpaperManager;
//import android.graphics.Bitmap;
//import android.graphics.Bitmap.Config;
//import android.graphics.Canvas;
//import android.graphics.drawable.Drawable;
//import android.graphics.drawable.BitmapDrawable;
//
//import de.fraunhofer.aisec.trustme.cmlcom.Sender;
//import de.fraunhofer.aisec.trustme.service.CService.ServiceToCmldMessage;
//
///**
// * The WallpaperHandler thread is used by the ServiceReceiver thread to trigger
// * the sending of the currently set wallpaper to cmld. The WallpaperHandler
// * does not repeat the sending of a wallpaper if it has already been sent
// * (unless the sending was forced).
// */
//class WallpaperHandler implements Runnable {
//	private static final String TAG = "TrustmeService";
//	private static final int WALLPAPER_WIDTH_SCALED = 400;
//	private final WallpaperManager wallpaperManager;
//	private final Sender sender;
//	private byte[] lastWallpaperByteArray = null;
//	private boolean forceSend = false;
//	private final Object lock = new Object();
//
//	public WallpaperHandler(WallpaperManager wallpaperManager, Sender sender) {
//		super();
//		this.wallpaperManager = wallpaperManager;
//		this.sender = sender;
//	}
//
//	private Bitmap drawableToBitmap(Drawable drawable) {
//		Bitmap bitmap;
//		if (drawable instanceof BitmapDrawable) {
//			bitmap = ((BitmapDrawable)drawable).getBitmap();
//			if (bitmap != null)
//				return bitmap;
//		}
//
//		bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
//		Canvas canvas = new Canvas(bitmap);
//		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
//		drawable.draw(canvas);
//
//		return bitmap;
//	}
//
//	// Note: height will be calculated automatically in proportion to `width'.
//	private Bitmap scaleBitmap(Bitmap bitmap, int width) {
//		int height = bitmap.getHeight() / (bitmap.getWidth() / WALLPAPER_WIDTH_SCALED);
//		return Bitmap.createScaledBitmap(bitmap, WALLPAPER_WIDTH_SCALED, height, false);
//	}
//
//	private byte[] bitmapToByteArray(Bitmap bitmap) {
//		ByteArrayOutputStream stream = new ByteArrayOutputStream();
//		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//		return stream.toByteArray();
//	}
//
//	private boolean isLiveWallpaper() {
//		return wallpaperManager.getWallpaperInfo() != null;
//	}
//
//	private synchronized void sendWallpaperInternal() {
//		try {
//			// We don't proceed in case of live wallpapers.
//			if (isLiveWallpaper()) {
//				Log.d(TAG, "Live wallpaper set; not sending anything");
//				/* Note that it would be nice to get something like
//				 * ``wallpaperDrawable = wallpaperManager.getWallpaperInfo().loadIcon(packageManager);''
//				 * to work so that we could send a static image of the live wallpaper.
//				 * Unfortunately, the above did not work. */
//				return;
//			}
//
//			// Get current (static) wallpaper.
//			Drawable wallpaperDrawable = wallpaperManager.getDrawable();
//
//			// Convert wallpaper to bitmap.
//			Bitmap wallpaperBitmap = drawableToBitmap(wallpaperDrawable);
//
//			// Scale bitmap proportionally (i.e., keep aspect ratio).
//			Bitmap wallpaperBitmapScaled = scaleBitmap(wallpaperBitmap, WALLPAPER_WIDTH_SCALED);
//
//			// Convert scaled bitmap to byte array so it can be sent via protobuf.
//			byte[] wallpaperByteArray = bitmapToByteArray(wallpaperBitmapScaled);
//
//			// Check if we already sent the currently set wallpaper to cmld.
//			if (!forceSend && lastWallpaperByteArray != null && Arrays.equals(wallpaperByteArray, lastWallpaperByteArray)) {
//				Log.d(TAG, "Active wallpaper already sent to cmld; not sending again");
//				return;
//			}
//
//			// Store scaled wallpaper so we can do the above check for already sent wallpapers.
//			lastWallpaperByteArray = Arrays.copyOf(wallpaperByteArray, wallpaperByteArray.length);
//
//			// Send scaled wallpaper via protobuf to cmld.
//			Log.d(TAG, "Sending active wallpaper to cmld");
//			ServiceToCmldMessage message = new ServiceToCmldMessage();
//			message.code = ServiceToCmldMessage.WALLPAPER;
//			message.wallpaperData = wallpaperByteArray;
//			sender.sendMessage(message);
//		}
//		catch (Exception e) {
//			Log.d(TAG, "Exception in WallpaperHandler: " + e.getMessage());
//		}
//	}
//
//	public synchronized void sendWallpaper() {
//		Log.d(TAG, "Sending of wallpaper has been triggered");
//		forceSend = false;
//		synchronized(lock) {
//			lock.notify();
//		}
//	}
//
//	public synchronized void sendWallpaper(boolean forceSend) {
//		Log.d(TAG, "Sending of wallpaper has been triggered (force send)");
//		forceSend = true;
//		synchronized(lock) {
//			lock.notify();
//		}
//	}
//
//	/*
//	 * WallpaperHandler Main
//	 */
//	public void run() {
//		for (;;) {
//			try {
//				synchronized(lock) {
//					lock.wait(); // Wait until sendWallpaper has been invoked.
//				}
//			}
//			catch (InterruptedException e) {
//				continue;
//			}
//			sendWallpaperInternal();
//		}
//	}
//}
