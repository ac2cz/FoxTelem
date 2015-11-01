package telemServer;

@SuppressWarnings("serial")
public class StpFileProcessException extends Exception {

	public String fileName;
	
	public StpFileProcessException(String file, String error) {
		super(error);
		fileName = file;
	}
}
