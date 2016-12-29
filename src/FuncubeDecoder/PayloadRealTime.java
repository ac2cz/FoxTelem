package FuncubeDecoder;

import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.FramePart;

public class PayloadRealTime extends FramePart {
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
	public String toString() {
		copyBitsToFields();
		return "FUNcube RealTime id:"+id;
	}

}
