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

import java.util.List;
import java.util.function.Consumer;
/**
 * The main API for a DeRec helper app.
 * Instantiating this will start the threads that listen for incoming messages, and respond to them.
 * This class also provides messaging and getters to let the app view current secrets and sharers.
 * This also allows for pairing to be initiated, if the pairing started with the sharer giving a contact to the helper.
 *
 * NOTE: this file is a draft, and may change substantially.
 */
public interface DeRecHelper {
	/**
	 * Get the secret with this ID, return null if none with this ID
	 *
	 * @param secretId a secret ID
	 * @return a secret or null
	 */
	DeRecSecret getSecret(byte[] secretId);

	/**
	 * Get a list of all secrets known to this helper
	 *
	 * @return a list
	 */
	List<? extends DeRecSecret> getSecrets();

	/**
	 * Provide a "listener" for status and lifecycle event notifications relating to this helper's information,
	 * such as changes in the list of sharers or secrets or their status.
	 * <p>
	 * Note: More than one listener may be provided by composition such as:
	 * <p>
	 * <pre>{@code
	 * Consumer<DeRecStatusNotification> listener1 = n -> log(n.getType().name());
	 * Consumer<DeRecStatusNotification> listener2 = n -> {if (n.getSeverity().equals(ERROR)) alert(n.getType().name());};
	 * sharer.setListener(listener1.andThen(listener2));
	 * }</pre>
	 */
	void setListener(Consumer<DeRecStatusNotification> listener);
}
