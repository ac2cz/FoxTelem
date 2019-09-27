package device;

@SuppressWarnings("serial")
public class SourceException extends DeviceException {

	public SourceException(String string) {
		super(string);
		// TODO Auto-generated constructor stub
	}

	public SourceException(String string, String str2) {
		super(string + ": " + str2);
		// TODO Auto-generated constructor stub
	}
}
