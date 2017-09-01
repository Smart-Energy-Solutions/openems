/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.protocol.studer;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;

@ThingInfo(title = "Studer")
public abstract class StuderDevice extends Device {

	/*
	 * Constructors
	 */
	public StuderDevice(Bridge parent) throws OpenemsException {
		super(parent);
	}

	/*
	 * Config
	 */
	@ConfigInfo(title = "Address", description = "Sets the device address (e.g. 701).", type = Integer.class)
	public final ConfigChannel<Integer> address = new ConfigChannel<Integer>("address", this);

	/*
	 * Methods
	 */

	protected int getDstAddress() throws OpenemsException {
		int dstAddress;
		try {
			dstAddress = this.address.value();
		} catch (Throwable e) {
			throw new OpenemsException("Unable to find dstAddress: " + e.getMessage());
		}
		return dstAddress;
	}
}
