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
	interface NotificationResponse {
		/**
		 * Set to true if the helper wishes to refuse the request and discontinue the helper relationship. The
		 * semantics are as though {@link DeRecShare#remove()} had been called.
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
	List<? extends DeRecShare> getShares();

	/**
	 * Get a list of all version numbers stored by this helper for a given secret id
	 *
	 * @param secretId secret id
	 * @return a list of all version numbers stored for a given secret
	 */
	List<? extends Integer> getVersionNumbersForASecret(DeRecSecret.Id secretId);

  /**
	 * Get a list of all secrets stored by this helper for a given sharer
	 *
	 * @param sharerStatus sharer
	 * @return a list of secret ids
	 */
	List<? extends DeRecSecret.Id> getSecretIds(DeRecSharerStatus sharerStatus);
  
	/**
	 * Get a list of all sharers that this helper is helping
	 *
	 * @return list of sharers
	 */
	List<? extends DeRecSharerStatus> getSharers();

	/**
	 * Remove a sharer (identified by SharerStatus) as seen by this helper
	 *
	 * @param sharerStatus sharer to remove
	 */
	void removeSharer(DeRecSharerStatus sharerStatus);

	/**
	 * Provide a "listener" for status and lifecycle event notifications relating to this helper's information,
	 * such as changes in the list of shares or requests to pair. The listener both provides information about
	 * an event and also allows the listener to respond positively, or negatively with a reason, which
	 * causes the pairing relationship to end.
	 * <p>
	 * Note: More than one listener may be provided by composition such as:
	 * <p>
	 */
	void setListener(Function<DeRecHelperNotification, NotificationResponse> listener);
}
