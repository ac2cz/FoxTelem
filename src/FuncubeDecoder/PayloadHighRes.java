package FuncubeDecoder;

import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.Conversion;
import telemetry.FramePart;

public class PayloadHighRes extends FramePart {

	public static final int HIGH_RES_TYPE = 0;

	protected PayloadHighRes(BitArrayLayout l) {
		super(HIGH_RES_TYPE, l);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getStringValue(String name, Spacecraft fox) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
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

	@Override
	protected void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

}
