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
import java.util.function.Consumer;
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

		enum PairingResponseStatus {
			PAIRED, // replied positively
			REFUSED, // replied negatively
			REMOVED, // at sharer request, potentially as a result of a DISCONNECT from helper
			PENDING_DISCONNECTION // disconnecting at Helper Request
		}
	}

	interface Share {
		SharerStatus getSharer();

		DeRecSecret.Id getSecretId();

		int getVersion();

		void remove();
	}

	interface Notification {

	}

	/**
	 * Get a list of all protected items known to this helper
	 *
	 * @return a list
	 */
	List<? extends DeRecHelper.Share> getShares();

	/**
	 * Provide a "listener" for status and lifecycle event notifications relating to this helper's information,
	 * such as changes in the list of shares or requests to pair.
	 * <p>
	 * Note: More than one listener may be provided by composition such as:
	 * <p>
	 * <pre>{@code
	 * Consumer<DeRecStatusNotification> listener1 = n -> log(n.getType().name());
	 * Consumer<DeRecStatusNotification> listener2 = n -> {if (n.getSeverity().equals(ERROR)) alert(n.getType().name());};
	 * sharer.setListener(listener1.andThen(listener2));
	 * }</pre>
	 */
	void setListener(Consumer<DeRecHelper.Notification> listener);
}
