package gui.tabs;

import gui.tabs.FoxTelemTableModel;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
@SuppressWarnings("serial")
class CanPacketTableModel extends FoxTelemTableModel {

	CanPacketTableModel() {
//		if (Config.splitCanPackets) {
			columnNames = new String[6];
			columnNames[0] = "ID";
			columnNames[1] = "ID HEX";
			columnNames[2] = "GROUND";
			columnNames[3] = "FRAME";
			columnNames[4] = "SENDER";
			columnNames[5] = "TOTAL";
//		} else {
//			columnNames = new String[6];
//			columnNames[0] = "EPOCH";
//			columnNames[1] = "UPTIME";
//			columnNames[2] = "FOX SEQ";
//			columnNames[3] = "ID";
//			columnNames[4] = "LEN";
//			columnNames[5] = "DATA";
//		}
	}
	
}