package telemetry.mesat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.imageio.ImageIO;

import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.payloads.CanPacket;

/**
 * FOX Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2022 amsat.org
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
 * This is an image from the multispectal camera on MESAT1.  It is built from can packets.
 * The bytes can be saved and loaded from disk.
 * 
 * Each image has an image number from 0-31
 * Images are sent in 12 blocks
 * There are 5 channels with different frequency responses.  An image is for 1 channel.
 * Images are 256 x 256.
 * 
 *
 */
public class MesatImage implements Comparable<MesatImage>{
	public static final String IMAGES_DIR = "images";
	public static final int NO_INDEX = -1;
	
	static final int BLOCK_MASK     = 0xF;
	static final int CHANNEL_OFFSET   = 4;
	static final int CHANNEL_MASK   = 0x7;
	static final int INDEX_OFFSET   = 7;
	static final int INDEX_MASK     = 0x1F;
	
	static final int XBYTE     = 2;
	static final int YBYTE     = 3;
	
	public static final int BLOCKS     = 11;
	public static final int BLOCKS_FULL_LIMIT     = 5956;
	public static final int CHANNELS     = 5;
	static final int LEN     = 8;
	static final int MAX_PKTS_IN_BLOCK = 24;
	static final int SIZE = 256;
	
	public int id;
	public int epoch;
	public long fromUptime;
	public String captureDate;
	BufferedImage image;
	int width = SIZE; // default size
	int height = SIZE; 
	public byte[] bytes;
	byte[] blockMap; // which bytes are populated.  We write the block that they came from into this array
	//public int[] blockPkts; // How many bytes do we have in each block
	String filename;
	Spacecraft sat;
	public int image_index = NO_INDEX; // the index number of the image. 0 - 31
	public int image_channel = NO_INDEX; // the channel from the multispectral camera
	
	String[] image_channels = {"c0", "c1", "c2", "c3", "c4"};
	public static String[] image_channels_desc = {
			"C0 - Red Cam1 (IR Filter) - 550-680nm", 
			"C1 - Blue Cam2 (Yellow lens) - 480-600nm", 
			"C2 - Blue Cam3 (Violet Lens) - 400-500nm", 
			"C3 - Red Cam4 (Red Lens) - 600-1100nm", 
			"C4 - Blue Cam4 (Red Lens) - 750-1100nm"};
	
	public MesatImage(int id, int epoch, long uptime, String date) {
		this.id = id;
		this.sat = Config.satManager.getSpacecraft(id);
		this.epoch = epoch;
		this.fromUptime = uptime;
		this.captureDate = date;
		bytes = new byte[width * height];
		blockMap = new byte[width * height];
	}
	
	public MesatImage(int id, CanPacket p,  int epoch, long uptime, String date) {
		this.id = id;
		this.sat = Config.satManager.getSpacecraft(id);
		this.epoch = epoch;
		this.fromUptime = uptime;
		this.captureDate = date;
		bytes = new byte[width * height];
		blockMap = new byte[width * height];
	}
	
	/**
	 * Constructor to load a MESAT Image from Disk
	 * @param id
	 * @param epoch
	 * @param fromUptime
	 * @param image_index
	 * @param image_channel
	 * @param date
	 * @param name
	 * @throws IOException
	 */
	public MesatImage(int id, int epoch, long fromUptime, int image_index, int image_channel, String date, String name) throws IOException {
		this.id = id;
		this.epoch = epoch;
		this.fromUptime = fromUptime;
		this.image_index = image_index;
		this.image_channel = image_channel;
		this.captureDate = date;
		this.filename = name;
		this.sat = Config.satManager.getSpacecraft(id);
		bytes = new byte[width * height];
		blockMap = new byte[width * height];
		if (fileExists()) {
			loadImage();
		}
	}
	
	public static BufferedImage getCompositeImage(MesatImage red_img, MesatImage green_img, MesatImage blue_img) {
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB); 

