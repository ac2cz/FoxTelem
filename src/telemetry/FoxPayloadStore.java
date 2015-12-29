package telemetry;

import java.io.IOException;
import java.util.ArrayList;

import measure.Measurement;
import measure.PassMeasurement;
import measure.RtMeasurement;
import measure.SatMeasurementStore;
import telemServer.StpFileProcessException;
import common.Config;
import common.Log;
import common.Spacecraft;

public abstract class FoxPayloadStore implements Runnable {

	public abstract boolean hasQueuedFrames();

	public abstract boolean hasQueuedMeasurements();

	public abstract void setUpdatedAll();
	public abstract void setUpdatedAll(int id);
	
	public abstract boolean getUpdatedRt(int id);
	
	public abstract void setUpdatedRt(int id, boolean u);
	
	public abstract boolean getUpdatedMax(int id);
	public abstract void setUpdatedMax(int id, boolean u);

	public abstract boolean getUpdatedMin(int id);
	public abstract void setUpdatedMin(int id, boolean u);
	public abstract boolean getUpdatedRad(int id);
	public abstract void setUpdatedRad(int id, boolean u);
	public abstract boolean getUpdatedCamera(int id);
	public abstract void setUpdatedCamera(int id, boolean u);
	public abstract boolean getUpdatedMeasurement(int id);
	public abstract void setUpdatedMeasurement(int id, boolean u);
	public abstract boolean getUpdatedPassMeasurement(int id);
	public abstract void setUpdatedPassMeasurement(int id, boolean u);
	public abstract boolean getUpdatedHerci(int id);
	public abstract void setUpdatedHerci(int id, boolean u);
	public abstract boolean getUpdatedHerciHeader(int id);
	public abstract void setUpdatedHerciHeader(int id, boolean u);
	
	public abstract int getTotalNumberOfFrames();
	public abstract int getTotalNumberOfTelemFrames();
	public abstract int getTotalNumberOfRadFrames();

	public abstract int getNumberOfFrames(int id);
	public abstract int getNumberOfTelemFrames(int id);
	public abstract int getNumberOfRadFrames(int id);
	public abstract int getNumberOfHerciFrames(int id);
	
	public abstract int getNumberOfPictureCounters(int id);
	public abstract SortedJpegList getJpegIndex(int id);
	
	public abstract boolean add(int id, long uptime, int resets, FramePart f);
	
	public abstract boolean addToFile(int id, long uptime, int resets, FramePart f);

	/**
	 * Add an array of payloads, usually when we have a set of radiation data from the high speed
	 * @param f
	 * @return
	 */
	public abstract boolean add(int id, long uptime, int resets, PayloadRadExpData[] f);
	public abstract boolean add(int id, long uptime, int resets, PayloadHERCIhighSpeed[] herci); 
	
	public abstract boolean addToFile(int id, long uptime, int resets, PayloadRadExpData[] f) throws IOException;

	/**
	 * Add a camera payload.  This is added to the picture line store one line at a time.  We do not store the actual
	 * camera payloads as there is no additional information that we need beyond the lines.  The raw frame are sent to the server
	 * @param id
	 * @param uptime
	 * @param resets
	 * @param f
	 * @return
	 */
	public abstract boolean addToPictureFile(int id, long uptime, int resets, PayloadCameraData f);
	public abstract boolean add(int id, RtMeasurement m);

	public abstract boolean addToFile(int id, Measurement m);
	
	public abstract RtMeasurement getLatestMeasurement(int id);

	public abstract boolean add(int id, PassMeasurement m);
	public abstract boolean addStpHeader(Frame f); // add the stp header records to the db
	public abstract boolean updateStpHeader(Frame f) throws StpFileProcessException; // add the stp header records to the db
	
	public abstract PassMeasurement getLatestPassMeasurement(int id);

	public abstract PayloadRtValues getLatestRt(int id);

	public abstract PayloadMaxValues getLatestMax(int id);

	public abstract PayloadMinValues getLatestMin(int id);
	public abstract PayloadRadExpData getLatestRad(int id);
	public abstract RadiationTelemetry getLatestRadTelem(int id);
	public abstract PayloadHERCIhighSpeed getLatestHerci(int id);
	public abstract HerciHighspeedHeader getLatestHerciHeader(int id);
	/**
	 * Try to return an array with "period" entries for this attribute, starting with the most 
	 * recent
	 * 
	 * @param name
	 * @param period
	 * @return
	 */
	public abstract double[][] getRtGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime);

	public abstract double[][] getMaxGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime);

	public abstract double[][] getMinGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime);

	/**
	 * Return an array of radiation data with "period" entries for this sat id and from the given reset and
	 * uptime.
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 */
	public abstract String[][] getRadData(int period, int id, int fromReset, long fromUptime);

	public abstract String[][] getRadTelemData(int period, int id, int fromReset, long fromUptime);
	public abstract double[][] getRadTelemGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime);
	public abstract double[][] getHerciScienceHeaderGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime);
	public abstract String[][] getHerciPacketData(int period, int id, int fromReset, long fromUptime);
	public abstract double[][] getMeasurementGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime);

	
	public abstract String getRtUTCFromUptime(int id, int reset, long uptime);
	
	
	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public abstract void deleteAll();	
	
	public abstract void initRad2();
}
