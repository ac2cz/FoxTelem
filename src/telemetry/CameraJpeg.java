package telemetry;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import common.Config;
import common.Log;
import gui.CameraTab;
import common.FoxSpacecraft;

/**
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
 * A Jpeg that we have downloaded from the camera.  This holds the index information and the methods to write the JPEG file to disk
 * with the header information at the start of the file.  The header is copied from an existing file that is supplied with FoxTelem
 * We do not load the image data into this class unless we are calculating the Thumbnail.  This is therefore an index entry and is
 * used to store a reference to the file and to construct the file in the first place. 
 * It also stores some useful routines to handle thumbnails and files
 *
 */
public class CameraJpeg implements Comparable<CameraJpeg> {

	public static final String IMAGES_DIR = "images";
	public static final String JPG_HEADER = "spacecraft" + File.separator + "jpeg_header.jpg";
	public static final String JPG_HEADER_LOW_RES = "spacecraft" + File.separator + "jpeg_header_low_res.jpg";
	public static final int UPTIME_THRESHOLD = 500; // if the uptime is within this value then it is the same picture.  Assumes 300 seconds per picture
	public static final boolean INSERT_MARKERS = true;
	public static final int LAST_LINE = 59;  // 60 lines from 0 - 59
	public int id; // Fox Id
	public int resets;
	public long fromUptime;
	public long toUptime;
	public int pictureCounter;
	private String fileName;
	public String captureDate;
	private boolean thumbStale = false;
	
	BufferedImage thumbNail; // cache the thumbnail in memory
	SortedArrayList<PictureScanLine> pictureLines;
	
	/**
	 * Constructor for creation of a Jpeg index item from disk
	 * @param id
	 * @param resets
	 * @param from
	 * @param to
	 * @param pc
	 * @param name
	 */
	CameraJpeg(int id, int resets, long from, long to, int pc, String date, String name) {
		this.id = id;
		this.resets = resets;
		fromUptime = from;
		toUptime = to;
		pictureCounter = pc;
		captureDate = date;
		fileName = name;
	}

	/**
	 * Create a new empty Jpeg Entry that is suitable as an index entry that can be saved to the database
	 * Initialize it with any existing image lines
	 * @param id
	 * @param resets
	 * @param from
	 * @param to
	 * @param pc
	 * @throws SQLException 
	 */
	CameraJpeg(int id, int resets, long from, long to, int pc) {
		this.id = id;
		this.resets = resets;
		fromUptime = from;
		toUptime = to;
		pictureCounter = pc;
		pictureLines = new SortedArrayList<PictureScanLine>(60);
	
	}
	
	CameraJpeg(int id, int resets, long from, long to, int pc, SortedArrayList<PictureScanLine> psl) throws SQLException {
		this(id, resets, from, to, pc);
		pictureLines = psl;
		
		fileName = makeFileName();
	}	
	
	/**
	 * Constructor for creation of a jpeg from a new scan line
	 * @param id
	 * @param resets
	 * @param from
	 * @param to
	 * @param pc
	 * @param line
	 * @throws IOException
	 */
	CameraJpeg(int id, int resets, long from, long to, int pc, PictureScanLine line) throws IOException {
		this.id = id;
		this.resets = resets;
		fromUptime = from;
		toUptime = to;
		pictureCounter = pc;
		captureDate = line.captureDate;
		fileName = createJpegFile(id, resets, from, pc, false);
		addLine(line);
	}

