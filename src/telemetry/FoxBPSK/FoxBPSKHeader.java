package telemetry.FoxBPSK;

import common.Config;
import common.FoxSpacecraft;
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
		if (rawBits != null) { // only convert if we actually have a raw binary array.  Otherwise this was loaded from a file and we do not want to convert
			super.copyBitsToFields();
			type = nextbits(4);
			if (id == 0) // then take the foxId from the next 8 bits
				id = nextbits(8);
			if (id > Spacecraft.FOX1E) { // Post Fox-1E BPSK has mode in header
				safeMode = nextbits(1);
				healthMode = nextbits(1);
				scienceMode = nextbits(1);
				cameraMode = nextbits(1);
				minorVersion = nextbits(4);
			}
			setMode();
		}
	}

	public void setMode() {
		newMode = FoxSpacecraft.NO_MODE;
		if (id > Spacecraft.FOX1E) {
			if (safeMode != 0 ) newMode = FoxSpacecraft.SAFE_MODE;
			if (healthMode != 0 ) newMode = FoxSpacecraft.HEALTH_MODE;
			if (scienceMode != 0 ) newMode = FoxSpacecraft.SCIENCE_MODE;
			if (cameraMode != 0 ) newMode = FoxSpacecraft.CAMERA_MODE;
		}
	
	}

	public boolean isValid() {
		copyBitsToFields();
		if (Config.satManager.validFoxId(id))
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
		s = s	+ "ID: " + FoxDecoder.dec(id) 
				+ " RESET COUNT: " + FoxDecoder.dec(resets)
				+ " UPTIME: " + FoxDecoder.dec(uptime)
				+ " TYPE: " + FoxDecoder.dec(type);
		
		if (id > Spacecraft.FOX1E)
			s = s + " - MODE: " + newMode;
		return s;
	}
}
