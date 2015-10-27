package telemServer;

import java.util.Properties;

/**
 * Most of the program properties are read from the standard Config - such as for file locations
 * @author chris.e.thompson
 *
 */
public class ServerConfig {

	public static Properties properties; // Java properties file for user defined values

	public static String VERSION_NUM = "0.01";
	public static String VERSION = VERSION_NUM + " - 26 October 2015";
	public static final String propertiesFileName = "FoxTelem.properties";

}
