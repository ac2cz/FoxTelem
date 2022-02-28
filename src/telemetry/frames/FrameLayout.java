package telemetry.frames;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import common.Config;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;

public class FrameLayout {
//	public static final String NUMBER_OF_PAYLOADS = "number_of_payloads";
//	public static final String PAYLOAD = "payload";
//	public static final String DOT_LENGTH = ".length";
	
	public Properties properties; // Java properties file for user defined values
	public File propertiesFile;
	String fileName;
	public String name;
	HashMap<Integer,String> payloadNames;
	int foxId;
	
	public FrameLayout(int foxId, String fileName) throws LayoutLoadException {
		properties = new Properties();
		this.fileName = fileName;
		this.foxId = foxId;
		propertiesFile = new File(fileName);
		payloadNames = new HashMap<Integer,String>();
		load();
//		try { getInt(NUMBER_OF_PAYLOADS); } catch (Exception e) {
//			throw new LayoutLoadException("Invalid or missing "+NUMBER_OF_PAYLOADS+" in FrameLayout file: " + propertiesFile.getAbsolutePath());
//		}
	}
	
	protected void load() throws LayoutLoadException {
		// try to load the properties from a file
		FileInputStream f = null;
		try {
			f=new FileInputStream(propertiesFile);
			properties.load(f);
			f.close();

			@SuppressWarnings("unchecked")
			Enumeration<String> enums = (Enumeration<String>) properties.propertyNames();
			while (enums.hasMoreElements()) {
				try {
					// If we have a line that starts with payloadX then grab the number and ignore rest of key
					String key = enums.nextElement();
					String[] keyparts = key.split("\\.");
					String part = keyparts[0].substring(0, 7);
					String index = keyparts[0].substring(7);
					int idx = Integer.parseInt(index);
					if (part.equalsIgnoreCase("payload")) {
						if (keyparts[1].equalsIgnoreCase("name")) {
							String value = properties.getProperty(key);
							//System.out.println(key + " : " + value);
							payloadNames.put(idx, value);
						}
					}
				} catch (NumberFormatException e) {
					// skip this one as we could not parse the index
				}
			}
			// Some integrity checks
			if (payloadNames.size() == 0)
				throw new LayoutLoadException("Empty Frame Definition File.  No Payloads were defined in the Frame definition file.  It will not be loaded.");
			for (int i=0; i < payloadNames.size(); i++) {
				String payloadName = payloadNames.get(i);
				if (payloadName == null)
					throw new LayoutLoadException("Frame Definition File: "+ propertiesFile.getAbsolutePath()+"\n missing payload number "+ i +".  Was that defined in the file? It will not be loaded.");
			}
		} catch (IOException e) {
			if (f!=null) try { f.close(); } catch (Exception e1) {};
			throw new LayoutLoadException("Could not load FrameLayout file: " + propertiesFile.getAbsolutePath());
		} 
	}
	
	public String getPayloadName(int i) {
		return payloadNames.get(i);
	}
	
	public BitArrayLayout getPayload(int i) {
		String name = payloadNames.get(i);
		BitArrayLayout lay = Config.satManager.getLayoutByName(foxId, name);
		return lay;
	}
	
	public int getPayloadLength(int i) {
		String name = payloadNames.get(i);
		BitArrayLayout lay = Config.satManager.getLayoutByName(foxId, name);
		return lay.getMaxNumberOfBytes();
	}
	
	public int getNumberOfPayloads() {
		return payloadNames.size();
	}
	
	public String get(String key) {
		return properties.getProperty(key);
	}
	
	public int getInt(String key) {
		return Integer.parseInt(properties.getProperty(key)); 
	}

	public boolean getBoolean(String key) {
		return Boolean.parseBoolean(properties.getProperty(key));
	}

	public String toString() {
		String s = name;
		return s;
	}
	
}
