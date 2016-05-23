package FuncubeDecoder;

import common.FoxSpacecraft;
import telemetry.BitArray;
import telemetry.BitArrayLayout;

public class PayloadRealTime extends BitArray {
	int MAX_BYTES;
	public static final int MAX_RT_PAYLOAD_SIZE = 55;
	public String[] fieldName = null;
	protected String[] fieldUnits = null;
	protected int[] fieldBitLength = null;
	public String[] description = null;
	
	protected PayloadRealTime(BitArrayLayout l) {
		super(l);
		MAX_BYTES = MAX_RT_PAYLOAD_SIZE;
		fieldValue = new int[layout.fieldName.length];
		rawBits = new boolean[MAX_BYTES*8];
	}

	@Override
	public String getStringValue(String name, FoxSpacecraft fox) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double convertRawValue(String name, int rawValue, int conversion, FoxSpacecraft fox) {
		// TODO Auto-generated method stub
		return 0;
	}

}