	public String getFileName() {
		String toFileName = fileName;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			toFileName = Config.logFileDirectory + File.separator + fileName;
		}	
		return toFileName;
	}
	
	/**
	 * Add a line to this jpeg and write it straight out to disk.  The file we write to will already have the Jpeg header
	 * @param line
	 * @throws IOException
	 */
	public void addLine(PictureScanLine line) throws IOException {
		if (toUptime < line.uptime)
			toUptime = line.uptime;

		String toFileName = getFileName();
			
		OutputStream out = new FileOutputStream(toFileName, true);
		try {
			writeLine(line, out);
		} finally {
			out.close();
		}
	}
	
	private void writeLine(PictureScanLine line, OutputStream out) throws IOException {

		for (int a : line.scanLineData)
			out.write((byte)a);
		// Write the JPEG markers if we needed to
		if (INSERT_MARKERS) {
			out.write((byte)0xFF);
			int num = line.scanLineNumber % 8;
			int rsi = (num & 0x07) | 0xD0;  // calculate the reset indication D0 - D7
			out.write((byte)rsi);

			// If this is the last line then it is followed by the JPEG Footer
			if (line.scanLineNumber == LAST_LINE) {
				Log.println("LAST LINE - JPEG FOOTER ADDED");
				out.write((byte)0xFF);
				out.write((byte)0xD9);
			}
		}
		thumbStale = true;

	}

	/**
	 * Add a line when the Jpeg Index is stored in the DB and we expect to get scan lines out of sequence.
	 * This is the server mode.
	 * 
	 * @param line
	 * @throws IOException
	 */
	public void writeAllLines() throws IOException {
	//	if (toUptime < line.uptime)
	//		toUptime = line.uptime;
	//	pictureLines.add(line);
	//	savePictureLinesFile(line);
		fileName = createJpegFile(id, resets, fromUptime, pictureCounter, true);
		String toFileName = getFileName();
		
		OutputStream out = new FileOutputStream(toFileName, true);
		try {
			for (int i=0; i < pictureLines.size(); i++) {
				writeLine(pictureLines.get(i), out);
			}
		} finally {
			out.close();
		}
		// This does not work because the thumbnail calculation requires a screen, which we do not have on the server.  Needs more thought
		//makeServerThumbnail(CameraTab.THUMB_X);
	}
	
	
	/**
	 * Check to see if the uptime we have been passed is within 150 seconds of the uptimes in this file
	 * @param id
	 * @param resets
	 * @param uptime
	 * @param pc
	 * @return
	 */
	public boolean isSameFile(int id, int resets, long uptime, int pc) {
		if (this.id == id && this.resets == resets && this.pictureCounter == pc) {
			if ((Math.abs(fromUptime - uptime) < UPTIME_THRESHOLD) || (Math.abs(toUptime - uptime) < UPTIME_THRESHOLD)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * These are sorted by reset, uptime as picture Counter is not unique
	 */
	@Override
	public int compareTo(CameraJpeg p) {
		if (resets == p.resets && fromUptime == p.fromUptime) 
			return 0;
		else if (resets < p.resets)
			return -1;
		else if (resets > p.resets)
			return +1;
		else if (resets == p.resets)	
			if (fromUptime < p.fromUptime)
				return -1;
		
		return +1;
	}

	
	public boolean fileExists() {
		String toFileName = getFileName();
			
		File toFile = new File(toFileName);
		if(!toFile.exists())
			return false;
		return true;
			
	}
	
	/**
	 * Check to see if a jpeg file already exists for this picture counter.  Create it if it does not exist
	 * @param id
	 * @param reset
	 * @param uptime
	 * @param pc
	 * @param overWrite - overwrite the existing JPEG
	 * @return
	 * @throws IOException 
	 */
	public String createJpegFile(int id, int reset, long uptime, int pc, boolean overWrite) throws IOException {
		String header = JPG_HEADER;
		FoxSpacecraft sat = (FoxSpacecraft) Config.satManager.getSpacecraft(id);
		if (sat.hasLowResCamera())
			header = JPG_HEADER_LOW_RES;
		
		String name = makeFileName();
		String toFileName = name;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			toFileName = Config.logFileDirectory + File.separator + name;
		}
		File toFile = new File(toFileName);
		if(overWrite || !toFile.exists()){
			File headerFile = new File(header);
			SatPayloadStore.copyFile(headerFile, toFile);
			return name;
		}
		
		return name;
		
	}
	
	public String makeFileName() {
		String name = IMAGES_DIR + File.separator + id + "_" + resets + "_" + fromUptime + "_" + pictureCounter  + ".jpg";
	//	if (!Config.logFileDirectory.equalsIgnoreCase("")) {
	//		name = Config.logFileDirectory + File.separator + name;
	//	} 
		return name;
	}

	public void makeServerThumbnail(int sizeX) {
		
		BufferedImage img = null;
		String imageFile = getFileName();
		
		//scale based on X
		File source = new File(imageFile);
		imageFile = imageFile.replace(".jpg", "_tn.jpg");
		File f = new File(imageFile);
		try {
			img = ImageIO.read(source);
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());
			// Error reading the image file.  Probably corrupt. Create a blank file to show it is there but not valid
			img = new BufferedImage(sizeX, 75,  BufferedImage.TYPE_INT_ARGB);
			try {
				ImageIO.write(img, "JPEG", f);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}	
		}
		if (img != null) {
			double w = img.getWidth();
			double scale = sizeX/w;
			
			thumbNail = scale(img, scale);
			try {
				ImageIO.write(thumbNail, "JPEG", f);
			} catch (IOException e) {
				// ERROR Writing the thumbnail.  We dont want to crash for this, so try to write a blank thumb
				e.printStackTrace();
				// Error reading the image file.  Probably corrupt. Create a blank file to show it is there but not valid
				img = new BufferedImage(sizeX, 75,  BufferedImage.TYPE_INT_ARGB);
				try {
					ImageIO.write(img, "JPEG", f);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}	
			}
		}
		thumbStale = false;
	}
	
	/**
	 * Load the JPEG file from disk and create a Thumbnail.
	 * @param sizeX
	 * @return
	 * @throws IOException
	 * @throws IIOException
	 */
	public BufferedImage getThumbnail(int sizeX) throws IOException, IIOException {
		if (!thumbStale && thumbNail != null) return thumbNail;
		BufferedImage img = null;
		String imageFile = getFileName();
		
		File source = new File(imageFile);
		imageFile = imageFile.replace(".jpg", "_tn.jpg");
		File thumbFile = new File(imageFile);
		if(!thumbStale && thumbFile.exists()) {
			img = ImageIO.read(thumbFile);
			//Log.println("Loading thumb");
			thumbNail = img;
			return img;
		}
		//scale based on X
		
		File f = new File(imageFile);
		try {
			img = ImageIO.read(source);
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());
			// Error creating the image file.  Probably corrupt. Create a blank file to show it is there but not valid
			try {
			img = new BufferedImage(sizeX, 75,  BufferedImage.TYPE_INT_ARGB);
			ImageIO.write(img, "JPEG", f);	
			} catch (Exception e2) {
				Log.errorDialog("ERROR Writing the Jpeg Header to File", "Can't read the file: " + source.getName() + "\n"
						+ "Can't write the file: " + f.getName());
			}
		}
		if (img != null) {
			double w = img.getWidth();
			double scale = sizeX/w;
			
			thumbNail = scale(img, scale);
			ImageIO.write(thumbNail, "JPEG", f);
		}
		thumbStale = false;
		return thumbNail;
	}
	
	/**
	 * Utility function to scale an image
	 * @param source
	 * @param ratio
	 * @return
	 */
	public static BufferedImage scale(BufferedImage source,double ratio) {
		  int w = (int) (source.getWidth() * ratio);
		  int h = (int) (source.getHeight() * ratio);
		  BufferedImage bi = getCompatibleImage(w, h);
		  Graphics2D g2d = bi.createGraphics();
		  double xScale = (double) w / source.getWidth();
		  double yScale = (double) h / source.getHeight();
		  AffineTransform at = AffineTransform.getScaleInstance(xScale,yScale);
		  g2d.drawRenderedImage(source, at);
		  g2d.dispose();
		  return bi;
		}

		private static BufferedImage getCompatibleImage(int w, int h) {
		  GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		  GraphicsDevice gd = ge.getDefaultScreenDevice();
		  GraphicsConfiguration gc = gd.getDefaultConfiguration();
		  BufferedImage image = gc.createCompatibleImage(w, h);
		  return image;
		}

	public String toString() {
		String s = captureDate + "," + id + "," + resets + "," + fromUptime + "," + toUptime + "," + pictureCounter + "," + fileName;
		return s;
	}
	
	public static String getTableCreateStmt() {
		String s = new String();
		s = s + "(id int, resets int, fromUptime bigint, toUptime bigint, "
		 + "pictureCounter int, "
		 + "fileName varchar(255),"
		+ "date_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,";
		s = s + "PRIMARY KEY (id, resets, fromUptime, pictureCounter))";
		return s;
	}
	
	public String getInsertStmt() {
		String s = new String();
		s = s + " (id, resets, fromUptime, toUptime, \n";
		s = s + "pictureCounter,\n";
		s = s + "fileName)\n";
		
		s = s + "values (" + this.id + ", " + resets + ", " + fromUptime + ", " + toUptime + ",\n";
		s = s + pictureCounter+",\n";
		s = s + "'" + fileName+"')\n";
		return s;
	}
}
