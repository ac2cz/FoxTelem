package gui.herci;

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
@SuppressWarnings({ "serial"})
class HerciHsPacketTableModel extends FoxTelemTableModel {
	
	HerciHsPacketTableModel() {
		columnNames = new String[12];
		columnNames[0] = "RESET";
		columnNames[1] = "UPTIME";
		columnNames[2] = "FOX SEQ";
		
		columnNames[3] = "ACQUIRE TIME";
		columnNames[4] = "TYPE";
		columnNames[5] = "LEN";
		columnNames[6] = "RTI";
		columnNames[7] = "SEG";
		columnNames[8] = "ST1";
		columnNames[9] = "ST2";
		columnNames[10] = "ST3";
		columnNames[11] = "COUNT DATA (30 x 1-second integrated counts)";
	}
}