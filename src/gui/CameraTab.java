package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import javax.imageio.ImageIO;
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
import telemetry.SortedJpegList;
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
 */
@SuppressWarnings("serial")
public class CameraTab extends FoxTelemTab implements Runnable, MouseListener, ItemListener, ActionListener {
	public static final int MAX_THUMBNAILS_LIMIT = 100;
	public static final int MIN_SAMPLES = 1;
	public static final String CAMERATAB = "CAMERATAB";
	public int MAX_THUMBNAILS = 30;
	public static final int THUMB_X = 100; //640/5
	//public static final int THUMB_Y = 96; //480/5
	
	private static final String DECODED = "Images: ";
	
	private SortedJpegList jpegIndex;
	
	Spacecraft fox;
	int foxId = 0;

	int selectedThumb = 0;
	
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
	
//	JLabel picture;
	ImagePanel picture;
	
	CameraThumb[] thumbnails;
	
	// Picture params
	JLabel picReset;
	JLabel picUptime;
	JLabel picPC;
	JLabel picDate;

	int splitPaneHeight = 0;
	
	private static final String PIC_RESET = "Reset: ";
	private static final String PIC_UPTIME = "Uptime: ";
	private static final String PIC_PC = "Pic Number: ";
	private static final String PIC_DATE = "Captured: ";
	
	int numberOfFiles = 0;
	
