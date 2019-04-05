package telemetry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import common.Config;
import common.FoxSpacecraft;

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
public class PayloadRtValues extends FoxFramePart {

	//public static final int NUMBER_OF_FIELDS = 43;
	
	public PayloadRtValues(BitArrayLayout lay) {
		super(TYPE_REAL_TIME, lay);
	}
	
	public PayloadRtValues(ResultSet r, BitArrayLayout lay) throws SQLException {
		super(r, TYPE_REAL_TIME, lay);
	}
	
	public PayloadRtValues(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_REAL_TIME, date, st, lay);	
	}

	protected void init() {
		// Any local init goes here
	}
	
	public String toWebString() {
		
		///// NEED TO DO THIS BY LOOPING THROUGH THE LAYOUT AND SUPPORT RAW / CONVERTED
		
		//copyBitsToFields(); // dont need this as we loaded from the DB for the Web service
		
		FoxSpacecraft fox = (FoxSpacecraft) Config.satManager.getSpacecraft(id);
		String s = new String();
		s = s + "<h3>Fox "+ fox.getIdString()+"  REAL TIME Telemetry   Reset: " + resets + " Uptime: " + uptime + "</h3>"
				+ "<table>"
				
				+ "<tr bgcolor=silver>"
				+ "<td><h3>Radio</h3>"
				+ "</td>"
				+ "<td><h3>Computer</h3>"
				+ "</td>"
				+ "<td><h3>Battery</h3>"
				+ "</td>"
				+ "</tr>"

				+ "<tr>"
				+ "<td>"
				+ "TXPACurrent: " + getStringValue("TXPACurrent", fox)  + "<br>" 
				+ " TXTemperature: " + getStringValue("TXTemperature", fox) + "<br>" 
				
				+ "RXTemperature: " + getStringValue("RXTemperature", fox)  + "<br>" 
				+ " RSSI: " + getStringValue("RSSI", fox) + "<br>" 
				+ "\n" + "<br>" 
				+ " AntennaDeploySensors: " + getStringValue("RXAntenna", fox) + " "
				+ getStringValue("TXAntenna", fox) + "<br>" 
				
				+ "</td>"
				
				+ "<td>"
				+ "IHUTemperature: " + getStringValue("IHUTemperature", fox) + "<br>"
				+ " SPIN: " + getStringValue("SPIN", fox)+ "<br>"
				+ "IHUDiagnosticData: " + getStringValue("IHUDiagnosticData", fox) + "<br>"
				+ "SystemI2CFailureIndications: " + getStringValue("BATTI2CFailureIndications", fox) + " "
				+ getStringValue("PSU1I2CFailureIndications", fox) + " "
				+ getStringValue("PSU2I2CFailureIndications", fox) + " "
				+ "\n" + "<br>"
				+ "NumberofGroundCommandedTLMResets: " + getStringValue("NumberofGroundCommandedTLMResets", fox)
				
				+ "</td>"
				
				+ "<td>"
				+ "BATT_A_V: " + getStringValue("BATT_A_V", fox) + "<br>" 
				+ " BATT_B_V: " + getStringValue("BATT_B_V", fox) + "<br>" 
				+ " BATT_C_V: " + getStringValue("BATT_C_V", fox) + "<br>"
				+ "\n"
				+ "BATT_A_T: " + getStringValue("BATT_A_T", fox)  + "<br>"
				+ " BATT_B_T: " + getStringValue("BATT_B_T", fox) + "<br>"
				+ " BATT_C_T: " + getStringValue("BATT_C_T", fox) + "<br>"
				+ "\n" + "<br>"
				+ "TOTAL_BATT_I: " + getStringValue("TOTAL_BATT_I", fox)  + "<br>"
				+ " BATTBoardTemperature: " + getStringValue("BATTBoardTemperature", fox) + "<br>"

				+ "</td>"
				+ "</tr>"
				
				+ "<tr bgcolor=silver>"
				+ "<td><h3>Power Supply</h3>"
				+ "</td>"
				+ "<td><h3></h3>"
				+ "</td>"
				+ "<td><h3>Experiments</h3>"
				+ "</td>"
				+ "</tr>"

				+ "<tr>"
				+ "<td>"
				+ "PSUTemperature: " + getStringValue("PSUTemperature", fox) + "<br>"
				+ " PSUCurrent: " + getStringValue("PSUCurrent", fox)+ "<br>"
				+ "\n"
				
				+ "</td>"
				
				+ "<td>"
				+ "</td>"
				
				+ "<td>"
				+ "EXP4Temp: " + getStringValue("EXP4Temperature", fox) + "<br>"
				+ "ExperimentFailureIndication: " + getStringValue("Experiment1FailureIndication", fox) + " "+ "<br>"
				+ getStringValue("Experiment2FailureIndication", fox) + " "+ "<br>"
				+ getStringValue("Experiment3FailureIndication", fox) + " "+ "<br>"
				+ getStringValue("Experiment4FailureIndication", fox)+ " "+ "<br>"
				+ "\n"
				
				+ "</tr>"
				
				+ "<tr bgcolor=silver>"
				+ "<td><h3>X Panels</h3>"
				+ "</td>"
				+ "<td><h3>Y Panels</h3>"
				+ "</td>"
				+ "<td><h3>Z Panels</h3>"
				+ "</td>"
				+ "</tr>"

				
				+ "<tr>"
				+ "</td>"
				+ "<td>"
				+ "PANEL_PLUS_X_V: " + getStringValue("PANEL_PLUS_X_V", fox) + "<br>"
				+ " PANEL_MINUS_X_V: " + getStringValue("PANEL_MINUS_X_V", fox)+ "<br>"
				+ "PANEL_PLUS_X_T: " + getStringValue("PANEL_PLUS_X_T", fox)+ "<br>" 
				+ " PANEL_MINUS_X_T: " + getStringValue("PANEL_MINUS_X_T", fox)+ "<br>"
				+ "SatelliteXAxisAngularVelocity: " + getStringValue("SatelliteXAxisAngularVelocity", fox) + " "+ "<br>"
				
				+ "</td>"

				+ "<td>"
				+ " PANEL_PLUS_Y_V: " + getStringValue("PANEL_PLUS_Y_V", fox)+ "<br>"
				+ " PANEL_MINUS_Y_V: " + getStringValue("PANEL_MINUS_Y_V", fox)+ "<br>"
				+ " PANEL_PLUS_Y_T: " + getStringValue("PANEL_PLUS_Y_T", fox)+ "<br>"
				+ " PANEL_MINUS_Y_T: " + getStringValue("PANEL_MINUS_Y_T", fox)+ "<br>"
				+ " SatelliteYAxisAngularVelocity: " + getStringValue("SatelliteYAxisAngularVelocity", fox)+ " "+ "<br>"
				
				+ "</td>"

				+ "<td>"
				+ " PANEL_PLUS_Z_V: " + getStringValue("PANEL_PLUS_Z_V", fox)+ "<br>"
				+ " PANEL_MINUS_Z_V: " + getStringValue("PANEL_MINUS_Z_V", fox)+ "<br>"
				+ " PANEL_PLUS_Z_T: " + getStringValue("PANEL_PLUS_Z_T", fox)+ "<br>"
				+ " PANEL_MINUS_Z_T: " + getStringValue("PANEL_MINUS_Z_T", fox)+ "<br>"
				+ " SatelliteZAxisAngularVelocity: " + getStringValue("SatelliteZAxisAngularVelocity", fox)+ " "+ "<br>"
				
				+ "</td>"

				+ "</tr>"
				
				+ "\n"
				+ "</table>";
		return s;
		
	}
	
	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "REAL TIME Telemetry:\n";
		for (int i=0; i < layout.fieldName.length; i++) {
			s = s + layout.fieldName[i] + ": " + fieldValue[i] + ",   ";
			if ((i+1)%6 == 0) s = s + "\n";
		}
		return s;
	}
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}


}
