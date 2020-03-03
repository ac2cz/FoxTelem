package FuncubeDecoder;

import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.Conversion;
import telemetry.FramePart;

public class PayloadHighRes extends FramePart {

	public static final int HIGH_RES_TYPE = 0;

	protected PayloadHighRes(BitArrayLayout l) {
		super(l, HIGH_RES_TYPE);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getStringValue(String name, Spacecraft fox) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double convertRawValue(String name, int rawValue, int conversion, Spacecraft fox) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double convertCoeffRawValue(String name, int rawValue, Conversion conversion, Spacecraft fox) {
		// TODO Auto-generated method stub
		return 0;
	}

}
