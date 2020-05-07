package telemetry;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import measure.Measurement;
import measure.PassMeasurement;
import measure.RtMeasurement;
import telemServer.StpFileProcessException;
import common.FoxSpacecraft;
import common.Spacecraft;

public abstract class FoxPayloadStore implements Runnable {
	protected boolean loaded = false; // set this to true once we have completed initial start up to prevent graphs reading null data
	
	public boolean initialized() { return loaded; }
	public abstract boolean hasQueuedFrames();
	public abstract int getQueuedFramesSize();

	public abstract boolean hasQueuedMeasurements();

	public abstract void setUpdatedAll();
	public abstract void setUpdatedAll(int id);
	
	public abstract boolean getUpdated(int id, String lay);
	public abstract void setUpdated(int id, String lay, boolean u);
	
	public abstract boolean getUpdatedCamera(int id);
	public abstract void setUpdatedCamera(int id, boolean u);
	public abstract boolean getUpdatedMeasurement(int id);
	public abstract void setUpdatedMeasurement(int id, boolean u);
	public abstract boolean getUpdatedPassMeasurement(int id);
	public abstract void setUpdatedPassMeasurement(int id, boolean u);
	
	public abstract int getTotalNumberOfFrames();
	public abstract int getTotalNumberOfFrames(String lay);

	public abstract int getNumberOfFrames(int id, String lay);
	public abstract int getNumberOfTelemFrames(int id);
	public abstract int getNumberOfPictureCounters(int id);
	public abstract SortedJpegList getJpegIndex(int id, int period, int fromReset, long fromUptime);
	
	public abstract boolean add(int id, long uptime, int resets, FramePart payload);
	
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
	
	public abstract boolean processNewImageLines() throws SQLException, IOException; // process new lines once they are received.  Called from the server only
	
	public abstract PassMeasurement getLatestPassMeasurement(int id);

	public abstract SortedFramePartArrayList getFrameParts(int id, int fromReset, long fromUptime, int period, boolean reverse, String layout) throws IOException;
	public abstract FramePart getLatest(int id, String layout);
	public abstract FramePart getLatestRt(int id);
	public abstract FramePart getLatestMax(int id);
	public abstract FramePart getLatestMin(int id);

	public abstract FramePart getFramePart(int id, int reset, long uptime, String layout, boolean prev);
	public abstract FramePart getFramePart(int id, int reset, long uptime, int type, String layout, boolean prev);

	public abstract FoxFramePart getLatestRad(int id);
	public abstract RadiationTelemetry getLatestRadTelem(int id);
	public abstract RadiationTelemetry getRadTelem(int id, int resets, long uptime);
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
	public abstract double[][] getGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime, String layout, boolean positionData, boolean reverse);

	public abstract double[][] getRtGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime, boolean positionData, boolean reverse);

	public abstract double[][] getMaxGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime, boolean positionData, boolean reverse);

	public abstract double[][] getMinGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime, boolean positionData, boolean reverse);

	/**
	 * Return an array of radiation data with "period" entries for this sat id and from the given reset and
	 * uptime.
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 */
	public abstract String[][] getRadData(int period, int id, int fromReset, long fromUptime, boolean reverse);

	public abstract String[][] getRadTelemData(int period, int id, int fromReset, long fromUptime, boolean reverse);
	public abstract double[][] getRadTelemGraphData(String name, int period, FoxSpacecraft fox, int fromReset, long fromUptime, boolean positionData, boolean reverse);
	public abstract double[][] getHerciScienceHeaderGraphData(String name, int period, FoxSpacecraft fox, int fromReset, long fromUptime, boolean positionData, boolean reverse);
	public abstract String[][] getHerciPacketData(int period, int id, int fromReset, long fromUptime, boolean type, boolean reverse);
	public abstract String[][] getHerciHsData(int period, int id, int fromReset, long fromUptime, boolean reverse);
	public abstract double[][] getMeasurementGraphData(String name, int period, FoxSpacecraft fox, int fromReset, long fromUptime, boolean reverse);
	public abstract double[][] getPassMeasurementGraphData(String name, int period, FoxSpacecraft fox, int fromReset, long fromUptime, boolean reverse);
	public abstract String[][] getWodRadTelemData(int sAMPLES, int foxId, int sTART_RESET, long sTART_UPTIME, boolean reverse);
	public abstract String[][] getRtData(int sAMPLES, int foxId, int sTART_RESET, long sTART_UPTIME, boolean reverse);
	public abstract String[][] getWODData(int sAMPLES, int foxId, int sTART_RESET, long sTART_UPTIME, boolean reverse);
	public abstract String[][] getWODRadData(int sAMPLES, int foxId, int sTART_RESET, long sTART_UPTIME, boolean reverse);
	
	public abstract String getRtUTCFromUptime(int id, int reset, long uptime);
	
	public abstract int getNumberOfPayloadsBetweenTimestamps(int id, int reset, long uptime, int toReset, long toUptime, String payloadType);
	
	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public abstract void deleteAll();	
	
	public abstract void initRad2();
	public abstract void initHerciPackets();
	public abstract String[][] getTableData(int period, int id, int fromReset, long fromUptime, boolean returnType, boolean reverse, String layout);
	public abstract String[][] getTableData(int period, int id, int fromReset, long fromUptime, boolean reverse, String layout);
	
	public abstract int checkForNewReset(int id, long uptime, Date stepDate, int resetOnFrame, String groundStation);
}
