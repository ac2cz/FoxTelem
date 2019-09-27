package telemetry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class FrameLayout {
	public static final String NUMBER_OF_PAYLOADS="number_of_payloads";
	
	public Properties properties; // Java properties file for user defined values
	public File propertiesFile;
	String fileName;
	public String name;
	
	public FrameLayout(String fileName) throws LayoutLoadException {
		properties = new Properties();
		this.fileName = fileName;
		propertiesFile = new File(fileName);
		load();
		try { int i = getInt(NUMBER_OF_PAYLOADS); } catch (Exception e) {
			throw new LayoutLoadException("Invalid or missing "+NUMBER_OF_PAYLOADS+" in FrameLayout file: " + propertiesFile.getAbsolutePath());
		}
	}
	
	protected void load() throws LayoutLoadException {
		// try to load the properties from a file
		FileInputStream f = null;
		try {
			f=new FileInputStream(propertiesFile);
			properties.load(f);
			f.close();
		} catch (IOException e) {
			if (f!=null) try { f.close(); } catch (Exception e1) {};
			throw new LayoutLoadException("Could not load FrameLayout file: " + propertiesFile.getAbsolutePath());
		}
	}
	
	public String getPayloadName(int i) {
		return get("payload"+i+".name");
	}
	
	public String get(String key) {
		return properties.getProperty(key);
	}
	
	public void set(String sat, String fieldName, String key, String value) {
		properties.setProperty(sat + fieldName + key, value);
		//store();
	}
	
	public String get(String sat, String fieldName, String key) {
		return properties.getProperty(sat + fieldName + key);
	}
	
	public void set(String key, int value) {
		properties.setProperty(key, Integer.toString(value));
		//store();
	}

	public void set(String sat, String fieldName, String key, int value) {
		properties.setProperty(sat +  fieldName + key, Integer.toString(value));
		//store();
	}

	public void set(String sat, String fieldName, String key, long value) {
		properties.setProperty(sat +  fieldName + key, Long.toString(value));
		//store();
	}
	public int getInt(String key) {
		return Integer.parseInt(properties.getProperty(key)); 
	}
	public int getInt(String sat, String fieldName, String key) {
		return Integer.parseInt(properties.getProperty(sat +  fieldName + key));

	}

	public boolean getBoolean(String key) {
		return Boolean.parseBoolean(properties.getProperty(key));
	}
	public boolean getBoolean(String sat, String fieldName, String key) {
		return Boolean.parseBoolean(properties.getProperty(sat +  fieldName + key));
	}
}
