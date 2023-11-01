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

package com.thebuildingblocks.derec.v0_9.interfaces;

/**
 * Representation of a sharer as perceived by a helper.
 */
public interface DeRecSharerStatus {
	DeRecHelperInfo getId();

	PairingStatus getStatus();

	enum PairingStatus {
		NONE, // not yet invited
		INVITED, // no reply yet
		PAIRED, // replied positively
		REFUSED, // replied negatively
		PENDING_REMOVAL, // in the process of being removed
		REMOVED, // at sharer request
		FAILED, // timeout, disconnect etc.
		GONE // disconnected at Helper Request
	}
}
