package telemetry.payloads;

import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.FoxPayloadStore;
import telemetry.FramePart;

public class PayloadCanWODExperiment extends PayloadCanExperiment {
	
	public static final String WOD_RESETS = "WODTimestampReset";
	public static final String WOD_UPTIME = "WODTimestampUptime";
//	public static final int CRC_ERROR_FIELD = 3;
	public static final String CRC_ERROR = "crcError";
//	public static final int PAD2_FIELD = 4;
//	public static final String PAD2 = "pad2";
		
	public PayloadCanWODExperiment(BitArrayLayout lay, int id, long uptime, int resets) {
		super(lay, id, uptime, resets);
		// TODO Auto-generated constructor stub
	}

	public PayloadCanWODExperiment(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);
	}

	
	@Override
	protected void init() {
		// nothing to add here
	}
	
	@Override
	public void copyBitsToFields() {
		super.copyBitsToFields();
		resets = getRawValue(WOD_RESETS);
		uptime = getRawValue(WOD_UPTIME);
	}
	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = "CAN WOD EXPERIMENT PAYLOAD - " + canPackets.size() + " CAN PACKETS\n";
		s = s + "RESET: " + getRawValue(WOD_RESETS);
		s = s + "  UPTIME: " + getRawValue(WOD_UPTIME);
		s = s + "  TYPE: " +  type;
		s = s + "  OVERFLOW FLAG: " + getRawValue(CRC_ERROR) + "\n";
		for (int p=0; p < canPackets.size(); p++) {
			s = s + canPackets.get(p).toString() + "    " ;
			if ((p+1)%3 == 0) s = s + "\n";
		}
		s=s+"\n";

		return s;
	}
	
}
