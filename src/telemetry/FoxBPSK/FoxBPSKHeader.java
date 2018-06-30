package telemetry.FoxBPSK;

import common.Config;
import common.Spacecraft;
import decoder.FoxDecoder;
import telemetry.Header;

public class FoxBPSKHeader extends Header {
	// Extended Mode Bits that are only in FoxId 6 and later
	int safeMode;
	int healthMode;
	int scienceMode;
	int cameraMode;
	int minorVersion;
	
	public FoxBPSKHeader() {
		super(TYPE_EXTENDED_HEADER);
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
		if (id >= Spacecraft.HUSKY_SAT) {
			safeMode = nextbits(1);
			healthMode = nextbits(1);
			scienceMode = nextbits(1);
			cameraMode = nextbits(1);
			minorVersion = nextbits(4);

		}
	}

	public String getMode() {
		if (id >= Spacecraft.HUSKY_SAT) {
			if (safeMode != 0 ) return "SAFE";
			if (healthMode != 0 ) return "HEALTH";
			if (scienceMode != 0 ) return "SCIENCE";
			if (cameraMode != 0 ) return "CAMERA";
			return "UNKNOWN";
		}
		return null;
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
		
		if (id >= Spacecraft.HUSKY_SAT)
			s = s + " - " + getMode() + " MODE";
		return s;
	}
}
