package telemetry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import common.Log;

public class FrameLayout {
	public int frameLength;
	public int numberOfPayloads = 0;
	public int[] payloads = null;
			
	public String fileName;
	public String name; // the name, which is stored in the spacecraft file and used to index the layouts
	
	public FrameLayout(String f) throws LayoutLoadException, IOException {
		fileName = f;
		load(fileName);
	}
	
	protected void load(String f) throws LayoutLoadException, IOException {

		String line;
		fileName = "spacecraft" +File.separator + f;
	//	File aFile = new File(fileName);
		
		Log.println("Loading frame layout: "+ fileName);
		BufferedReader dis = new BufferedReader(new FileReader(fileName));
		frameLength = loadIntValue(dis);
		syncLength = loadIntValue(dis);
		syncType = loadIntValue(dis);
				
	}
	
	private int loadIntValue(BufferedReader dis) throws IOException {
		String line = dis.readLine();
		StringTokenizer header = new StringTokenizer(line, ",");
		Log.print(header.nextToken() + ": ");
		int value = Integer.valueOf(header.nextToken()).intValue();
		Log.println(""+value);
		return value;
	}
}
