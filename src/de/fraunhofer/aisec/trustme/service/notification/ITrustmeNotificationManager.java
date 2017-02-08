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

import android.app.Notification;
import android.content.Intent;
import android.service.notification.StatusBarNotification;

import de.fraunhofer.aisec.trustme.CNotification.ContainerNotification;

public interface ITrustmeNotificationManager {
    /**
     * Creates an Intent based on a standard Android StatusBarNotification.
     *
     * This method may be used in the source container by the NotificationListener to broadcast the
     * generated intent such that the ActionReceiver will receive and forward it.
     */
    public Intent createIntentFromStatusBarNotification(StatusBarNotification sbn, boolean cancel);

    /**
     * Creates a message (e.g., a protobuf message) generated from the extra fields of our "wrapped"
     * intent.
     *
     * This method may be used in the source container by the ActionReceiver to create a protobuf
     * message which can be forwarded to cmld.
     */
    public ContainerNotification createMessageFromIntent(Intent i);

    /**
     * Creates a trustme notification based on a ContainerNotification (automatically generated protobuf
     * class).
     *
     * This method may be used in the target container by the ServiceReceiver to generate a
     * trustme notification based on the received protobuf data, which can then be posted
     * in the target container.
     */
    public Notification createNotificationFromMessage(String sourceContainer, String sourceContainerColor, ContainerNotification cn);
}