	CameraTab(Spacecraft sat) {
		
		
		
		fox = sat;
		foxId = fox.foxId;
		NAME = fox.toString() + " Virginia Tech Camera";
		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), CAMERATAB, "splitPaneHeight");
		
		setLayout(new BorderLayout(0, 0));
	
		thumbnails = new CameraThumb[MAX_THUMBNAILS];
		
		topPanel = new JPanel();
		topPanel.setMinimumSize(new Dimension(34, 250));
		add(topPanel, BorderLayout.NORTH);
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		
		lblName = new JLabel(NAME);
		lblName.setMaximumSize(new Dimension(1600, 20));
		lblName.setMinimumSize(new Dimension(1600, 20));
		lblName.setFont(new Font("SansSerif", Font.BOLD, 14));
		topPanel.add(lblName);
		
		lblImagesDecoded = new JLabel(DECODED);
		lblImagesDecoded.setToolTipText("The number of images that we have downloaded from Fox");
		lblImagesDecoded.setFont(new Font("SansSerif", Font.BOLD, 14));
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
		picPC = addPicParam(PIC_PC);
		picDate = addPicParam(PIC_DATE);
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
	      		Config.saveGraphIntParam(fox.getIdString(), CAMERATAB, "splitPaneHeight", splitPaneHeight);
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
		showLatestImage.setMinimumSize(new Dimension(100, 14));
		//showRawValues.setMaximumSize(new Dimension(100, 14));
		bottomPanel.add(showLatestImage );
		showLatestImage.addItemListener(this);

		addBottomFilter();
	}
	
	private JLabel addPicParam(String text) {
		JLabel title = new JLabel(text);
		title.setFont(new Font("SansSerif", Font.BOLD, 12));
		JLabel lab = new JLabel();
		JPanel panel = new JPanel();
		//panel.setLayout(new FlowLayout(FlowLayout.LEADING));
		panel.setLayout(new GridLayout(1,2,5,5));
		panel.add(title);
		panel.add(lab);
		
		leftHalf.add(panel);
		title.setBorder(new EmptyBorder(3, 5, 3, 0) ); // top left bottom right
		lab.setBorder(new EmptyBorder(3, 0, 3, 25) ); // top left bottom right
		return lab;
	}
	
	protected void addBottomFilter() {
		JLabel displayNumber1 = new JLabel("Displaying last");
		displayNumber2 = new JTextField();
		JLabel displayNumber3 = new JLabel("payloads decoded");
		displayNumber1.setFont(new Font("SansSerif", Font.BOLD, 10));
		displayNumber3.setFont(new Font("SansSerif", Font.BOLD, 10));
		displayNumber1.setBorder(new EmptyBorder(5, 2, 5, 10) ); // top left bottom right
		displayNumber3.setBorder(new EmptyBorder(5, 2, 5, 10) ); // top left bottom right
		displayNumber2.setMinimumSize(new Dimension(50, 14));
		displayNumber2.setMaximumSize(new Dimension(50, 14));
		displayNumber2.setText(Integer.toString(MAX_THUMBNAILS));
		displayNumber2.addActionListener(this);
		bottomPanel.add(displayNumber1);
		bottomPanel.add(displayNumber2);
		bottomPanel.add(displayNumber3);
		
	}

	
	/**
	 * Load the thumbnails from disk based on the entries in the Jpeg Index
	 */
	private void loadThumbs() {
		for (int j=0; j<numberOfFiles; j++)
			if (thumbnails[j] != null)
				thumbnailsPanel.remove(thumbnails[j]);
		
		int lastPic = 0;
		numberOfFiles = jpegIndex.size();
		if (numberOfFiles > MAX_THUMBNAILS)
			lastPic = numberOfFiles - MAX_THUMBNAILS;
//			numberOfFiles = MAX_THUMBNAILS;
		
		for (int i=numberOfFiles-1; i >=lastPic; i--) {
			
			if (jpegIndex.get(i).fileExists()) {
				//Log.println("Picture from: " + jpegIndex.get(i).fileName);
				BufferedImage thumb = null;
				try {
					thumb = jpegIndex.get(i).getThumbnail(THUMB_X);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(Log.getWriter());
				}
				if (thumb != null) {
					if (thumbnails[i] == null)
						thumbnails[i] = new CameraThumb();
					thumbnails[i].setThumb(thumb, jpegIndex.get(i).pictureCounter, jpegIndex.get(i).resets, jpegIndex.get(i).fromUptime);
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
		if (Config.showLatestImage) {
			if (selectedThumb != -1 && thumbnails[selectedThumb] != null)
				thumbnails[selectedThumb].setBorder(new MatteBorder(3, 3, 3, 3, Color.GRAY));
			selectedThumb = numberOfFiles-1;
			if (selectedThumb != -1 && thumbnails[selectedThumb] != null)
				thumbnails[selectedThumb].setBorder(new MatteBorder(3, 3, 3, 3, Color.BLUE));
		}
	}
	
	
	/**
	 * Display the main picture with its paramaters
	 * @param selected
	 * @param reset
	 * @param uptime
	 * @param pc
	 * @param date
	 */
	private void displayPictureParams(int selected) {
		BufferedImage pic = null;
		if (jpegIndex != null)
			if (selected != -1 && jpegIndex.size() > selected && jpegIndex.get(selected) != null)
				try {
					File f = new File(jpegIndex.get(selected).fileName);
					pic = ImageIO.read(f);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(Log.getWriter());
				}
		if (pic != null) {
			picture.setBufferedImage(pic);
//			picture.setIcon(new ImageIcon(pic));
			picReset.setText(""+jpegIndex.get(selected).resets);
			picUptime.setText("" + jpegIndex.get(selected).fromUptime);
			picPC.setText("" + jpegIndex.get(selected).pictureCounter);
			picDate.setText("" + displayCaptureDate(jpegIndex.get(selected).captureDate));
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
				reportDate = "unknown";				
			}
			
			return reportDate;
	}
//	private void setLinesDownloaded(int lines) {
//		lblLinesDownloaded.setText(LINES + lines);
//	}

	private void setImagesDownloaded(int pc) {
		lblImagesDecoded.setText(DECODED + pc);
	}

		
	@Override
	public void run() {
		running = true;
		done = false;
		
		
		while(running) {
			try {
				Thread.sleep(1000); // refresh data once a second, no point in more often as payloads take 5 seconds to download
			} catch (InterruptedException e) {
				Log.println("ERROR: CameraTab thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 			
			if (Config.payloadStore.getUpdatedCamera(foxId)) {
//				Log.println("New data from the camera");

//				setLinesDownloaded(Config.payloadStore.getNumberOfPictureLines(foxId));
				jpegIndex = Config.payloadStore.getJpegIndex(foxId);
				if (jpegIndex != null)
					loadThumbs();
				
				displayPictureParams(selectedThumb);
				setImagesDownloaded(Config.payloadStore.getNumberOfPictureCounters(foxId));
			
				Config.payloadStore.setUpdatedCamera(foxId, false);

			}
			//System.out.println("Camera tab running: " + running);
		}
		done = true;
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		//Log.println("Mouse Clicked");
		for (int i=0; i< thumbnails.length; i++) {
			if (e.getSource() == thumbnails[i]) {
				if (selectedThumb != -1 && thumbnails[selectedThumb] != null)
				thumbnails[selectedThumb].setBorder(new MatteBorder(3, 3, 3, 3, Color.GRAY));
				this.selectedThumb = i;
				displayPictureParams(selectedThumb);
				thumbnails[i].setBorder(new MatteBorder(3, 3, 3, 3, Color.BLUE));
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
					thumbnails[i].setBorder(new MatteBorder(3, 3, 3, 3, Color.BLUE));
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
			
		}

		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.displayNumber2) {
			String text = displayNumber2.getText();
			try {
				MAX_THUMBNAILS = Integer.parseInt(text);
				if (MAX_THUMBNAILS > MAX_THUMBNAILS_LIMIT) {
					MAX_THUMBNAILS = MAX_THUMBNAILS_LIMIT;
					text = Integer.toString(MAX_THUMBNAILS_LIMIT);
				}
				if (MAX_THUMBNAILS < MIN_SAMPLES) {
					MAX_THUMBNAILS = MIN_SAMPLES;
					text = Integer.toString(MIN_SAMPLES);
				}
				//System.out.println(SAMPLES);
				
				//lblActual.setText("("+text+")");
				//txtPeriod.setText("");
			} catch (NumberFormatException ex) {
				
			}
			displayNumber2.setText(text);
			loadThumbs();
			repaint();
		}
		
	}

}
