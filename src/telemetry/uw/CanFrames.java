package telemetry.uw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import common.Log;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2018 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * This class holds the layout for the telemetry for a given satelite.  It is loaded from a CSV file at program start
 * 
 * The layout will not change during the life of the program for a given satellite, so no provision is added for version control
 * or loading old formats.
 * 
 *
 */
public class CanFrames {
	public int NUMBER_OF_FIELDS = 0;
	public static int ERROR_POSITION = -1;

	public String fileName;
	public String name; // the name, which is stored in the spacecraft file and used to index the layouts

	public static final String NONE = "NONE";

	public String[] frame = null;  // name of the can packet frame
	public int[] priority = null; // the can packet priority
	public int[] canId = null; // the can id in decimal
	public int[] dataLength = null; // the number of bytes this frame should have
	public String[] groundClass = null;
	public String[] senders = null; // the sending component
	public String[] description = null;
	public String[] notes = null;

	/**
	 * Create a layout and load it from the file with the given path
	 * @param f
	 * @throws FileNotFoundException
	 * @throws LayoutLoadException 
	 */
	public CanFrames(String f) throws FileNotFoundException, LayoutLoadException {
		load(f);
	}

	public String getNameByCanId(int id) {
		int pos = ERROR_POSITION;
		for (int i=0; i < canId.length; i++) {
			if (id == canId[i])
				return frame[i];
		}
		return null;
	}

	public String getGroundByCanId(int id) {
		int pos = ERROR_POSITION;
		for (int i=0; i < canId.length; i++) {
			if (id == canId[i])
				return groundClass[i];
		}
		return null;
	}
	
	public String getSenderByCanId(int id) {
		int pos = ERROR_POSITION;
		for (int i=0; i < canId.length; i++) {
			if (id == canId[i])
				return senders[i];
		}
		return null;
	}


	protected void load(String f) throws FileNotFoundException, LayoutLoadException {

		String line;
		fileName = "spacecraft" +File.separator + f;
		//	File aFile = new File(fileName);

		Log.println("Loading Can Frames: "+ fileName);
		ArrayList<String[]> lines = new ArrayList<String[]>();
		BufferedReader dis = new BufferedReader(new FileReader(fileName));
		int field=0;

		try {
			line = dis.readLine();
			while ((line = dis.readLine()) != null) {
				String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				if (values.length == 12 && !values[11].contains("descope")) {
					NUMBER_OF_FIELDS++;
					lines.add(values);
				}
			}

			frame = new String[NUMBER_OF_FIELDS];		
			priority = new int[NUMBER_OF_FIELDS];
			canId = new int[NUMBER_OF_FIELDS];
			dataLength = new int[NUMBER_OF_FIELDS];
			senders = new String[NUMBER_OF_FIELDS];
			groundClass = new String[NUMBER_OF_FIELDS];
			description = new String[NUMBER_OF_FIELDS];
			notes = new String[NUMBER_OF_FIELDS];
			dis.close();
		} catch (IOException e) {
			Log.errorDialog("File Read Error", "In Can Frames " + e.getMessage());
			e.printStackTrace(Log.getWriter());
		} // consume the header
		try {
			field = 0;
			for (field = 0; field < NUMBER_OF_FIELDS; field++) {
				String[] values = lines.get(field);
				if (values != null) {
					frame[field] = values[0];
					priority[field] = Integer.valueOf(values[1]).intValue();
					String id = values[2];
					String id_hex = values[3];
					String msp_cat = values[4];
					groundClass[field] = values[5];
					canId[field] = Integer.valueOf(values[6]).intValue();
					String canid_hex = values[7];
					dataLength[field] = Integer.valueOf(values[8]).intValue();
					senders[field] = values[9];
					try {
						description[field] = values[10];
					} catch (NoSuchElementException e) {
						// ignore no description
					}
					try {
						notes[field] = values[11];
					} catch (NoSuchElementException e) {
						// ignore no description
					}
				}
			}
			

		} catch (NumberFormatException n) {
			Log.errorDialog("NUMBER FORMAT EXCEPTION", "In Can Frames at field: "+ frame[field] + " - " + n.getMessage());
			n.printStackTrace(Log.getWriter());
		} catch (IndexOutOfBoundsException n) {
			Log.errorDialog("INDEX EXCEPTION", "Error loading Can Frames at Index: " + n.getMessage());
			n.printStackTrace(Log.getWriter());
		} catch (NoSuchElementException n) {
			Log.errorDialog("Missing Field in Can Frames File", "Halted loading at field: \"+ field + " + fileName);
			n.printStackTrace(Log.getWriter());
		} catch (NullPointerException nf) {
			nf.printStackTrace(Log.getWriter());
			Log.errorDialog("ERROR", "Missing data value at field: "+ field + " - " + nf.getMessage() + "\nwhen processing Can Frames file: "  + fileName );		
			nf.printStackTrace(Log.getWriter());
		}
		
	}	
}
