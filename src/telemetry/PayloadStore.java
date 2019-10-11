package telemetry;

import gui.MainWindow;
import gui.ProgressPanel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import measure.Measurement;
import measure.PassMeasurement;
import measure.RtMeasurement;
import measure.SatMeasurementStore;
import measure.SortedMeasurementArrayList;
import common.Config;
import common.Log;
import common.Spacecraft;
import common.FoxSpacecraft;

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
 * This stores the payloads for all of the satellites.  The class calling the methods does not need to know
 * how the data is stored.  The data could be moved into an SQL database in the future and it should make no 
 * difference to code outside of this class (THIS IS NOT STRICTLY TRUE CURRENTLY)
 * 
 *
 */
public class PayloadStore extends FoxPayloadStore implements Runnable {
	public static final String DB_NAME = "FOXDB";
	public static final String DB_VERSION = "1.00";
	public static final int DATA_COL = 0;
	public static final int UPTIME_COL = 1;
	public static final int RESETS_COL = 2;
	public static final int LAT_COL = 3;
	public static final int LON_COL = 4;
	private boolean running = true;
	
	@SuppressWarnings("unused")
	private boolean done = false;
	
	private SortedFramePartArrayList payloadQueue;
	private SortedMeasurementArrayList measurementQueue;

	private final int INITIAL_QUEUE_SIZE = 1000;
	
	SatPayloadStore[] payloadStore;
	SatPictureStore[] pictureStore;
	SatMeasurementStore[] measurementStore;
	
	public PayloadStore() {
		payloadQueue = new SortedFramePartArrayList(INITIAL_QUEUE_SIZE);
		measurementQueue = new SortedMeasurementArrayList(INITIAL_QUEUE_SIZE);
		ArrayList<Spacecraft> sats = Config.satManager.getSpacecraftList();
		payloadStore = new SatPayloadStore[sats.size()];
		pictureStore = new SatPictureStore[sats.size()];
		measurementStore = new SatMeasurementStore[sats.size()];
		
		String dir = "";
        if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			dir = Config.logFileDirectory + File.separator ;
			//System.err.println("Loading: "+log);
		}
        String loadMessage = "FoxTelem: Loading logged data, please wait ...";
        
		boolean newDB = makeDir(dir + DB_NAME);
		if (newDB) {
			// Check to see if the Fox 1 Real Time file is present
			String testFile = "Fox1"+"rtTelemetry"+".log"; // name is now hardcoded because it is the LEGACY name and no longer used
			if (!Config.logFileDirectory.equalsIgnoreCase("")) {
				testFile = Config.logFileDirectory + File.separator + testFile;
			}
			File aFile = new File(testFile);
			if(aFile.exists()){
				Log.infoDialog("Database Conversion", "You have pre version 1.03 payload log files.  These will be converted to the new 1.03 database format.\n"
						+ "This process may take a few minutes to load the data and convert it\n");
				loadMessage = "FoxTelem: Database conversion in progress, please wait ...";
			} else {
				newDB = false; // no need to convert
			}
			
		}
		Config.fileProgress = new ProgressPanel(MainWindow.frame, loadMessage, false);
		Config.fileProgress.setVisible(true);
		
