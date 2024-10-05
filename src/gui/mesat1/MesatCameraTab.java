package gui.mesat1;

import gui.tabs.FoxTelemTab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import telemetry.FramePart;
import telemetry.legacyPayloads.CameraJpeg;
import telemetry.mesat.MesatImage;
import telemetry.mesat.SortedMesatImageList;
import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;
import gui.WrapLayout;
import gui.graph.GraphFrame;
import gui.legacyTabs.ImagePanel;

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
 */
@SuppressWarnings("serial")
public class MesatCameraTab extends FoxTelemTab implements Runnable, MouseListener, ItemListener, ActionListener, FocusListener {
	public static final int MAX_THUMBNAILS_LIMIT = 500;
	public static final int DEFAULT_THUMBNAILS = 30;
	public static final int MIN_SAMPLES = 1;
	public static final String CAMERATAB = "MESAT1CAMERATAB";
	public int maxThumbnails = DEFAULT_THUMBNAILS;;
	public static final int THUMB_X = 100; //640/5
	//public static final int THUMB_Y = 96; //480/5
	
	private static final String DECODED = "Images: ";
	
	private SortedMesatImageList imageIndex;
	
	Spacecraft fox;
	int foxId = 0;

	int selectedThumb = 0;
	MesatImage selectedImage;
	
	JLabel lblName;
	private String NAME;
	JLabel lblImagesDecoded;
	//JLabel lblLinesDownloaded;
	//JLabel lblUniquePC;
	JCheckBox showLatestImage;
	JSplitPane splitPane;
	JPanel topPanel;
	JPanel bottomPanel;
	JPanel thumbnailsPanel;
	JPanel picturePanel;
	JPanel leftHalf;
	JPanel rightHalf;

	JTextField displayNumber2;
	private JLabel lblFromUptime;
	private JTextField textFromUptime;
	private JLabel lblFromReset;
	private JTextField textFromReset;
	public static long DEFAULT_START_UPTIME = 0;
	public static int DEFAULT_START_RESET = 0;
	public long START_UPTIME = DEFAULT_START_UPTIME;
	public int START_RESET = DEFAULT_START_RESET;
//	JLabel picture;
	ImagePanel picture;
	
	MesatImageThumb[] thumbnails;
	
	// Picture params
	JLabel picReset;
	JLabel picUptime;
	JLabel picNumber, picChannel;
	JLabel picDate;
	JLabel[] picBlock;

	int splitPaneHeight = 0;
	
	private static final String PIC_RESET = "Epoch: ";
	private static final String PIC_UPTIME = "Uptime: ";
	private static final String PIC_NUMBER = "Image Number: ";
	private static final String PIC_CHANNEL = "Channel: ";
	private static final String PIC_DATE = "Downloaded: ";
	
	int actualThumbnails = 0;
	
	public MesatCameraTab(Spacecraft sat) {
		
		fox = sat;
		foxId = fox.foxId;
		loadProperties();
		NAME = fox.toString() + " University of Maine Multispectral Camera";
		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_CAMERA_DATA, CAMERATAB, "splitPaneHeight");
		
		setLayout(new BorderLayout(0, 0));
	
		thumbnails = new MesatImageThumb[maxThumbnails]; // size this to hold references up to the limit that was loaded from the properties file
		
		topPanel = new JPanel();
		topPanel.setMinimumSize(new Dimension(34, 250));
		add(topPanel, BorderLayout.NORTH);
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		
		int fonth = (int)(Config.displayModuleFontSize * 14/11);
		lblName = new JLabel(NAME);
		lblName.setMaximumSize(new Dimension(1600, (int)(Config.displayModuleFontSize * 20/11)));
		lblName.setMinimumSize(new Dimension(1600, (int)(Config.displayModuleFontSize * 20/11)));
		lblName.setFont(new Font("SansSerif", Font.BOLD, fonth));
		topPanel.add(lblName);
		
		lblImagesDecoded = new JLabel(DECODED);
		lblImagesDecoded.setToolTipText("The number of images that we have downloaded from Fox");
		lblImagesDecoded.setFont(new Font("SansSerif", Font.BOLD, fonth));
		lblImagesDecoded.setBorder(new EmptyBorder(5, 2, 5, 5) );
		topPanel.add(lblImagesDecoded);

		picturePanel = new JPanel();
		
		picturePanel.setLayout(new BorderLayout(0,0));
		picturePanel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		picturePanel.setBackground(Color.DARK_GRAY);
		
		
		leftHalf = new JPanel(); 
		leftHalf.setLayout(new BoxLayout(leftHalf, BoxLayout.Y_AXIS));
		
