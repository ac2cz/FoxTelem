package telemetry;

import java.util.ArrayList;

import common.Config;
import common.Log;

public class PayloadCameraScanLineCount extends FramePart {

	int scanLineCount = 0;
	private boolean foundEndOfJpegData = false;
	
	public PayloadCameraScanLineCount() {
		super(new BitArrayLayout());
	}

	@Override
	protected void init() { 
		MAX_BYTES = 1;
	}
	
	public boolean foundEndOfJpegData() { return foundEndOfJpegData; }
	public int getScanLineCount() { return scanLineCount; }
	public void setFoundEndOfJpegData() { foundEndOfJpegData = true; }
	
	public void addNext8Bits(byte b) {
		
		super.addNext8Bits(b);
			resetBitPosition();
			scanLineCount = nextbits(8);
			if (scanLineCount == 0) 
				foundEndOfJpegData = true;
			if (Config.debugCameraFrames)
				Log.print("PayloadCameraSLC: " + scanLineCount + " endJpeg:" + foundEndOfJpegData());
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
