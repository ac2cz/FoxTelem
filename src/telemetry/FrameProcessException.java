package telemetry;

@SuppressWarnings("serial")
public class FrameProcessException extends Exception {

	
	public FrameProcessException(String error) {
		super(error);
	}
	
	public String getMessage() {
		String s = super.getMessage();
		return s;
	}
}
