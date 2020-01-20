package telemetry.GolfBPSK;

import common.Config;
import common.FoxSpacecraft;
import common.Spacecraft;
import decoder.FoxDecoder;
import telemetry.Header;
import telemetry.TelemFormat;

public class GolfBPSKHeader extends Header {
	// Extended Mode Bits that are only in FoxId 6 and later
	int safeMode;
	int healthMode;
	int transponderEnabled;
	int minorVersion;
	TelemFormat telemFormat;
	
	public GolfBPSKHeader(TelemFormat telemFormat) {
		super(TYPE_GOLF_HEADER);
		this.telemFormat = telemFormat;
		MAX_BYTES = telemFormat.getInt(TelemFormat.HEADER_LENGTH);
		rawBits = new boolean[MAX_BYTES*8];
	}
	
	public int getType() { return type; }
	
	/**
	 * Take the bits from the raw bit array and copy them into the fields
	 */
	@Override
	public void copyBitsToFields() {
		resetBitPosition();
		id = nextbits(8);
		type = nextbits(8);
		resets = nextbits(16);
		uptime = nextbits(32);
		minorVersion = nextbits(8);
		safeMode = nextbits(1);
		healthMode = nextbits(1);
		transponderEnabled = nextbits(1);
		setMode();
	}

	public void setMode() {
		newMode = FoxSpacecraft.NO_MODE;
		if (safeMode != 0 ) newMode = FoxSpacecraft.SAFE_MODE;
		if (healthMode != 0 ) newMode = FoxSpacecraft.HEALTH_MODE;
		if (transponderEnabled != 0 ) newMode = FoxSpacecraft.TRANSPONDER_MODE;
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
		
		if (id >= Spacecraft.FIRST_FOXID_WITH_MODE_IN_HEADER)
			s = s + " - MODE: " + newMode;
		return s;
	}
}