		picReset = addPicParam(PIC_RESET);
		picUptime = addPicParam(PIC_UPTIME);
		picNumber = addPicParam(PIC_NUMBER);
		picChannel = addPicParam(PIC_CHANNEL);
		picDate = addPicParam(PIC_DATE);
		picBlock = new JLabel[12];
		for(int i=0; i<MesatImage.BLOCKS; i++) {
			picBlock[i] = addPicBlockParam("Block " + i + ":  ");
		}
		leftHalf.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));
		
		//leftHalf.setBackground(Color.DARK_GRAY);
		rightHalf = new JPanel(); //new ImagePanel("C:/Users/chris.e.thompson/Desktop/workspace/SALVAGE/data/stars5.png");
		rightHalf.setBackground(Color.DARK_GRAY);
		rightHalf.setLayout(new BorderLayout(0,0));

//		picture = new JLabel();
		picture = new ImagePanel();
		rightHalf.add(picture, BorderLayout.CENTER);

		picturePanel.add(leftHalf, BorderLayout.WEST);
		picturePanel.add(rightHalf, BorderLayout.CENTER);
	
		thumbnailsPanel = new JPanel();
		thumbnailsPanel.setLayout(new WrapLayout(FlowLayout.LEADING, 25, 25));
		JScrollPane scrollPane = new JScrollPane (thumbnailsPanel, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				picturePanel, scrollPane);
		splitPane.setOneTouchExpandable(true);
		splitPane.setContinuousLayout(true); // repaint as we resize, otherwise we can not see the moved line against the dark background
		if (splitPaneHeight != 0) 
			splitPane.setDividerLocation(splitPaneHeight);
		else
			splitPane.setDividerLocation(450);
		SplitPaneUI spui = splitPane.getUI();
	    if (spui instanceof BasicSplitPaneUI) {
	      // Setting a mouse listener directly on split pane does not work, because no events are being received.
	      ((BasicSplitPaneUI) spui).getDivider().addMouseListener(new MouseAdapter() {
	          public void mouseReleased(MouseEvent e) {
	        	  splitPaneHeight = splitPane.getDividerLocation();
	        	  Log.println("SplitPane: " + splitPaneHeight);
	        	  displayPictureParams(selectedThumb);
	      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_CAMERA_DATA, CAMERATAB, "splitPaneHeight", splitPaneHeight);
	          }
	      });
	    }
	    //Provide minimum sizes for the two components in the split pane
		Dimension minimumSize = new Dimension(100, 50);
		picturePanel.setMinimumSize(minimumSize);
		thumbnailsPanel.setMinimumSize(minimumSize);
		add(splitPane, BorderLayout.CENTER);
		
		bottomPanel = new JPanel();
		add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));		

		showLatestImage = new JCheckBox("Show Latest Image", Config.showLatestImage);
		showLatestImage.setMinimumSize(new Dimension(150, fonth));
		//showRawValues.setMaximumSize(new Dimension(100, 14));
		bottomPanel.add(showLatestImage );
		showLatestImage.addItemListener(this);
		bottomPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(500,10), new Dimension(1500,10)));

		addBottomFilter();
	}
	
	private JLabel addPicParam(String text) {
		JLabel title = new JLabel(text);
		title.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		JLabel lab = new JLabel();
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEADING));
		//panel.setLayout(new GridLayout(1,2,5,5));
		panel.add(title);
		panel.add(lab);
		
		leftHalf.add(panel);
		title.setBorder(new EmptyBorder(3, 5, 3, 0) ); // top left bottom right
		lab.setBorder(new EmptyBorder(3, 0, 3, 25) ); // top left bottom right
		return lab;
	}
	
	private JLabel addPicBlockParam(String text) {
		JLabel title = new JLabel(text);
		title.setFont(new Font("SansSerif", Font.PLAIN, (int)(Config.displayModuleFontSize * 9/11)));
		JLabel lab = new JLabel();
		lab.setFont(new Font("SansSerif", Font.PLAIN, (int)(Config.displayModuleFontSize * 9/11)));
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEADING));
		//panel.setLayout(new GridLayout(1,2,5,5));
		panel.add(title);
		panel.add(lab);
		
		leftHalf.add(panel);
