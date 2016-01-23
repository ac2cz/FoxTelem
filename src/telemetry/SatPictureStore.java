
package telemetry;

import gui.MainWindow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import common.Config;
import common.Log;
import common.Spacecraft;

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
 * A store of picture lines for a single satellite.
 * 
 *
 */
public class SatPictureStore {

	private static final int INIT_SIZE = 100;
	@SuppressWarnings("unused")
	private Spacecraft fox;
	public int foxId;
	public static String JPG_INDEX_NAME = "jpg_index.dat";
	private String fileName = JPG_INDEX_NAME;
	boolean updatedCamera = false;		
	SortedJpegList jpegIndex;
	
	/**
	 * Create the payload store this this fox id
	 * @param id
	 */
	public SatPictureStore(int id) {
		foxId = id;
		fox = Config.satManager.getSpacecraft(id);
		initPayloadFiles();
	}

	private void initPayloadFiles() {
		// Check for and create the images directory if it does not exist
		String dir = CameraJpeg.IMAGES_DIR;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			dir = Config.logFileDirectory + File.separator + dir;

		}
		File aFile = new File(dir);
		if(!aFile.isDirectory()){
				aFile.mkdir();
				Log.println("Making directory: " + dir);
		}
		if(!aFile.isDirectory()){
			Log.errorDialog("ERROR", "ERROR can't create the directory: " + aFile.getAbsolutePath() +  
					"\nAny decoded pictures will not be saved to disk. Downloaded picture lines\n"
					+ "can still be decoded and uploaded to the server.");
		}
		jpegIndex = new SortedJpegList(INIT_SIZE);
		fileName = CameraJpeg.IMAGES_DIR + File.separator + "Fox"+foxId+JPG_INDEX_NAME;
		try {
			load(fileName);
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					e.toString(),
					"ERROR Loading Stored Jpeg Index data",
					JOptionPane.ERROR_MESSAGE) ;
			e.printStackTrace(Log.getWriter());
		}
		
		//Confirm that we have the jpeg header
		String header = CameraJpeg.JPG_HEADER;
		
		File hFile = new File(header);
		if(!hFile.exists()){
			Log.errorDialog("ERROR", "ERROR can't find the file: " + hFile.getAbsolutePath() +  
					"\nWithout the JPEG header information downloaded picture lines can not be converted into a viewable image.\n"
						+ "They can still be decoded and uploaded to the server.");
			
		}
		
	}
	public void setUpdatedAll() {
		updatedCamera = true;
	}

	public boolean getUpdatedCamera() { return updatedCamera; }
	public void setUpdatedCamera(boolean u) {
		updatedCamera = u;
	}