		for (int x = 0; x < SIZE; x++) {
			for (int y = 0; y < SIZE; y++) {
				int pos = y * SIZE + x;
				int r = 0,g = 0,b = 0;
				
				if (red_img != null)
					r = (int)red_img.bytes[pos] & 0xff;
				if (green_img != null)
					g = (int)green_img.bytes[pos] & 0xff; 
				if (blue_img != null)
					b = (int)blue_img.bytes[pos] & 0xff;
				Color pix = new Color(r,g,b);
				image.setRGB(x, y, pix.getRGB());
			}
		}

	//	  File outputFile = new File("/output.bmp");
	//	  ImageIO.write(image, "bmp", outputFile);
		  
		  return image;
	}
	
	public BufferedImage getImage() {
		makeGrayScaleImage();
		return image;
	}
	
	public String getFileName() {
		String toFileName = IMAGES_DIR + File.separator + sat.series+sat.foxId + "_" + image_index + "_" + image_channels[image_channel] + ".dat";
		
		return toFileName;
	}
	
	public boolean fileExists() {
		String toFileName = filename;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			toFileName = Config.logFileDirectory + File.separator + filename;
		}	
		File toFile = new File(toFileName);
		if(!toFile.exists())
			return false;
		return true;
			
	}
	
	/**
	 * Total the number of bytes received for each block
	 * @return
	 */
	public int[] getBlockByteNumbers() {
		int[] blockCounts = new int[BLOCKS];
		for (int i=0; i<blockMap.length; i++) {
			if (blockMap[i] > 0 && blockMap[i] <= BLOCKS) // check for Block Number + 1
				blockCounts[blockMap[i]-1]++;
		}
	
		return blockCounts;
	}
	
	/**
	 * Return true if this is a valid image Can Packet has data for the this image.
	 * It is the same image if it has the same image number.
	 * 
	 * @param canPacket
	 * @return
	 */			
	public boolean addPacket(CanPacket canPacket) {
		int canid = canPacket.getID();
		int len = canPacket.getLength();
		
		// bits 0-3 are the block
		int block = canid & BLOCK_MASK;
		// bits 4-6 are the channel
		int channel = (canid >> CHANNEL_OFFSET) & CHANNEL_MASK;
		// bits 7-11 are the image index
		int index = (canid >> INDEX_OFFSET) & INDEX_MASK;
		if (image_index == NO_INDEX || (index == image_index && channel == image_channel)) {

			if (image_index == NO_INDEX) {
				image_index = index;
				image_channel = channel;
			}

			// check if packet is actually correct image data     
			// with 8 data values, correct block and channel vals 
			if (len==LEN && block < BLOCKS && channel < CHANNELS) {
				
				int x = canPacket.fieldValue[XBYTE];
				int y = canPacket.fieldValue[YBYTE];
				filename = getFileName();
				if (Config.debugCameraFrames)
					Log.println("Adding packet to image: " + index + ":"+ channel + " for block "+ block + " at pixel " + x + "." + y + "   -> " + filename);
				for (int i=4; i<10; i++) {
					int pos = x * width + y;
					bytes[pos] = (byte) canPacket.fieldValue[i];
					blockMap[pos] = (byte) (block + 1); // we use zero as no block, so increment by one and store in the map
					y++;
					if (y >= width) {
						x = (x + 1)%height;
						y=0;
					}
				}
				return true;
			}
		} else {
			// then this is for a different image. Don't add it here
			return false;
		}
		return false;
	}
	
