package telemetry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class TelemFormat {
	public static final String NAME="name";
	public static final String MODE="mode";
	public static final String BPS="bps";
	public static final String FRAME_LENGTH="frame_length";
	public static final String DATA_LENGTH="data_length";
	public static final String HEADER_LENGTH="header_length";
	public static final String HEADER_LAYOUT_FILE="header_layout_file";
	public static final String TRAILER_LENGTH="trailer_length";
	public static final String RS_WORDS="rs_words";
	public static final String RS_PADDING="rs_padding";
	public static final String SYNC_WORD_LENGTH="sync_word_length";
	public static final String WORD_LENGTH="word_length";
	
	public Properties properties; // Java properties file for user defined values
	public File propertiesFile;
	String fileName;
	public String name;
	BitArrayLayout headerLayout;
	
	public TelemFormat(String fileName) throws LayoutLoadException {
		properties = new Properties();
		this.fileName = fileName;
		propertiesFile = new File(fileName);
		load();
		
		// TODO, should confirm that whole format is valid here
		try { 
			name = get(NAME); 
			//int i = name.length(); 
		} catch (Exception e) {
			throw new LayoutLoadException("Invalid or missing "+NAME+" in Telem Format file: " + propertiesFile.getAbsolutePath());
		}
		try { 
			String headerLayoutFilename = get(HEADER_LAYOUT_FILE);
			if (headerLayoutFilename != null) {
				headerLayout = new BitArrayLayout(headerLayoutFilename);
			} else
				headerLayout = new BitArrayLayout();
		} catch (Exception e) {
			throw new LayoutLoadException("Invalid or missing "+HEADER_LAYOUT_FILE+" in Telem Format file: " + propertiesFile.getAbsolutePath());
		}
		try { getInt(RS_WORDS); } catch (Exception e) {
			throw new LayoutLoadException("Invalid or missing "+RS_WORDS+" in Telem Format file: " + propertiesFile.getAbsolutePath());
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
			throw new LayoutLoadException("Could not load TelemFormat file: " + propertiesFile.getAbsolutePath()+"\n" + e.getMessage());
		}
	}
	
	/**
	 * Calculate the distance between the start of two sync words
	 * @return
	 */
	public int getSyncWordDistance() {
		int syncWordLength = getInt(SYNC_WORD_LENGTH);
		int wordLength = getInt(WORD_LENGTH);
		int frameLength = getInt(FRAME_LENGTH);
		return frameLength * wordLength + syncWordLength;
	}
	
	public BitArrayLayout getHeaderLayout() { 
		return headerLayout; }
	
	public int[] getPaddingArray() {
		String rs_padding = get(RS_PADDING);
		String[] pads = rs_padding.split(",");
		int[] sourceRsPadding = new int[pads.length];
		int j = 0;
		for (String p : pads)
			sourceRsPadding[j++] = Integer.parseInt(p);
		return sourceRsPadding;
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
		String s = "";
		s = s + name;
		return s;
	}

}