//	public int getNumberOfPictureLines() { return cameraRecords.size(); }
	public int getNumberOfPictureCounters() { return jpegIndex.size(); }

	/**
	 * Add a picture line.  First check to see if we have a suitable file to add it to, if not it is created.  Then append to the file
	 * 
	 * @param f
	 * @return
	 * @throws IOException 
	 */
	public boolean add(int id, int resets, long uptime,  PictureScanLine line) throws IOException {
		for (CameraJpeg jpg : jpegIndex) {
			if (jpg.isSameFile(id, resets, uptime, line.pictureCounter)) {
				jpg.addLine(line);
				updatedCamera = true;
				return true;
			}
		}
		// We did not find it, so create a new Jpeg on disk.  This will also copy the header and add the first line
		CameraJpeg jpg = new CameraJpeg(id, resets, uptime, uptime, line.pictureCounter, line);
		jpegIndex.add(jpg);
		updatedCamera = true;
		save(jpg, fileName, true);
		return true;
	}
	
	public SortedJpegList getJpegIndex(int id, int period, int fromReset, long fromUptime) {
		
		int start = 0;
		int end = 0;
		if (fromReset == 0 && fromUptime == 0) { // then we take records nearest the end
			start = jpegIndex.size()-period;
			end = jpegIndex.size();
		} else {
			// we need to find the start point
			start = jpegIndex.getNearestFrameIndex(fox.foxId, fromUptime, fromReset);
			if (start == -1 ) return null;
			end = start + period;
		}
		if (end > jpegIndex.size()) end = jpegIndex.size();
		if (end < start) end = start;
		if (start < 0) start = 0;
		if (start > jpegIndex.size()) start = jpegIndex.size();
		SortedJpegList results = new SortedJpegList(end-start);

		
		//int j = end-start-1;
		for (int i=end-1; i>= start; i--) {
			//System.out.println(rtRecords.size());
			results.add(jpegIndex.get(i));
		}
		
		return results;
	}
	
	/**
	 * Load the jpeg index from disk
	 * @param log
	 * @throws FileNotFoundException
	 */
	public void load(String log) throws FileNotFoundException {
		String line;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			log = Config.logFileDirectory + File.separator + log;
			Log.println("Loading: " + log);
		}
		File aFile = new File(log );
		if(!aFile.exists()){
			try {
				aFile.createNewFile();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(MainWindow.frame,
						e.toString(),
						"ERROR creating jpeg index file " + log,
						JOptionPane.ERROR_MESSAGE) ;
				e.printStackTrace(Log.getWriter());
			}
		}
		boolean deleted = false;
		BufferedReader dis = new BufferedReader(new FileReader(log));

		try {
			while ((line = dis.readLine()) != null) {
				if (line != null) {
					StringTokenizer st = new StringTokenizer(line, ",");
					String date = st.nextToken();
					int id = Integer.valueOf(st.nextToken()).intValue();
					int resets = Integer.valueOf(st.nextToken()).intValue();
					long fromUptime = Long.valueOf(st.nextToken()).longValue();
					long toUptime = Long.valueOf(st.nextToken()).longValue();
					int pictureCounter = Integer.valueOf(st.nextToken()).intValue();
					String name = st.nextToken();
					
					// We should never get this situation, but good to check..
					if (Config.satManager.getSpacecraft(id) == null) {
						Log.errorDialog("FATAL", "Attempting to Load payloads from the Camera Line store for satellite with Fox Id: " + id 
								+ "\n when no sattellite with that FoxId is configured.  Add this spacecraft to the satellite directory and restart FoxTelem."
								+ "\nProgram will now exit");
						System.exit(1);
					}
					CameraJpeg jpg = new CameraJpeg(id, resets, fromUptime, toUptime, pictureCounter, date, name);
					// Confirm this file is still on disk
					if (jpg.fileExists()) {
						jpegIndex.add(jpg);
						updatedCamera = true;
					} else {
						deleted = true;
					}
					
				}
			}
			dis.close();
			if (deleted) {
				// rewrite the output file
				if (jpegIndex.size() > 0) {
					save(jpegIndex.get(0), log, false); // overwrite the file
					for (int i=1; i<jpegIndex.size(); i++)
						save(jpegIndex.get(i), log, true); // append the other records
				} else {
					// all of the files must have been manually removed
					// so we do nothing
				}
			}
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());

		} catch (NumberFormatException n) {
			n.printStackTrace(Log.getWriter());
		} catch (NoSuchElementException n) {
			Log.println("ERROR loading record from camera picture line store");
			n.printStackTrace(Log.getWriter());
		}


	}

	/**
	 * Save an index line to the log file
	 * @param pictureScanLine
	 * @param log
	 * @throws IOException
	 */
	public void save(CameraJpeg frame, String log, boolean append) throws IOException {
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			log = Config.logFileDirectory + File.separator + log;
		} 
		File aFile = new File(log);
		if(!aFile.exists()) {
			aFile.createNewFile();
		}
		//Log.println("Saving: " + log);
		//use buffering and append to the existing file
		Writer output = new BufferedWriter(new FileWriter(aFile, append));
		try {
			output.write( frame.toString() + "\n" );
			output.flush();
		} finally {
			// Make sure it is closed even if we hit an error
			output.flush();
			output.close();
		}

	}

	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public void deleteAll() {
		String log = fileName;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			log = Config.logFileDirectory + File.separator + log;
			//Log.println("Rremoving: " + log);
		}
		try {
			for (int i=0; i<jpegIndex.size(); i++) {	
				if (jpegIndex.get(i).fileExists()) {
					SatPayloadStore.remove(jpegIndex.get(i).fileName);
					SatPayloadStore.remove(jpegIndex.get(i).fileName+".tn");
				}
			}
			SatPayloadStore.remove(log);
			initPayloadFiles();
			setUpdatedAll();
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					ex.toString(),
					"Error Deleting JPeg Index Files for FoxId:"+foxId+", check permissions on file: \n" + log,
					JOptionPane.ERROR_MESSAGE) ;
		}

	}



}
