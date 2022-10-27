package telemetry.payloads;

import java.util.StringTokenizer;

import telemetry.BitArrayLayout;
import telemetry.FramePart;

public class PayloadWODExperiment extends FramePart {
	public static final String WOD_RESETS = "WODTimestampReset";
	public static final String WOD_UPTIME = "WODTimestampUptime";
	public static final String CRC_ERROR = "crcError";
		
	public PayloadWODExperiment(BitArrayLayout lay, int id, long uptime, int resets) {
		super(TYPE_WOD_RAG,lay);
		captureHeaderInfo(id, uptime, resets);
	}

	public PayloadWODExperiment(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_WOD_RAG, date, st, lay);
	}
	
	@Override
	protected void init() {
		// nothing extra to init here
	}	
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
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
		String s = "RAG WOD ADAC PAYLOAD\n";
		s = s + "RESET: " + getRawValue(WOD_RESETS);
		s = s + "  UPTIME: " + getRawValue(WOD_UPTIME);
		s = s + "  TYPE: " +  type + "\n";

		return s;
	}
	
}
