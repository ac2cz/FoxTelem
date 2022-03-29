package FuncubeDecoder;

import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.conversion.Conversion;

public class PayloadFitterMessages extends FramePart {

	public static final int FITTER_TYPE = 0;
	
	protected PayloadFitterMessages(BitArrayLayout l) {
		super(FITTER_TYPE, l);
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
