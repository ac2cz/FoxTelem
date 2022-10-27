package telemetry.herci;

import telemetry.BitArrayLayout;
import telemetry.FramePart;

public class PayloadHerciLineCount extends FramePart {

	int scanLineCount = 1; // default to true so that we initialize it the first time we populate data
	
	public PayloadHerciLineCount() {
		super(TYPE_HERCI_LINE_COUNT, new BitArrayLayout());
	}

	@Override
	protected void init() { 
		MAX_BYTES = 1;
		rawBits = new boolean[MAX_BYTES*8];
		fieldValue = new int[MAX_BYTES];
	}
	
	public int getLineCount() { return scanLineCount; }
	public boolean hasData() {
		if (scanLineCount == 0) return false;
		return true;
	}
	
	public void zeroLineCount() { scanLineCount = 0; }
	
	public void addNext8Bits(byte b) {
		super.addNext8Bits(b);
			resetBitPosition();
			scanLineCount = nextbits(8);
	}
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
