package telemetry.mesat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;
import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.SatPayloadStore;
import telemetry.SortedFramePartArrayList;
import telemetry.payloads.CanPacket;

public class MesatImageStore {
	private static final int INIT_SIZE = 100;
	private Spacecraft fox;
	public int foxId;
	public static String INDEX_NAME = "image_index.log";
	private String fileName = INDEX_NAME;
	SortedMesatImageList images;
	boolean updatedImage = false;	
	boolean massUpdate = false;

	public MesatImageStore(int id) {
		foxId = id;
		fox = Config.satManager.getSpacecraft(id);
		initPayloadFiles();
	}

	private void initPayloadFiles() {
		// Check for and create the images directory if it does not exist
		String dir = MesatImage.IMAGES_DIR;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			dir = Config.logFileDirectory + File.separator + dir;

		}
		File aFile = new File(dir);
		if(!aFile.isDirectory()){
			aFile.mkdir();
			Log.println("Making directory: " + dir);
		}
		if(!aFile.isDirectory()){
			Log.errorDialog("ERROR", "ERROR can't create the MESAT1 image directory: " + aFile.getAbsolutePath() +  
					"\nAny decoded pictures will not be saved to disk. Downloaded picture packets\n"
					+ "will still be decoded and uploaded to the server.");
		}

		images = new SortedMesatImageList(INIT_SIZE);

		fileName = MesatImage.IMAGES_DIR + File.separator + fox.series+foxId+INDEX_NAME;

		try {
			load(fileName);
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					e.toString(),
					"ERROR Loading Stored MESAT1 Image Index data",
					JOptionPane.ERROR_MESSAGE);

