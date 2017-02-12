package telemetry;

import decoder.FoxDecoder;

public class PayloadWODRad extends FoxFramePart {
	public static final String WOD_RESETS = "WODTimestampReset";
	public static final String WOD_UPTIME = "WODTimestampUptime";

	public PayloadWODRad(BitArrayLayout lay) {
		super(lay);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void init() {
		type = TYPE_WOD;
		fieldValue = new int[layout.NUMBER_OF_FIELDS];
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
		s = s + "WOD RADIATION EXPERIMENT DATA:\n";
		for (int i =0; i< PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE; i++) {
			s = s + FoxDecoder.hex(fieldValue[i]) + " ";
			// Print 8 bytes in a row
			if ((i+1)%8 == 0) s = s + "\n";
		}
		for (int i=PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE; i < layout.fieldName.length; i++) {
			s = s + layout.fieldName[i] + ": " + fieldValue[i] + ",   ";
			if ((i+1)%6 == 0) s = s + "\n";
		}
		return s;
	}


}
