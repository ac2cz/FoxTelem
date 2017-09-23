package predict;

import telemetry.FramePart;

public class PositionCalcException extends Exception {

	public double errorCode = FramePart.NO_POSITION_DATA;
	
	public PositionCalcException(double exceptionCode) {
		errorCode = exceptionCode;
	}
}
