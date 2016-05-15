package telemetry;

public class PayloadHerciLineCount extends FramePart {

	int scanLineCount = 1; // default to true so that we initialize it the first time we populate data
	
	public PayloadHerciLineCount() {
		super(new BitArrayLayout());
	}

	@Override
	protected void init() { 
		MAX_BYTES = 1;
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