		for (int s=0; s<sats.size(); s++) {
			
				payloadStore[s] = new SatPayloadStore(sats.get(s).foxId);
				if (newDB) {
					// convert any legacy data
					try {
						payloadStore[s].convert();
					} catch (IOException e) {
						Log.errorDialog("ERROR", "Could not convert the old FoxTelem payload files to the new format: " +  
								"\nAny old payloads will not be available\n");
						e.printStackTrace(Log.getWriter());
					}
				}
				if (sats.get(s).isFox1())
					if (((FoxSpacecraft)sats.get(s)).hasCamera()) pictureStore[s] = new SatPictureStore(sats.get(s).foxId);;
				measurementStore[s] = new SatMeasurementStore(sats.get(s).foxId);
				Config.fileProgress.updateProgress(100 * s / sats.size());
			
		}
		loaded = true;
	}
	
	/**
	 * Make the database directory if needed.  Check to see if we have existing legacy data and run the conversion if we do
	 * @param dir
	 */
	private boolean makeDir(String dir) {
		
		File aFile = new File(dir);
		if(!aFile.isDirectory()){
			Log.println("Making new database: " + dir);
			aFile.mkdir();
			if(!aFile.isDirectory()){
				Log.errorDialog("ERROR", "ERROR can't create the directory: " + aFile.getAbsolutePath() +  
						"\nAny decoded payloads will not be saved to disk\n");
				Config.storePayloads = false;
				return false;
			}
			return true;
		}
		
		return false;
	}
	
	
	
	public boolean hasQueuedFrames() {
		if (payloadQueue.size() > 0) return true;
		return false;
	}

	public boolean hasQueuedMeasurements() {
		if (measurementQueue.size() > 0) return true;
		return false;
	}

	private SatPayloadStore getPayloadStoreById(int id) {
		for (SatPayloadStore store : payloadStore)
			if (store != null)
				if (store.foxId == id) return store;
		return null;
	}
	private SatPictureStore getPictureStoreById(int id) {
		for (SatPictureStore store : pictureStore)
			if (store != null)
				if (store.foxId == id) return store;
		return null;
	}
	private SatMeasurementStore getMeasurementStoreById(int id) {
		for (SatMeasurementStore store : measurementStore)
			if (store != null)
				if (store.foxId == id) return store;
		return null;
	}

	public void setUpdatedAll() {
		for (SatPayloadStore store : payloadStore)
		if (store != null)
			store.setUpdatedAll();
		for (SatPictureStore store : pictureStore)
		if (store != null)
			store.setUpdatedAll();
		for (SatMeasurementStore store : measurementStore)
		if (store != null)
			store.setUpdatedAll();

	}

	public void setUpdatedAll(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedAll();
		SatPictureStore picstore = getPictureStoreById(id);
		if (picstore != null)
			picstore.setUpdatedAll();
		SatMeasurementStore mStore = getMeasurementStoreById(id);
		if (mStore != null)
			mStore.setUpdatedAll();
	}
	
	public boolean getUpdated(int id, String lay) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdated(lay);
		return false;
	}
	
	public void setUpdated(int id, String lay, boolean u) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdated(lay, u);
	}
	/*
	public boolean getUpdatedMax(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdatedMax();
		return false;
	}
	public void setUpdatedMax(int id, boolean u) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedMax(u);
	}

	public boolean getUpdatedMin(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdatedMin();
		return false;
	}
	public void setUpdatedMin(int id, boolean u) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedMin(u);
	}
	public boolean getUpdatedRad(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdatedRad();
		return false;
	}
	public void setUpdatedRad(int id, boolean u) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedRad(u);
	}
	public boolean getUpdatedHerci(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdatedHerci();
		return false;
	}
	public void setUpdatedHerci(int id, boolean u) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedHerci(u);
	}
	public boolean getUpdatedHerciHeader(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdatedHerciHeader();
		return false;
	}
	public void setUpdatedHerciHeader(int id, boolean u) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedHerciHeader(u);
	}
	*/
	public boolean getUpdatedCamera(int id) { 
		SatPictureStore store = getPictureStoreById(id);
		if (store != null)
			return store.getUpdatedCamera();
		return false;
	}
	public void setUpdatedCamera(int id, boolean u) {
		SatPictureStore store = getPictureStoreById(id);
		if (store != null)
			store.setUpdatedCamera(u);
	}
	public boolean getUpdatedMeasurement(int id) { 
		SatMeasurementStore store = getMeasurementStoreById(id);
		if (store != null)
			return store.getUpdatedMeasurement();
		return false;
	}
	public void setUpdatedMeasurement(int id, boolean u) {
		SatMeasurementStore store = getMeasurementStoreById(id);
		if (store != null)
			store.setUpdatedMeasurement(u);
	}
	public boolean getUpdatedPassMeasurement(int id) { 
		SatMeasurementStore store = getMeasurementStoreById(id);
		if (store != null)
			return store.getUpdatedPassMeasurement();
		return false;
	}
	public void setUpdatedPassMeasurement(int id, boolean u) {
		SatMeasurementStore store = getMeasurementStoreById(id);
		if (store != null)
			store.setUpdatedPassMeasurement(u);
	}

	
	public int getTotalNumberOfFrames() {
		int total = 0;
		for (SatPayloadStore store : payloadStore)
			total += store.getNumberOfFrames();
		return total;
	}
	public int getTotalNumberOfFrames(String lay) { 
		int total = 0;
		for (SatPayloadStore store : payloadStore)
			total += store.getNumberOfFrames(lay);
		return total;
	}
	
	/*
	public int getTotalNumberOfRadFrames() { 
		int total = 0;
		for (SatPayloadStore store : payloadStore)
			total += store.getNumberOfRadFrames();
		return total;
	}
*/
	public int getNumberOfFrames(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfFrames();
		return 0;
	}
	public int getNumberOfFrames(int id, String lay) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfFrames(lay);
		return 0;
	}
	public int getNumberOfTelemFrames(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfTelemFrames();
		return 0;		
	}
	/*
	public int getNumberOfRadFrames(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfRadFrames();
		return 0;
	}
	public int getNumberOfHerciFrames(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfHerciFrames();
		return 0;
	}
	*/
	
	public int getNumberOfPictureCounters(int id) { 
		SatPictureStore store = getPictureStoreById(id);
		if (store != null)
			return store.getNumberOfPictureCounters();
		return 0;
	}

	public SortedJpegList getJpegIndex(int id, int period, int fromReset, long fromUptime) {
		SatPictureStore store = getPictureStoreById(id);
		if (store != null)
			return store.getJpegIndex(id, period, fromReset, fromUptime);
		return null;
	}
	
	public boolean add(int id, long uptime, int resets, FramePart f) {
		f.captureHeaderInfo(id, uptime, resets);
		return payloadQueue.addToEnd(f);
	}
	
	public boolean addToFile(int id, long uptime, int resets, FramePart f) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.add(id, uptime, resets, f);
			} catch (IOException e) {
				// FIXME We dont want to stop the decoder but we want to warn the user...
				e.printStackTrace(Log.getWriter());
//			} catch (Exception e) {
//				// Something bad happened
//				Log.errorDialog("ERROR", "Could not parse and save frame for sat: " + id + " reset: " + resets + " uptime: " + uptime + " type: " + f.type
//						+ "\nCheck if the correct frame layouts are being used for this spacecrat");
			}
		return false;
	}

	/**
	 * Add an array of payloads, usually when we have a set of radiation data from the high speed
	 * @param f
	 * @return
	 */
	public boolean add(int id, long uptime, int resets, PayloadRadExpData[] f) {
		for (int i=0; i< f.length; i++) {
			if (f[i].hasData()) {
				f[i].captureHeaderInfo(id, uptime, resets);
				f[i].type = 400 + i; // store the index in the type field so it is unique for high speed, but duplicates rejected if same frame processed again
				payloadQueue.addToEnd(f[i]);
			}
		}
		return true;
	}

	/**
	 * Add an array of payloads, usually when we have a set of radiation data from the high speed
	 * @param f
	 * @return
	 */
	public boolean add(int id, long uptime, int resets, PayloadHERCIhighSpeed[] herci) {
		for (int i=0; i< herci.length; i++) {
			herci[i].captureHeaderInfo(id, uptime, resets);
			herci[i].type = 600 + i;
			payloadQueue.addToEnd(herci[i]);
		}
		return true;
	}

	public boolean addToFile(int id, long uptime, int resets, PayloadRadExpData[] f) throws IOException {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.add(id, uptime, resets, f);
		return false;
		
	}

	/**
	 * Add a camera payload.  This is added to the picture line store one line at a time.  We do not store the actual
	 * camera payloads as there is no additional information that we need beyond the lines.  The raw frame are sent to the server
	 * @param id
	 * @param uptime
	 * @param resets
	 * @param f
	 * @return
	 */
	public boolean addToPictureFile(int id, long uptime, int resets, PayloadCameraData f) {
		SatPictureStore store = getPictureStoreById(id);
		if (store != null) {
			ArrayList<PictureScanLine> lines = f.pictureLines;
			for (PictureScanLine line : lines) {
				// Capture the header into the line
				line.id = id;
				line.resets = resets;
				line.uptime = uptime;
				try {
					if (!store.add(id, resets, uptime, line))
						return false;
				} catch (IOException e) {
					// FIXME We don't want to stop the decoder but we want to warn the user...
					// this probably means we did not store the camera payload or could not create the Jpeg.  Perhaps the header was missing etc
					// or the file was in use by another process, e.g. Backup
					Log.println("File Error writting CAMERA DATA, line not written: " + id + " " + resets + " " + uptime + "\n" + e.getMessage());
					e.printStackTrace(Log.getWriter());
				} catch (ArrayIndexOutOfBoundsException e) {
					// FIXME We dont want to stop the decoder but we want to warn the user...
					Log.println("CORRUPT CAMERA DATA, line not written: " + id + " " + resets + " " + uptime);
					e.printStackTrace(Log.getWriter());
				}
			}
		}
		return true;
	}

	public boolean add(int id, RtMeasurement m) {
		return measurementQueue.addToEnd(m);
	}

	public boolean addToFile(int id, Measurement m) {
		SatMeasurementStore store = getMeasurementStoreById(id);
		if (store != null)
			try {
				return store.add(id, m);
			} catch (IOException e) {
				// FIXME We dont want to stop the decoder but we want to warn the user...
				e.printStackTrace(Log.getWriter());
			}
		return false;
	}
	
	public RtMeasurement getLatestMeasurement(int id) {
		SatMeasurementStore store = getMeasurementStoreById(id);
		if (store != null)
			return store.getLatestMeasurement();
		return null;
	}

	public boolean add(int id, PassMeasurement m) {
		SatMeasurementStore store = getMeasurementStoreById(id);
		if (store != null)
			try {
				return store.add(id, m);
			} catch (IOException e) {
				// FIXME We dont want to stop the decoder but we want to warn the user...
				e.printStackTrace(Log.getWriter());
			}
		return false;
	}
	
	public PassMeasurement getLatestPassMeasurement(int id) {
		SatMeasurementStore store = getMeasurementStoreById(id);
		if (store != null)
			return store.getLatestPassMeasurement();
		return null;
	}


	@Override
	public FramePart getFramePart(int id, int reset, long uptime, String layout, boolean prev) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatest(id, reset, uptime, layout, prev);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}
	
	public FramePart getFramePart(int id, int reset, long uptime, int type, String layout, boolean prev) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatest(id, reset, uptime, type, layout, prev);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}
	
	public FramePart getLatest(int id,  String lay) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatest(lay);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}
	
	public FramePart getLatestRt(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestRt();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}

	public FramePart getLatestMax(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestMax();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}

	public FramePart getLatestMin(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestMin();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;

	}

	public FoxFramePart getLatestRad(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestRad();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;

	}

	public RadiationTelemetry getLatestRadTelem(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestRadTelem();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;

	}
	
	public RadiationTelemetry getRadTelem(int id, int resets, long uptime) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getRadTelem(id, resets, uptime);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;

	}

	
	
	public PayloadHERCIhighSpeed getLatestHerci(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestHerci();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;

	}

	public HerciHighspeedHeader getLatestHerciHeader(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestHerciHeader();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;

	}

	
	/**
	 * Try to return an array with "period" entries for this attribute, starting with the most 
	 * recent
	 * 
	 * @param name
	 * @param period
	 * @return
	 */
	public double[][] getGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime, String layout, boolean positionData, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			try {
				return store.getGraphData(name, period, fox, fromReset, fromUptime, layout, positionData, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
				Log.println("ERROR getting graph data: " + e.getMessage());
			}
		return null;
	}
	public double[][] getRtGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime, boolean positionData, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			try {
				return store.getRtGraphData(name, period, fox, fromReset, fromUptime, positionData, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}

	public double[][] getMaxGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime, boolean positionData, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			try {
				return store.getMaxGraphData(name, period, fox, fromReset, fromUptime, positionData, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;		
	}

	public double[][] getMinGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime, boolean positionData, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			try {
				return store.getMinGraphData(name, period, fox, fromReset, fromUptime, positionData, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;		
	}

	/**
	 * Return an array of real time data with "period" entries for this sat id and from the given reset and
	 * uptime.
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 */
	public String[][] getRtData(int period, int id, int fromReset, long fromUptime, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getRtData(period, id, fromReset, fromUptime, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}

	/**
	 * Return an array of WOD data with "period" entries for this sat id and from the given reset and
	 * uptime.
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 */
	public String[][] getWODData(int period, int id, int fromReset, long fromUptime, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getWODData(period, id, fromReset, fromUptime, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}

	/**
	 * Return an array of radiation data with "period" entries for this sat id and from the given reset and
	 * uptime.
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 */
	@Override
	public String[][] getTableData(int period, int id, int fromReset, long fromUptime, boolean reverse, String layout)  {
		return getTableData(period, id, fromReset, fromUptime, false, reverse, layout);
	}
	
	public String[][] getTableData(int period, int id, int fromReset, long fromUptime, boolean returnType, boolean reverse, String layout)  {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getTableData(period, id, fromReset, fromUptime, returnType, reverse, layout);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}

	
	public String[][] getRadData(int period, int id, int fromReset, long fromUptime, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getRadData(period, id, fromReset, fromUptime, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}
	
	public String[][] getWODRadData(int period, int id, int fromReset, long fromUptime, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getWODRadData(period, id, fromReset, fromUptime, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}
	
	@Override
	public String[][] getWodRadTelemData(int period, int id, int fromReset, long fromUptime, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getWodRadTelemData(period, id, fromReset, fromUptime, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	} 
	
	public String[][] getRadTelemData(int period, int id, int fromReset, long fromUptime, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getRadTelemData(period, id, fromReset, fromUptime, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}
	public String[][] getHerciPacketData(int period, int id, int fromReset, long fromUptime, boolean type, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getHerciPacketData(period, id, fromReset, fromUptime, type, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}
	public String[][] getHerciHsData(int period, int id, int fromReset, long fromUptime, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getHerciHsData(period, id, fromReset, fromUptime, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}
	public double[][] getRadTelemGraphData(String name, int period, FoxSpacecraft fox, int fromReset, long fromUptime, boolean positionData, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			try {
				return store.getRadTelemGraphData(name, period, fox, fromReset, fromUptime, positionData, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}
	public double[][] getHerciScienceHeaderGraphData(String name, int period, FoxSpacecraft fox, int fromReset, long fromUptime, boolean positionData, boolean reverse) {
		SatPayloadStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			try {
				return store.getHerciScienceHeaderGraphData(name, period, fox, fromReset, fromUptime, positionData, reverse);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}
	public double[][] getMeasurementGraphData(String name, int period, FoxSpacecraft fox, int fromReset, long fromUptime, boolean reverse) {
		SatMeasurementStore store = getMeasurementStoreById(fox.foxId);
		if (store != null)
			return store.getMeasurementGraphData(name, period, fox, fromReset, fromUptime, reverse);
		return null;
	}
	
	
	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public void deleteAll() {
		loaded=false;
		for (SatPayloadStore store : payloadStore)
			if (store != null)
				store.deleteAll();
		for (SatPictureStore store : pictureStore)
			if (store != null)
				store.deleteAll();
		for (SatMeasurementStore store : measurementStore)
			if (store != null)
				store.deleteAll();
		loaded=true;
	}
	
	public void offloadSegments() {
		for (SatPayloadStore store : payloadStore)
			if (store != null)
				store.offloadSegments();
//		for (SatMeasurementStore store : measurementStore)
//			if (store != null)
//				store.offloadSegments();
//		for (SatPictureStore store : pictureStore)
//			if (store != null)
//				store.offloadSegments();

	}
	
	/**
	 * The run thread is for inserts, so that we minimize the load on the decoder.  We check the queue of payloads and add any that are in it
	 */
	@Override
	public void run() {

		running = true;
		done = false;
		while(running) {
			try {
				Thread.sleep(100); // check for new inserts multiple times per second
			} catch (InterruptedException e) {
				Log.println("ERROR: PayloadStore thread interrupted");
				e.printStackTrace(Log.getWriter());
			}
			offloadSegments();
			if (this.initialized()) {
				if (payloadQueue.size() > 0) {
					while (payloadQueue.size() > 0) {
						FramePart f = payloadQueue.get(0);
						if (f == null) {
							Log.println("NULL RECORD IN THE Q");
						} else {
							if (Config.debugFieldValues) {
								Log.println(f.toString() + "\n");
							}
							if (f instanceof PayloadCameraData)
								addToPictureFile(f.id, f.uptime, f.resets, (PayloadCameraData)f);
							else
								addToFile(f.id, f.uptime, f.resets, f);
						}
						payloadQueue.remove(0);
						Thread.yield(); // don't hog the thread or we block when trying to add new payloads
					}
				}
				if (measurementQueue.size() > 0) {
					while (measurementQueue.size() > 0) {
						Measurement f = measurementQueue.get(0);
						if (f == null) {
							Log.println("NULL RECORD IN THE MEASUREMENT Q");
						} else {
							if (Config.debugFieldValues)
								Log.println(f.toString() + "\n");
							addToFile(f.id, f);
							measurementQueue.remove(0);
							Thread.yield(); // don't hog the thread or we block when trying to add new payloads
						}
					}
				}
			}
		}			
		done = true;
	}

	public void initRad2() {
		
	}
	
	public void initHerciPackets() {
		
	}
	
	@Override
	public boolean addStpHeader(Frame f) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean updateStpHeader(Frame f) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getRtUTCFromUptime(int id, int reset, long uptime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean processNewImageLines() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double[][] getPassMeasurementGraphData(String name, int period, FoxSpacecraft fox, int fromReset,
			long fromUptime, boolean reverse) {
		SatMeasurementStore store = getMeasurementStoreById(fox.foxId);
		if (store != null)
			return store.getPassMeasurementGraphData(name, period, fox, fromReset, fromUptime, reverse);
		return null;
	}

	@Override
	public int getNumberOfPayloadsBetweenTimestamps(int id, int reset, long uptime, int toReset, long toUptime, String payloadType) {
		if (payloadType == Spacecraft.MEASUREMENTS || payloadType == Spacecraft.PASS_MEASUREMENTS) {
			SatMeasurementStore store = getMeasurementStoreById(id);
			if (store != null)
				return store.getNumberOfPayloadsBetweenTimestamps(id, reset, uptime, toReset, toUptime, payloadType);
		}
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfPayloadsBetweenTimestamps(id, reset, uptime, toReset, toUptime, payloadType);
		return 0;
	}

	



	

	
}
