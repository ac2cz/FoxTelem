package telemetry.conversion;

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
 *
 * Look up table for the temperature sensors on the solar panels
 *
 */
@Deprecated
public class LookUpTableSolarPanelTemp extends ConversionLookUpTable {
	@Deprecated
	public LookUpTableSolarPanelTemp() {
		super("LookUpTableSolarPanelTemp", null);
		table.put(2998,95.0);
		table.put(3002,90.0);
		table.put(3006,85.0);
		table.put(3011,80.0);
		table.put(3017,75.0);
		table.put(3024,70.0);
		table.put(3032,65.0);
		table.put(3042,60.0);
		table.put(3053,55.0);
		table.put(3066,50.0);
		table.put(3082,45.0);
		table.put(3100,40.0);
		table.put(3121,35.0);
		table.put(3145,30.0);
		table.put(3174,25.0);
		table.put(3207,20.0);
		table.put(3244,15.0);
		table.put(3286,10.0);
		table.put(3332,5.0);
		table.put(3382,0.0);
		table.put(3435,-5.0);
		table.put(3489,-10.0);
		table.put(3545,-15.0);
		table.put(3599,-20.0);
		table.put(3650,-25.0);
		table.put(3697,-30.0);
		table.put(3738,-35.0);
		table.put(3775,-40.0);
		//table.put(3910,FramePart.ERROR_VALUE);
		//table.put(3911,FramePart.ERROR_VALUE);
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
	}

}
