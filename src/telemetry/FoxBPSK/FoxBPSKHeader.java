package telemetry.FoxBPSK;

import common.Config;
import decoder.FoxDecoder;
import telemetry.Header;

public class FoxBPSKHeader extends Header {
	
	public FoxBPSKHeader() {
		MAX_BYTES = FoxBPSKFrame.MAX_HEADER_SIZE;
		rawBits = new boolean[MAX_BYTES*8];
	}
	
	public int getType() { return type; }
	
	/**
	 * Take the bits from the raw bit array and copy them into the fields
	 */
	public void copyBitsToFields() {
		super.copyBitsToFields();
		type = nextbits(4);
		if (id == 0) // then take the foxId from the next 8 bits
			id = nextbits(8);
	}
	
	public boolean isValid() {
		copyBitsToFields();
		if (Config.satManager.validFoxId(id) && isValidType(type))
			return true;
		return false;
	}
	
	// TODO: This needs to be the 1E frame types
	public boolean isValidType(int t) {
	
		assert(false);
		return false;
			
	}
		
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "AMSAT FOX-1 BPSK Telemetry Captured at: " + reportDate() + "\n" 
				+ "ID: " + FoxDecoder.dec(id) 
				+ " RESET COUNT: " + FoxDecoder.dec(resets)
				+ " UPTIME: " + FoxDecoder.dec(uptime)
				+ " TYPE: " + FoxDecoder.dec(type);
		return s;
	}
}
