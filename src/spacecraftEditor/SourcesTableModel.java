package spacecraftEditor;

import spacecraftEditor.listEditors.ListTableModel;

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
public class SourcesTableModel extends ListTableModel {

	SourcesTableModel() {
		super();
	}
	
	@Override
	protected void initColumns() {
		columnNames = new String[3];
		columnNames[0] = "Num";
		columnNames[1] = "Name";
		columnNames[2] = "Format";

		columnClass = new Class[] {
				Integer.class,
				String.class, //Name
				String.class
		};

		columnWidths = new int[columnNames.length];
		columnWidths[0] = 20;
		columnWidths[1] = 50;
		columnWidths[2] = 50;
		
	}

}