//	/**
//	 * This is called when a packet is successfully added to the Image Payload Store.  We call it from there to avoid 
//	 * counting duplicates
//	 */
//	public void incrementBlock(int block) {
//		blockPkts[block]++;
//		//if (blockPkts[block] > 999) blockPkts[block] = 999;
//	}
	
	public void makeGrayScaleImage() {
		image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY); // vs BufferedImage.TYPE_3BYTE_BGR

		byte[] array = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		for (int i=0; i< array.length; i++) {
			array[i] = bytes[i];
		}

	}
	
	public BufferedImage getThumbnail(int thumbX) {
		makeGrayScaleImage();
		return image;
	}
	
	public void saveImage() throws IOException {
		
		// First write the block totals
		String toBlockFileName = filename + ".blk";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			toBlockFileName = Config.logFileDirectory + File.separator + toBlockFileName;
		}	
		FileOutputStream blockSaveFile = null;
		try {
			blockSaveFile = new FileOutputStream(toBlockFileName);
			blockSaveFile.write(blockMap);
//			for (int b : blockMap) {
//				blockSaveFile.write((b >> 24 )& 0xff);
//				blockSaveFile.write((b >> 16 )& 0xff);
//				blockSaveFile.write((b >> 8 )& 0xff);
//				blockSaveFile.write(b & 0xff);
//			}
		} finally {
			try { if (blockSaveFile != null) blockSaveFile.close(); } catch (IOException e) { }
		}
		
		String toFileName = filename;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			toFileName = Config.logFileDirectory + File.separator + toFileName;
		}	
		FileOutputStream saveFile = null;
		try {
			saveFile = new FileOutputStream(toFileName);
			saveFile.write(bytes);
		} finally {
			try { if (saveFile != null) saveFile.close(); } catch (IOException e) { }
		}
		
		saveAsBmp();
	}
	
	public void saveAsBmp() throws IOException {
		String toFileName = filename + ".bmp";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			toFileName = Config.logFileDirectory + File.separator + toFileName;
		}	
		makeGrayScaleImage();
		File outputfile = new File(toFileName);
	    ImageIO.write(image, "bmp", outputfile);
	}
	
	public boolean hasBlockMap() {
		String toBlockFileName = filename + ".blk";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			toBlockFileName = Config.logFileDirectory + File.separator + toBlockFileName;
		}	
		File blockFile = new File(toBlockFileName);
		if (blockFile.exists()) 
			return true;
		return false;
		
	}
	
	private void loadImage() throws IOException {
		// First load block file
		String toBlockFileName = filename + ".blk";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			toBlockFileName = Config.logFileDirectory + File.separator + toBlockFileName;
		}	
		RandomAccessFile blockFileOnDisk = null;
		File blockFile = new File(toBlockFileName);
		if (blockFile.exists()) {
			try {
				blockFileOnDisk = new RandomAccessFile(toBlockFileName, "r"); // opens file 

				if (blockFileOnDisk.length() != 256*256) throw new IOException("Invalid Block file length\n");
				blockFileOnDisk.read(blockMap);			
				//			for (int i=0; i <  12; i++)
				//					blockPkts[i] = blockFileOnDisk.readInt();			
			} finally {
				try { if (blockFileOnDisk != null) blockFileOnDisk.close(); } catch (IOException e) { }
			}
		}
		
		// Then the image file
		String toFileName = filename;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			toFileName = Config.logFileDirectory + File.separator + toFileName;
		}	
		RandomAccessFile fileOnDisk = null;
		
		try {
			fileOnDisk = new RandomAccessFile(toFileName, "r"); // opens file 
			if (fileOnDisk.length() != 256*256) throw new IOException("Invalid Image file length\n");
			//bytes = new byte[(int) fileOnDisk.length()];
			fileOnDisk.read(bytes);			
		} finally {
			try { if (fileOnDisk != null) fileOnDisk.close(); } catch (IOException e) { }
		}
	}
	
	/** 
	 * This is used to write to the index file
	 */
	public String toString() {
		String s = captureDate + "," + id + "," + epoch + "," + fromUptime + "," + image_index + "," + image_channel + "," + filename;
		return s;
	}

	@Override
	public int compareTo(MesatImage p) {
		if (epoch == p.epoch && fromUptime == p.fromUptime) 
			return 0;
		else if (epoch < p.epoch)
			return -1;
		else if (epoch > p.epoch)
			return +1;
		else if (epoch == p.epoch)	
			if (fromUptime < p.fromUptime)
				return -1;
		
		return +1;
	}
	
}
