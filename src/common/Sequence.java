package common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
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
 * Class that manages the sequence number that is stamped on each frame before it is sent to the server.  We save the sequence
 * number to a file in the log directory.  The number stored is the last sequence number that we used.
 * 
 * We have one sequence number for the decoder and it is incremented every time a frame is sent even if we are collecting frames
 * for multiple satellites.
 * 
 * 
 */
public class Sequence {
	
	public static final long ERROR_NUMBER = -1;
	private static long number = ERROR_NUMBER;
	private static final String SEQ_FILE_NAME = "seq.dat";
	
	public Sequence() throws IOException {
		load();
	}
	
	private void load() throws IOException {
		String log = SEQ_FILE_NAME;
		if (!Config.homeDirectory.equalsIgnoreCase("")) {
			log = Config.homeDirectory + File.separator + log;
			Log.println("Loading Sequence from: " + log);
		}
		File aFile = new File(log );
		if(!aFile.exists()){
			aFile.createNewFile();
			number = 0;
			save();
		} else {

			FileInputStream dis = new FileInputStream(log);
			BufferedReader reader = new BufferedReader(new InputStreamReader(dis));
			try {
				String line = reader.readLine();
				try {
					number = Long.parseLong(line);
				} catch (NumberFormatException e) {
					number = 0;
					save();
				}
			} finally {
				reader.close();
			}
		}
	}
	
	public long getNextSequence() throws IOException {
		if (number == ERROR_NUMBER) {
			load();
		}
		number = number + 1;
		save();
		return number;
	}
	
	private void save() throws IOException {
		String log = SEQ_FILE_NAME;
		if (!Config.homeDirectory.equalsIgnoreCase("")) {
			log = Config.homeDirectory + File.separator + log;
			//Log.println("Saving Sequence to: " + log);
		}
		
		FileOutputStream dis = new FileOutputStream(log);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dis));
		try {
			writer.write(Long.toString(number));
		} finally {
			writer.flush();
			writer.close();
		}
	}
}