//		title.setBorder(new EmptyBorder(3, 5, 3, 0) ); // top left bottom right
//		lab.setBorder(new EmptyBorder(3, 0, 3, 25) ); // top left bottom right
		return lab;
	}
	
	protected void addBottomFilter() {
		JLabel displayNumber1 = new JLabel("Displaying last");
		displayNumber2 = new JTextField();
		displayNumber2.setColumns(4);
		JLabel displayNumber3 = new JLabel("images decoded");
		displayNumber1.setFont(new Font("SansSerif", Font.PLAIN, 10));
		displayNumber3.setFont(new Font("SansSerif", Font.PLAIN, 10));
		displayNumber1.setBorder(new EmptyBorder(5, 2, 5, 10) ); // top left bottom right
		displayNumber3.setBorder(new EmptyBorder(5, 2, 5, 10) ); // top left bottom right
		//displayNumber2.setMinimumSize(new Dimension(50, 14));
		//displayNumber2.setMaximumSize(new Dimension(50, 14));
		displayNumber2.setText(Integer.toString(maxThumbnails));
		displayNumber2.addActionListener(this);
		bottomPanel.add(displayNumber1);
		bottomPanel.add(displayNumber2);
		bottomPanel.add(displayNumber3);
		
		lblFromReset = new JLabel("   from Epoch  ");
		lblFromReset.setFont(new Font("SansSerif", Font.PLAIN, 10));
		bottomPanel.add(lblFromReset);
		
		textFromReset = new JTextField();
		bottomPanel.add(textFromReset);
		textFromReset.setText(Integer.toString(START_RESET));

		textFromReset.setColumns(8);
		textFromReset.addActionListener(this);
		textFromReset.addFocusListener(this);
		
		lblFromUptime = new JLabel("   from Uptime  ");
		lblFromUptime.setFont(new Font("SansSerif", Font.PLAIN, 10));
		bottomPanel.add(lblFromUptime);
		
		textFromUptime = new JTextField();
		bottomPanel.add(textFromUptime);

		textFromUptime.setText(Long.toString(START_UPTIME));
		textFromUptime.setColumns(8);
//		textFromUptime.setPreferredSize(new Dimension(50,14));
		textFromUptime.addActionListener(this);
		textFromUptime.addFocusListener(this);
		
	}

	/**
	 * Load thumbnails from the image store
	 */
	private void loadThumbs() {
		for (int j=0; j<actualThumbnails; j++)
			if (thumbnails[j] != null)
				thumbnailsPanel.remove(thumbnails[j]);
		
		imageIndex = Config.payloadStore.mesatImageStore.getIndex(foxId, maxThumbnails, START_RESET, START_UPTIME);
		if (imageIndex == null) return;
		actualThumbnails = imageIndex.size();
		
		thumbnails = new MesatImageThumb[actualThumbnails];
		
		for (int i=0; i < actualThumbnails; i++) {
			
			if (imageIndex.get(actualThumbnails-i-1).fileExists()) {
				//Log.println("Picture from: " + jpegIndex.get(i).fileName);
				if (selectedImage != null)
					if (imageIndex.get(actualThumbnails-i-1).compareTo(selectedImage) == 0) {
						selectedThumb = i; // cache this
					}
				BufferedImage thumb = null;
				thumb = imageIndex.get(actualThumbnails-i-1).getThumbnail(THUMB_X);
				if (thumb != null) {
					
					if (thumbnails[i] == null)
						thumbnails[i] = new MesatImageThumb();
					thumbnails[i].setThumb(thumb, imageIndex.get(actualThumbnails-i-1).epoch, imageIndex.get(actualThumbnails-i-1).fromUptime
							, imageIndex.get(actualThumbnails-i-1).image_index, imageIndex.get(actualThumbnails-i-1).image_channel);
//					thumbnails[i].setIcon(new ImageIcon(thumb));
					thumbnailsPanel.add(thumbnails[i]);
					thumbnails[i].addMouseListener(this);
					thumbnails[i].setBorder(new MatteBorder(3, 3, 3, 3, Color.GRAY));
					//DataBuffer buff = thumb.getRaster().getDataBuffer();
					//int bytes = buff.getSize() * DataBuffer.getDataTypeSize(buff.getDataType()) / 8;
					//Log.println("Loaded Thumb " + i + " size: " + bytes);
				}	
			}
		}
		if (actualThumbnails > 0)
			if (Config.showLatestImage) { // we automatically select the latest image, which is the first in the array
				// first deselect the previous selection
				if (thumbnails != null && selectedThumb < thumbnails.length  && selectedThumb >=0) {
					if (thumbnails[selectedThumb] != null) 
						thumbnails[selectedThumb].setBorder(new MatteBorder(3, 3, 3, 3, Color.GRAY));
					selectedThumb = 0;
					if (thumbnails[selectedThumb] != null) {
						selectThumb(selectedThumb);
					}
				}
			} else {
				if (selectedThumb < thumbnails.length)
					if (thumbnails[selectedThumb] != null)
						selectThumb(selectedThumb);
			}
	}
	
	/**
	 * Load the thumbnails from disk based on the entries in the Jpeg Index
	 */
	
	private void selectThumb(int t) {
		thumbnails[t].setBorder(new MatteBorder(3, 3, 3, 3, Color.BLUE));
		int selected = imageIndex.size() - t-1;
		selectedImage = imageIndex.get(selected);
	}
	
	/**
	 * Display the main picture with its paramaters
	 * @param selected
	 * @param reset
	 * @param uptime
	 * @param pc
	 * @param date
	 */
	private void displayPictureParams(int clicked) {
		if (imageIndex == null) {
			return;
		}
		int selected = imageIndex.size() - clicked-1;
		BufferedImage pic = null;
		if (selected >= 0)
			if (imageIndex != null)
				if (selected != -1 && imageIndex.size() > selected && imageIndex.get(selected) != null)
					pic = imageIndex.get(selected).getImage();
		if (pic != null) {
			double ratio = (double)splitPaneHeight/(double)pic.getHeight();
			BufferedImage dimg = CameraJpeg.scale(pic, ratio);
			picture.setBufferedImage(dimg);
			picReset.setText(""+imageIndex.get(selected).epoch);
			picUptime.setText("" + imageIndex.get(selected).fromUptime);
			picNumber.setText("" + imageIndex.get(selected).image_index);
			picChannel.setText("" + imageIndex.get(selected).image_channels_desc[imageIndex.get(selected).image_channel]);
			picDate.setText("" + displayCaptureDate(imageIndex.get(selected).captureDate));
			
			int[] blockCounts = imageIndex.get(selected).getBlockByteNumbers();
			for (int i=0; i< MesatImage.BLOCKS; i++) {
				if (blockCounts[i] > MesatImage.BLOCKS_FULL_LIMIT) {
					picBlock[i].setText( "FULL");
				} else {
					picBlock[i].setText( "" + blockCounts[i]);
				}
			}
		}
	}

	private String displayCaptureDate(String u) {
		Date result = null;
		String reportDate = null;
			try {
				FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				result = FramePart.fileDateFormat.parse(u);
				FramePart.reportDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				reportDate = FramePart.reportDateFormat.format(result);
				
			} catch (ParseException e) {
				reportDate = u;				
			} catch (Exception e) {
				reportDate = u;	
			}
			
			return reportDate;
	}
