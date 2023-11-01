/*
 * Copyright (c) 2023 Swirlds Labs.
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

import java.util.Optional;

import static org.derecalliance.derec.api.DeRecStatusNotification.NotificationSeverity.*;

/**
 * A status notification sent to a helper app, to notify of changes in the list of secrets or sharers or their status.
 *
 * NOTE: this file is a draft, and may change substantially.
 */
public interface DeRecHelperMessageNotification {
	// extension point for enum below
	interface NotificationType {
		DeRecStatusNotification.NotificationSeverity getDefaultSeverity();
		String name();
	}

	/**
	 * The type of the notification
	 */
	DeRecStatusNotification.NotificationType getType();

	/**
	 * A message describing the nature of the notification
	 */
	String getMessage();

	/**
	 * The version, if any, that the update refers to
	 */
	Optional<DeRecVersion> getVersion();

	/**
	 * The helper, if any, that the update refers to
	 */
	Optional<DeRecHelperStatus> getHelper();

	/**
	 * The secret this update refers to
	 */
	DeRecSecret getSecret();

	/**
	 * The severity of the notification
	 */
	DeRecStatusNotification.NotificationSeverity getSeverity();

	enum StandardNotificationType implements DeRecStatusNotification.NotificationType {
		UPDATE_PROGRESS(UNCLASSIFIED),  // a vehicle for saying things about an update
		UPDATE_AVAILABLE(NORMAL), // a sufficient number of acknowledgements have been received for an update to consider it recoverable
		UPDATE_FAILED(ERROR), // update can't reach quorum
		UPDATE_COMPLETE(NORMAL), // all update requests have been replied to, or failed
		VERIFY_PROGRESS(UNCLASSIFIED), // a vehicle for saying things about a verification
		VERIFY_AVAILABLE(NORMAL), // a sufficient number of acknowledgements have been received for verify
		VERIFY_FAILED(ERROR), // verification can't reach quorum
		VERIFY_COMPLETE(NORMAL), // all update requests have been replied to, or failed
		RECOVERY_PROGRESS(UNCLASSIFIED),
		RECOVERY_AVAILABLE(NORMAL), // a sufficient number of responses have been received to reconstruct secret
		RECOVERY_FAILED(ERROR), // the secret cannot be recovered at the present time
		RECOVERY_COMPLETE(NORMAL), // all  requests have been replied to, or failed
		LIST_SECRET_PROGRESS(UNCLASSIFIED),
		LIST_SECRET_AVAILABLE(NORMAL), // a response has been received from a helper to list secrets
		LIST_SECRET_FAILED(ERROR), // helper won't list the secrets
		HELPER_PAIRED(NORMAL), // helper accepted a pair request
		HELPER_NOT_PAIRED(ERROR), // pairing failed
		HELPER_UNHEALTHY(WARNING), // a healthy helper has become unhealthy
		HELPER_HEALTHY(NORMAL), // an unhealthy helper has become healthy
		HELPER_UNPAIRED(NORMAL), // an unpair action successfully or unsuccessfully completed for this helper
		SECRET_UNAVAILABLE(ERROR), // a secret that had previously been usable is now not usable
		SECRET_AVAILABLE(NORMAL); // a secret is now available for use, i.e. a sufficient number of helpers can
		// receive updates and support recovery

		public DeRecStatusNotification.NotificationSeverity getDefaultSeverity() {
			return severity;
		}

		final DeRecStatusNotification.NotificationSeverity severity;

		StandardNotificationType(DeRecStatusNotification.NotificationSeverity severity){
			this.severity = severity;
		}
	}
}
