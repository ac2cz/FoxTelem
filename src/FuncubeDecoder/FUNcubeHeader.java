package FuncubeDecoder;

import common.Spacecraft;
import decoder.FoxDecoder;
import telemetry.BitArray;
import telemetry.BitArrayLayout;
import telemetry.Conversion;
import telemetry.FoxFramePart;

public class FUNcubeHeader extends BitArray {

	public int id = 100; // SAT ID
	public int type = 0; // unsigned 16 bit integer
	int MAX_BYTES;
	public static final int MAX_FC_HEADER_SIZE = 1;
	
	FUNcubeHeader() {
		super(new BitArrayLayout());
		MAX_BYTES = MAX_FC_HEADER_SIZE;
		rawBits = new boolean[MAX_BYTES*8];
	}
	
	protected void init() { }

	@Override
	public void copyBitsToFields() {
		resetBitPosition();
		id = nextbits(2);
		type = nextbits(6);
		
	}


	@Override
	public String getStringValue(String name, Spacecraft fox) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String toString() {
		copyBitsToFields();
		String s = new String();

		s = s + "AO-73 Telemetry Captured at: " + FoxFramePart.reportDate() + "\n" 
				+ "ID: " + FoxDecoder.dec(id) 
				+ " TYPE: " + FoxDecoder.dec(type);
		
		return s;
	}

	@Override
	protected double convertRawValue(String name, double rawValue, int conversion, Spacecraft fox) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected double convertCoeffRawValue(String name, double rawValue, Conversion conversion, Spacecraft fox) {
		// TODO Auto-generated method stub
		return 0;
	}

}
