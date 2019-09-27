package predict;

import telemetry.FramePart;

@SuppressWarnings("serial")
public class PositionCalcException extends Exception {

	public double errorCode = FramePart.NO_POSITION_DATA;
	
	public PositionCalcException(double exceptionCode) {
		super("Error: " + exceptionCode);
		errorCode = exceptionCode;
		
	}
}
