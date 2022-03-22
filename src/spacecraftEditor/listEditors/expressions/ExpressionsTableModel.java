package spacecraftEditor.listEditors.expressions;

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
public class ExpressionsTableModel extends CsvTableModel{

	private static final long serialVersionUID = 1L;

	public ExpressionsTableModel() {
		super();
	}

	protected void initColumns() {
		columnNames = new String[3];
		columnNames[0] = "Name";
		columnNames[1] = "Expression";
		columnNames[2] = "Description";

		columnClass = new Class[] {
				String.class, //Name
				String.class, // exp
				String.class
		};

		columnWidths = new int[columnNames.length];
		columnWidths[0] = 50;
		columnWidths[1] = 300;
		columnWidths[2] = 200;
	}

}
