package telemetry.payloads;

import java.util.StringTokenizer;

import telemetry.BitArrayLayout;

public class PayloadWOD extends PayloadRtValues {
	public static final String WOD_RESETS = "WODTimestampReset";
	public static final String WOD_UPTIME = "WODTimestampUptime";
	
	
	public PayloadWOD(BitArrayLayout lay) {
		super(lay);
	}
	
	public PayloadWOD(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);	

	}

	@Override
	protected void init() {
		type = TYPE_WOD;
		fieldValue = new int[layout.NUMBER_OF_FIELDS];
	}

	@Override
	public void captureHeaderInfo(int id, long uptime, int resets) {
		copyBitsToFields();
		this.id = id;
		this.reportDate = fileDateStamp();	
	}
	
	
	@Override
	public void copyBitsToFields() {
		super.copyBitsToFields();
		resets = getRawValue(WOD_RESETS);
		uptime = getRawValue(WOD_UPTIME);
	}
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "WOD Telemetry:\n";
		for (int i=0; i < layout.fieldName.length; i++) {
			s = s + layout.fieldName[i] + ": " + fieldValue[i] + ",   ";
			if ((i+1)%6 == 0) s = s + "\n";
		}
		return s;
	}

	
}