//	private void setLinesDownloaded(int lines) {
//		lblLinesDownloaded.setText(LINES + lines);
//	}

	private void setImagesDownloaded(int pc) {
		lblImagesDecoded.setText(DECODED + pc);
	}
	
	/**
	 * Rebuild the images from CanPackets
	 */
//	private void rebuildFromCanPackets() {
//		Config.payloadStore.mesatImageStore.rebuildFromCanPackets(fox.foxId);
//	}

		
	@Override
	public void run() {
		Thread.currentThread().setName("MesatCameraTab");

		running = true;
		done = false;
		int currentFrames = 0;
		
		while(running) {
			try {
				Thread.sleep(1000); // refresh data once a second, no point in more often as payloads take 5 seconds to download
			} catch (InterruptedException e) {
				Log.println("ERROR: MesatCameraTab thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 
			//parseCanPackets();
			if (foxId != 0 && Config.payloadStore.initialized()) {
				
				if (Config.payloadStore.mesatImageStore != null ) {
					Config.payloadStore.mesatImageStore.buildBlockMapsIfNeeded();
					int numberOfFrames = Config.payloadStore.mesatImageStore.getNumberOfImages();
					if (currentFrames != numberOfFrames || Config.payloadStore.mesatImageStore.getUpdatedImage()) {
						if (Config.debugCameraFrames)
							Log.println("New tab data from the mesat1 camera");

						currentFrames = numberOfFrames;
						loadThumbs();

						displayPictureParams(selectedThumb);
						setImagesDownloaded(Config.payloadStore.mesatImageStore.getNumberOfImages());

						Config.payloadStore.mesatImageStore.setUpdatedImage(false);
						MainWindow.frame.repaint();
					}

				}
			}
			//System.out.println("MESAT Camera tab running: " + running);
		}
		done = true;
		
	}

	public void saveProperties() {

		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_CAMERA_DATA, CAMERATAB, "maxThumbnails", maxThumbnails);
		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_CAMERA_DATA, CAMERATAB, "fromReset", this.START_RESET);
		Config.saveGraphLongParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_CAMERA_DATA, CAMERATAB, "fromUptime", this.START_UPTIME);
		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_CAMERA_DATA, CAMERATAB, "selectedThumb", this.selectedThumb);
	}
	
	public void loadProperties() {

		maxThumbnails = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_CAMERA_DATA, CAMERATAB, "maxThumbnails");
		if (maxThumbnails == 0) maxThumbnails = DEFAULT_THUMBNAILS;
		if (maxThumbnails > MAX_THUMBNAILS_LIMIT) {
			maxThumbnails = MAX_THUMBNAILS_LIMIT;
		}
			
		this.START_RESET = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_CAMERA_DATA, CAMERATAB, "fromReset");
		this.START_UPTIME = Config.loadGraphLongValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_CAMERA_DATA, CAMERATAB, "fromUptime");
		this.selectedThumb = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_CAMERA_DATA, CAMERATAB, "selectedThumb");

	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		//Log.println("Mouse Clicked");
		for (int i=0; i< thumbnails.length; i++) {
			if (e.getSource() == thumbnails[i]) {
				if (selectedThumb != -1 && selectedThumb < thumbnails.length && thumbnails[selectedThumb] != null)
				thumbnails[selectedThumb].setBorder(new MatteBorder(3, 3, 3, 3, Color.GRAY));
				this.selectedThumb = i;
				displayPictureParams(selectedThumb);
				selectThumb(i);
				saveProperties();
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		for (int i=0; i< thumbnails.length; i++) {
			if (e.getSource() == thumbnails[i]) {
				thumbnails[i].setBorder(new MatteBorder(3, 3, 3, 3, Color.DARK_GRAY));
			}
		}
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		for (int i=0; i< thumbnails.length; i++) {
			if (e.getSource() == thumbnails[i]) {
				if (selectedThumb == i)
					selectThumb(i);
				else
					thumbnails[i].setBorder(new MatteBorder(3, 3, 3, 3, Color.GRAY));
			}
		}
		
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		
		
		
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		
		if (source == showLatestImage) { //updateProperty(e, decoder.flipReceivedBits); }

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.showLatestImage = false;
			} else {
				Config.showLatestImage = true;
			}
			Config.save();
			
		}

		
	}

	private void parseTextFields() {
		String text = displayNumber2.getText();
		try {
			maxThumbnails = Integer.parseInt(text);
			if (maxThumbnails > MAX_THUMBNAILS_LIMIT) {
				maxThumbnails = MAX_THUMBNAILS_LIMIT;
				text = Integer.toString(MAX_THUMBNAILS_LIMIT);
			}
			if (maxThumbnails < MIN_SAMPLES) {
				maxThumbnails = MIN_SAMPLES;
				text = Integer.toString(MIN_SAMPLES);
			}

			//System.out.println(SAMPLES);
			
			//lblActual.setText("("+text+")");
			//txtPeriod.setText("");
		} catch (NumberFormatException ex) {
			
		}
		displayNumber2.setText(text);
		text = textFromReset.getText();
		try {
			START_RESET = Integer.parseInt(text);
			if (START_RESET < 0) START_RESET = 0;
			
		} catch (NumberFormatException ex) {
			if (text.equals("")) {
				START_RESET = DEFAULT_START_RESET;
				
			}
		}
		textFromReset.setText(text);
		
		text = textFromUptime.getText();
		try {
			START_UPTIME = Integer.parseInt(text);
			if (START_UPTIME < 0) START_UPTIME = 0;
			
		} catch (NumberFormatException ex) {
			if (text.equals("")) {
				START_UPTIME = DEFAULT_START_UPTIME;
				
			}
		}
		textFromUptime.setText(text);
		
		loadThumbs();
		displayPictureParams(selectedThumb);
		repaint();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.displayNumber2) {
			parseTextFields();
		} else if (e.getSource() == this.textFromReset) {
			parseTextFields();
			
		} else if (e.getSource() == this.textFromUptime) {
			parseTextFields();
			
		}
		saveProperties();
		
	}

	@Override
	public void focusGained(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusLost(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
