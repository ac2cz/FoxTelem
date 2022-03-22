package spacecraftEditor.listEditors.curves;

import spacecraftEditor.listEditors.CsvTableModel;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2022 amsat.org
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
public class CurvesTableModel extends CsvTableModel {

	public CurvesTableModel() {
		super();
	}
	
	protected void initColumns() {
		columnNames = new String[8];
		columnNames[0] = "Curve Name";
		columnNames[1] = "a";
		columnNames[2] = "bx";
		columnNames[3] = "cx^2";
		columnNames[4] = "dx^3";
		columnNames[5] = "ex^4";
		columnNames[6] = "fx^5";
		columnNames[7] = "Description";

		columnClass = new Class[] {
		        String.class, //Name
		        String.class, // a
		        String.class, // b
		        String.class, // c
		        String.class, // d
		        String.class, // e
		        String.class, // f
		        String.class // Description
		 };
		
		columnWidths = new int[columnNames.length];
		columnWidths[0] = 100;
		columnWidths[1] = 50;
		columnWidths[2] = 50;
		columnWidths[3] = 50;
		columnWidths[4] = 50;
		columnWidths[5] = 50;
		columnWidths[6] = 50;
		columnWidths[7] = 200;
	}

}
