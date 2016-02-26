package gui;
import javax.swing.table.AbstractTableModel;

import telemetry.PayloadHERCIhighSpeed;

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
@SuppressWarnings({ "serial", "unchecked" })
class HerciHsPacketTableModel extends FoxTelemTableModel {
	
	HerciHsPacketTableModel() {
		columnNames = new String[11];
		columnNames[0] = "RESET";
		columnNames[1] = "UPTIME";
		columnNames[2] = "ACQUIRE TIME";
		columnNames[3] = "TYPE";
		columnNames[4] = "LEN";
		columnNames[5] = "RTI";
		columnNames[6] = "SEG";
		columnNames[7] = "ST1";
		columnNames[8] = "ST2";
		columnNames[9] = "ST3";
		columnNames[10] = "COUNT DATA (30 x 1-second integrated counts)";
	}
}