			e.printStackTrace(Log.getWriter());
		}
	}


	public void setUpdatedAll() {
		updatedImage = true;
	}

	public boolean getUpdatedImage() { return updatedImage; }
	public void setUpdatedImage(boolean u) {
		updatedImage = u;
	}

	public int getNumberOfImages() { return images.size(); }

	/**
	 * Add a picture line.  First check to see if we have a suitable file to add it to, if not it is created.  Then append to the file
	 * 
	 * @param f
	 * @return
	 * @throws IOException 
	 */
	public boolean add(int id, int epoch, long uptime,  CanPacket p, String captureDate) throws IOException {
		if (p.getLength() != 8) return false; // not a valid camera packet
		int canid = p.getID();
		// bits 0-3 are the block
		int block = canid & MesatImage.BLOCK_MASK;
		// bits 4-6 are the channel
		int channel = (canid >> MesatImage.CHANNEL_OFFSET) & MesatImage.CHANNEL_MASK;
		// check if packet is actually correct image data     
		if (block < MesatImage.BLOCKS && channel < MesatImage.CHANNELS) {
			for (MesatImage img : images) {
				if (img.addPacket(p)) {
					updatedImage = true;
					if (!massUpdate) {
						img.saveImage(); // save the bytes
						//img.saveAsPng(); // debug
					}
					return true;
				}
			}
			// We did not find it, so try to create a new Image on disk.  This will also save the index entry if the packet is valid
			MesatImage img = new MesatImage(id, p, epoch, uptime, captureDate);
			if (Config.debugCameraFrames)
				Log.println("Adding new image: " + epoch + ":"+ uptime);
			if (img.addPacket(p)) {
				images.add(img);
				updatedImage = true;
				if (!massUpdate) {
					save(img, fileName, true);
					img.saveImage(); // save the bytes
					//img.saveAsPng(); // debug
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Rebuild the images from the Can Packets.  Used when the data is downloaded from the server.
	 */
	public void rebuildFromCanPackets(int id) {
		massUpdate = true;
		// Get a list of the Can Packets
		SortedFramePartArrayList data = null;
		try {
			data = Config.payloadStore.getFrameParts(id, 0, 0L, 99999999, true, fox.getLayoutByType(BitArrayLayout.CAN_PKT).name);
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());
		}
		if (data != null) {
			/* We can get a concurrent modification exception if we use for (FramePart f: data) construct*/
			//for (FramePart f : data) {
			//	CanPacket p = (CanPacket)f;
			for (Iterator<FramePart> it = data.iterator(); it.hasNext(); ) {
				CanPacket p = (CanPacket)it.next();
				
				try {
					Config.payloadStore.mesatImageStore.add(id, p.resets, p.uptime, p, p.getCaptureDate());
				} catch (IOException e) {
					e.printStackTrace(Log.getWriter());
				}
			}
		}
		for (MesatImage img : images) {
			try {
				save(img, fileName, true);
				img.saveImage();
			} catch (IOException e) {
				e.printStackTrace(Log.getWriter());
			} // save the bytes
		}
		massUpdate = false;
		Config.payloadStore.mesatImageStore.setUpdatedImage(true); //////////////////// DOES NOT WORK!!  Need to check number of images too in the tab
	}

	public SortedMesatImageList getIndex(int id, int period, int fromReset, long fromUptime) {

		int start = 0;
		int end = 0;
		if (fromReset == 0 && fromUptime == 0) { // then we take records nearest the end
			start = images.size()-period;
			end = images.size();
		} else {
			// we need to find the start point
			start = images.getNearestFrameIndex(fox.foxId, fromUptime, fromReset);
			if (start == -1 ) return null;
			end = start + period;
		}
		if (end > images.size()) end = images.size();
		if (end < start) end = start;
		if (start < 0) start = 0;
		if (start > images.size()) start = images.size();
		SortedMesatImageList results = new SortedMesatImageList(end-start);


		//int j = end-start-1;
		for (int i=end-1; i>= start; i--) {
			//System.out.println(rtRecords.size());
			results.add(images.get(i));
		}

		return results;
	}

	/**
	 * Load the index from disk
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
					String captureDate = st.nextToken();
					int id = Integer.valueOf(st.nextToken()).intValue();
					int resets = Integer.valueOf(st.nextToken()).intValue();
					long fromUptime = Long.valueOf(st.nextToken()).longValue();
					int image_index = Integer.valueOf(st.nextToken()).intValue();
					int image_channel = Integer.valueOf(st.nextToken()).intValue();
					String name = st.nextToken();

					// We should never get this situation, but good to check..
					if (Config.satManager.getSpacecraft(id) == null) {
						Log.errorDialog("FATAL", "Attempting to Load payloads from the Mesat Camera Image store for satellite with Fox Id: " + id 
								+ "\n when no sattellite with that FoxId is configured.  Add this spacecraft to the satellite directory and restart FoxTelem."
								+ "\nProgram will now exit");
						System.exit(1);
					}
					MesatImage img = new MesatImage(id, resets, fromUptime, image_index, image_channel, captureDate, name);
					// Confirm this file is still on disk
					if (img.fileExists()) {
						images.add(img);
					} else {
						deleted = true;
					}

				}
			}
			dis.close();
			if (deleted) {
				// rewrite the output file
				if (images.size() > 0) {
					save(images.get(0), log, false); // overwrite the file
					for (int i=1; i<images.size(); i++)
						save(images.get(i), log, true); // append the other records
				} else {
					// all of the files must have been manually removed
					// so we do nothing
				}
			}
			updatedImage = true;
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
	public void save(MesatImage frame, String log, boolean append) throws IOException {
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
			for (int i=0; i<images.size(); i++) {
				String imageFile = images.get(i).getFileName();
				if (!Config.logFileDirectory.equalsIgnoreCase("")) {
					imageFile = Config.logFileDirectory + File.separator + imageFile;
					//Log.println("Rremoving: " + log);
				}
				if (images.get(i).fileExists()) {
					SatPayloadStore.remove(imageFile);
					SatPayloadStore.remove(imageFile+".bmp");
					SatPayloadStore.remove(imageFile+".blk");
				}
			}
			SatPayloadStore.remove(log);
			initPayloadFiles();
			setUpdatedAll();
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					ex.toString(),
					"Error Deleting MESAT Image and Index Files for FoxId:"+foxId+", check permissions on file: \n" + log,
					JOptionPane.ERROR_MESSAGE) ;
		}

	}

}
