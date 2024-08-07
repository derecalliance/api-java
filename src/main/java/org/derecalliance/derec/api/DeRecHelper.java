/*
 * Copyright (c) 2023 The Building Blocks Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.derecalliance.derec.api;

import java.util.List;
import java.util.function.Function;

/**
 * A DeRec Helper implementation may implement this API to provide access to and visibility of its operation. The
 * implementation may provide non-standardised means of automatically accepting pairing requests and is assumed to
 * have a means that is independent of this interface for deciding how and where to store information that is to be
 * protected by it (HelperShares).
 * <p>
 * Users of this API could be enterprise applications that instantiate the Helper implementation or could be
 * Mobile Phone Apps.
 * <p>
 * The API makes no assumptions about threading models or message passing models.
 */
public interface DeRecHelper {

	/**
	 * Representation of a sharer as perceived by a helper in respect of a particular share
	 */
	interface SharerStatus {
		DeRecIdentity getId();

		PairingResponseStatus getStatus();

		/**
		 * How the pairing request was responded to
		 */
		enum PairingResponseStatus {
			/*
			This is needed only if we as a standard wish to report refused and gone
			sharers to the application. As things stand this interface only reports
			active secrets.
			REPLIED_PAIRED, // replied positively
			REPLIED_REFUSED, // replied negatively*/
			ACTIVE,
			REPLIED_REMOVED, // at sharer request, potentially as a result of a DISCONNECT from helper
			PENDING_DISCONNECTION // disconnecting at Helper Request
		}
	}

	/**
	 * Representation of a "share" at a helper. The helper knows nothing about a share other than that it is some
	 * binary content stored by the library (which the app has no access to) which is identified by a sharer id and
	 * a secret id local to that sharer id. The app also has access to the version numbers kept by the library. This
	 * may be useful for diagnostic purposes.
	 * <p>
	 * An app may remove a share, which has the effect of getting the library to mark the share as inactive and at
	 * the next opportunity (on receipt of the next communication from the sharer) to request unpairing.
	 */
	interface Share {
		/**
		 * The sharer that this share belongs to
		 */
		SharerStatus getSharer();

		/**
		 * This share's secret id
		 */
		DeRecSecret.Id getSecretId();

		/**
		 * A list of versions currently held by the library
		 */
		List<Integer> getVersions();

		/**
		 * request removal of a share meaning make the share inactive and at the next opportunity, unpair from
		 * the sharer for this secret.
		 * The sharer's status becomes {@link SharerStatus.PairingResponseStatus#PENDING_DISCONNECTION} until
		 * the unpair request has been signalled to the sharer.
		 * @return true if request has been carried out successfully, false if it has already been requested
		 * or if the share is not known (possibly as a result of having previously been removed)
		 */
		boolean remove();
	}

	interface Notification {
		Type getType(); // the type of the notification
		DeRecIdentity getSharerId(); // the sharer id
		DeRecSecret.Id getSecretId(); // the secretId or null if none
		int getVersion(); // the version number or -1 if inapplicable


		/**
		 * The type of the notification - allows for introduction of custom helper notifications
		 * aside from {@link StandardHelperNotificationType}
		 */
		interface Type {
			String name();
		}

		enum StandardHelperNotificationType implements Type {
			PAIR_INDICATION, // someone is trying to pair for a particular secret
			UNPAIR_INDICATION, // someone is unpairing for a particular secret
			UPDATE_INDICATION, // an update has been received for a secret
			VERIFY_INDICATION, // a verification request has been received for a secret
			LIST_SECRETS_INDICATION, // a request to list secrets has been received
			RECOVER_SECRET_INDICATION // a request to recover a secret has been received
		}
	}


	interface NotificationResponse {
		/**
		 * Set to true if the helper wishes to refuse the request and discontinue the helper relationship. The
		 * semantics are as though {@link Share#remove()} had been called.
		 */
		boolean getUnpairPlease();

		/**
		 * result of notification handling
		 */
		public boolean getResult();

		/**
		 * some optional text (mainly useful in the case of unpair being requested)
		 */
		public String getReason();

		/**
		 * an optional object associated with the notification response
		 */
		public Object getReferenceObject();
	}

	/**
	 * Respond to a received notification
	 *
	 * @param result the result of notification handling
	 * @param reason optional text associated with notification handling
	 * @param referenceObj optional object associated with notification handling
	 * @return NotificationResponse object
	 */
	DeRecHelper.NotificationResponse newNotificationResponse(boolean result, String reason, Object referenceObj);

	/**
	 * Get a list of all protected items known to this helper
	 *
	 * @return a list (empty if no items a known)
	 */
	List<? extends DeRecHelper.Share> getShares();

	/**
	 * Get a list of all sharers that this helper is helping
	 *
	 * @return list of sharers
	 */
	List<? extends SharerStatus> getSharers();

	/**
	 * Remove a sharer (identified by SharerStatus) as seen by this helper
	 *
	 * @param sharerStatus sharer to remove
	 */
	void removeSharer(SharerStatus sharerStatus);

	/**
	 * Provide a "listener" for status and lifecycle event notifications relating to this helper's information,
	 * such as changes in the list of shares or requests to pair. The listener both provides information about
	 * an event and also allows the listener to respond positively, or negatively with a reason, which
	 * causes the pairing relationship to end.
	 * <p>
	 * Note: More than one listener may be provided by composition such as:
	 * <p>
	 */
	void setListener(Function<Notification, NotificationResponse> listener);
}
