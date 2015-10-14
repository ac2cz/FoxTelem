package telemetry;

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
public class PayloadStore implements Runnable {
	public static final int DATA_COL = 0;
	public static final int UPTIME_COL = 1;
	public static final int RESETS_COL = 2;
	public static final int UTC_COL = 3;
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
		
		for (int s=0; s<sats.size(); s++) {
			payloadStore[s] = new SatPayloadStore(sats.get(s).foxId);
			if (sats.get(s).hasCamera()) pictureStore[s] = new SatPictureStore(sats.get(s).foxId);;
			measurementStore[s] = new SatMeasurementStore(sats.get(s).foxId);
		}
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
	
	public boolean getUpdatedRt(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdatedRt();
		return false;
	}
	
	public void setUpdatedRt(int id, boolean u) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedRt(u);
	}
	
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
	public int getTotalNumberOfTelemFrames() { 
		int total = 0;
		for (SatPayloadStore store : payloadStore)
			total += store.getNumberOfTelemFrames();
		return total;
	}
	public int getTotalNumberOfRadFrames() { 
		int total = 0;
		for (SatPayloadStore store : payloadStore)
			total += store.getNumberOfRadFrames();
		return total;
	}

	public int getNumberOfFrames(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfFrames();
		return 0;
	}
	public int getNumberOfTelemFrames(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfTelemFrames();
		return 0;
	}
	public int getNumberOfRadFrames(int id) { 
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfRadFrames();
		return 0;
	}
	
	
	public int getNumberOfPictureCounters(int id) { 
		SatPictureStore store = getPictureStoreById(id);
		if (store != null)
			return store.getNumberOfPictureCounters();
		return 0;
	}

	public SortedJpegList getJpegIndex(int id) {
		SatPictureStore store = getPictureStoreById(id);
		if (store != null)
			return store.jpegIndex;
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

	public PayloadRtValues getLatestRt(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getLatestRt();
		return null;
	}

	public PayloadMaxValues getLatestMax(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getLatestMax();
		return null;
	}

	public PayloadMinValues getLatestMin(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getLatestMin();
		return null;

	}

	public PayloadRadExpData getLatestRad(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getLatestRad();
		return null;

	}

	public RadiationTelemetry getLatestRadTelem(int id) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getLatestRadTelem();
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
	public double[][] getRtGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime) {
		SatPayloadStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			return store.getRtGraphData(name, period, fox, fromReset, fromUptime);
		return null;
	}

	public double[][] getMaxGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime) {
		SatPayloadStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			return store.getMaxGraphData(name, period, fox, fromReset, fromUptime);
		return null;		
	}

	public double[][] getMinGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime) {
		SatPayloadStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			return store.getMinGraphData(name, period, fox, fromReset, fromUptime);
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
	public String[][] getRadData(int period, int id, int fromReset, long fromUptime) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getRadData(period, id, fromReset, fromUptime);
		return null;
	}

	public String[][] getRadTelemData(int period, int id, int fromReset, long fromUptime) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getRadTelemData(period, id, fromReset, fromUptime);
		return null;
	}
	public double[][] getRadTelemGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime) {
		SatPayloadStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			return store.getRadTelemGraphData(name, period, fox, fromReset, fromUptime);
		return null;
	}

	public double[][] getMeasurementGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime) {
		SatMeasurementStore store = getMeasurementStoreById(fox.foxId);
		if (store != null)
			return store.getMeasurementGraphData(name, period, fox, fromReset, fromUptime);
		return null;
	}

	
	public String getRtUTCFromUptime(int id, int reset, long uptime) {
		SatPayloadStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getRtUTCFromUptime(reset, uptime);
		return null;
	}
	
	
	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public void deleteAll() {
		for (SatPayloadStore store : payloadStore)
			if (store != null)
				store.deleteAll();
		for (SatPictureStore store : pictureStore)
			if (store != null)
				store.deleteAll();
		for (SatMeasurementStore store : measurementStore)
			if (store != null)
				store.deleteAll();

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
			if (payloadQueue.size() > 0) {
				while (payloadQueue.size() > 0) {
					FramePart f = payloadQueue.get(0);
					if (f == null) {
						Log.println("NULL RECORD IN THE Q");
					} else {
						if (Config.debugFieldValues)
							Log.println(f.toString() + "\n");
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
		done = true;
	}
}
