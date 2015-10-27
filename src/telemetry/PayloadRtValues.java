package telemetry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import decoder.Decoder;

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
public class PayloadRtValues extends FramePart {

	//public static final int NUMBER_OF_FIELDS = 43;
	
	public PayloadRtValues(BitArrayLayout lay) {
		super(lay);
	}
	
	public PayloadRtValues(ResultSet r, BitArrayLayout lay) throws SQLException {
		super(r, lay);
	}
	
	public PayloadRtValues(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);	
	}

	protected void init() {
		type = TYPE_REAL_TIME;
		fieldValue = new int[layout.NUMBER_OF_FIELDS];
	}
	
	
	
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "REAL TIME Telemetry:\n" 
				+ "BATT_A_V: " + Decoder.dec(getRawValue("BATT_A_V")) 
				+ " BATT_B_V: " + Decoder.dec(getRawValue("BATT_B_V")) 
				+ " BATT_C_V: " + Decoder.dec(getRawValue("BATT_C_V"))
				+ "\n"
				+ "BATT_A_T: " + Decoder.dec(getRawValue("BATT_A_T")) 
				+ " BATT_B_T: " + Decoder.dec(getRawValue("BATT_B_T"))
				+ " BATT_C_T: " + Decoder.dec(getRawValue("BATT_C_T"))
				+ "\n"
				+ "TOTAL_BATT_I: " + Decoder.dec(getRawValue("TOTAL_BATT_I")) 
				+ " BATTBoardTemperature: " + Decoder.dec(getRawValue("BATTBoardTemperature"))
				+ "\n"
				+ "PANEL_PLUS_X_V: " + Decoder.dec(getRawValue("PANEL_PLUS_X_V")) 
				+ " PANEL_MINUS_X_V: " + Decoder.dec(getRawValue("PANEL_MINUS_X_V"))
				+ " PANEL_PLUS_Y_V: " + Decoder.dec(getRawValue("PANEL_PLUS_Y_V"))
				+ " PANEL_MINUS_Y_V: " + Decoder.dec(getRawValue("PANEL_MINUS_Y_V"))
				+ " PANEL_PLUS_Z_V: " + Decoder.dec(getRawValue("PANEL_PLUS_Z_V"))
				+ " PANEL_MINUS_Z_V: " + Decoder.dec(getRawValue("PANEL_MINUS_Z_V"))
				+ "\n"
				+ "PANEL_PLUS_X_T: " + Decoder.dec(getRawValue("PANEL_PLUS_X_T")) 
				+ " PANEL_MINUS_X_T: " + Decoder.dec(getRawValue("PANEL_MINUS_X_T"))
				+ " PANEL_PLUS_Y_T: " + Decoder.dec(getRawValue("PANEL_PLUS_Y_T"))
				+ " PANEL_MINUS_Y_T: " + Decoder.dec(getRawValue("PANEL_MINUS_Y_T"))
				+ " PANEL_PLUS_Z_T: " + Decoder.dec(getRawValue("PANEL_PLUS_Z_T"))
				+ " PANEL_MINUS_Z_T: " + Decoder.dec(getRawValue("PANEL_MINUS_Z_T"))
				+ "\n"
				+ "PSUTemperature: " + Decoder.dec(getRawValue("PSUTemperature")) 
				+ " SPIN: " + (getRawValue("SPIN"))
				+ "\n"
				+ "TXPACurrent: " + Decoder.dec(getRawValue("TXPACurrent")) 
				+ " TXTemperature: " + Decoder.dec(getRawValue("TXTemperature"))
				+ "\n"
				+ "RXTemperature: " + Decoder.dec(getRawValue("RXTemperature")) 
				+ " RSSI: " + Decoder.dec(getRawValue("RSSI"))
				+ "\n"
				+ "IHUTemperature: " + Decoder.dec(getRawValue("IHUTemperature")) 
				+ " AntennaDeploySensors: " + (int)(getRawValue("RXAntenna")) + " "
				+ (int)(getRawValue("TXAntenna"))
				+ "\n"
				+ "SatelliteXAxisAngularVelocity: " + Decoder.dec(getRawValue("SatelliteXAxisAngularVelocity")) + " "
				+ " SatelliteYAxisAngularVelocity: " + Decoder.dec(getRawValue("SatelliteYAxisAngularVelocity"))+ " "
				+ " SatelliteZAxisAngularVelocity: " + Decoder.dec(getRawValue("SatelliteZAxisAngularVelocity"))+ " "
				+ "\n"
				+ "EXP4Temp: " + Decoder.dec(getRawValue("EXP4Temperature")) 
				+ " PSUCurrent: " + Decoder.dec(getRawValue("PSUCurrent"))
				+ "\n"
				+ "IHUDiagnosticData: " + Decoder.dec(getRawValue("IHUDiagnosticData"))
				+ "\n"
				+ "ExperimentFailureIndication: " + (int)(getRawValue("Experiment1FailureIndication")) + " "
				+ (int)(getRawValue("Experiment2FailureIndication")) + " "
				+ (int)(getRawValue("Experiment3FailureIndication")) + " "
				+ (int)(getRawValue("Experiment4FailureIndication"))+ " "
				+ "\n"
				+ "SystemI2CFailureIndications: " + (int)(getRawValue("BATTI2CFailureIndications")) + " "
				+ (int)(getRawValue("PSU1I2CFailureIndications")) + " "
				+ (int)(getRawValue("PSU2I2CFailureIndications")) + " "
				+ "\n"
				+ "NumberofGroundCommandedTLMResets: " + Decoder.dec(getRawValue("NumberofGroundCommandedTLMResets"))
				;
		return s;
		
	}
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}


}
