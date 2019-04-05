package gui;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import common.Config;
import common.Log;
import common.PassManager;
import common.Spacecraft;
import decoder.RfData;
import decoder.SourceIQ;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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
 * A panel that displays an FFT of the data that it reads from a source
 * The frequency of each bin in the result is SAMPLE_RATE/fftSamples
 * 
 * We retrieve the Power Spectral Density from the Decoder.  This is the magnitude of the I/Q samples
 * after the FFT.  The FFT results are organized with the real (positive) frequencies from 0 - N/2
 * and the complex (negative) frequencies from N/2 - N.
 * 
 * The negative frequencies go to the left of the center and the positive to the right.  The selected carrier
 * frequency is in the middle of the screen.  We effectively draw two graphs side  by side.
 * 
 * We tune the SDR from here.  The user can click the mouse on the display or use the arrow buttons.  If automatic tracking
 * is enabled then this module calculates a new frequency as though the user changed it.
 * 
 *
 */
@SuppressWarnings("serial")
public class FFTPanel extends JPanel implements Runnable, MouseListener {
	private static final float TRACK_SIGNAL_THRESHOLD = -80;
	Spacecraft fox;
	
	int fftSamples = 0;
	//double[] fftData = new double[fftSamples*2];
	
	private double[] psd = null;
	
	boolean running = true;
	boolean done = false;
	double centerFreqX = 145950;
	//int selectedBin = 0; // this is the actual FFT bin, with negative and positve freq flipped
	int selection = 0; // this is the bin that the user clicked on, which runs from left to right
	boolean showFilteredAudio = false;

	int sideBorder = 2 * Config.graphAxisFontSize;
	int topBorder = Config.graphAxisFontSize;
	int labelWidth = 4 * Config.graphAxisFontSize;

	int zoomFactor = 1;
	public static final int MAX_ZOOM_FACTOR = 10;
	public static final int MIN_ZOOM_FACTOR = 1;
	
	int graphWidth;
	int graphHeight;
	
	Color graphColor = Config.AMSAT_RED;
	Color graphAxisColor = Color.BLACK;
	Color graphTextColor = Color.DARK_GRAY;
	
	SourceIQ iqSource;

	RfData rfData;
	boolean liveData = false; // true if we have not received a NULL buffer from the decoder.
	int tuneDelay = 0;
	int TUNE_THRESHOLD = 100; // 30 = 1 second, 3 = 100 ms tune time.  Delay only used when DECODE, ie locked on, unless in PSK mode
	JLabel title;
	
	FFTPanel() {
		title = new JLabel();
		add(title);
		addMouseListener(this);
		String TUNE_LEFT = "left";
		String TUNE_RIGHT = "right";
		InputMap inMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inMap.put(KeyStroke.getKeyStroke("LEFT"), TUNE_LEFT);
		inMap.put(KeyStroke.getKeyStroke("RIGHT"), TUNE_RIGHT);
		ActionMap actMap = this.getActionMap();
		actMap.put(TUNE_LEFT, new AbstractAction() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	            iqSource.decSelectedFrequency();    
//	        	int selectedBin = iqSource.getSelectedBin();
//	        	System.out.println("TUNE LEFT from " + selectedBin);
//	                selectedBin = selectedBin-1;
//	                if (selectedBin < 0) selectedBin = SourceIQ.FFT_SAMPLES;
//	                if (selectedBin > SourceIQ.FFT_SAMPLES) selectedBin = 0;
//	                iqSource.setSelectedBin(selectedBin);
//	               printBin();
	        }
	    });
		actMap.put(TUNE_RIGHT, new AbstractAction() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	        	iqSource.incSelectedFrequency();
	          //      System.out.println("TUNE RIGHT");
//	        	int selectedBin = iqSource.getSelectedBin();
//
//	                selectedBin = selectedBin+1;
//	                if (selectedBin < 0) selectedBin = SourceIQ.FFT_SAMPLES;
//	                if (selectedBin > SourceIQ.FFT_SAMPLES) selectedBin = 0;
//	                iqSource.setSelectedBin(selectedBin);
//	                printBin();
	        }
	    });
	}
	
	public void zoomIn() {
		zoomFactor++;
		if (zoomFactor > MAX_ZOOM_FACTOR)
			zoomFactor = MAX_ZOOM_FACTOR;
		System.err.println(zoomFactor);
	}
	
	public void zoomOut() {
		zoomFactor--;
		if (zoomFactor < MIN_ZOOM_FACTOR)
			zoomFactor = MIN_ZOOM_FACTOR;
		System.err.println(zoomFactor);
	}
	
	@SuppressWarnings("unused")
	private void printBin() {
		int freq=0;
    	int selectedBin = iqSource.getSelectedBin();

		 if (selectedBin < SourceIQ.FFT_SAMPLES/2)
         	freq= 192000*selectedBin/SourceIQ.FFT_SAMPLES;
         else
         	freq = 96000-192000*(selectedBin-SourceIQ.FFT_SAMPLES/2)/SourceIQ.FFT_SAMPLES;
		 
		 double cycles = getCycles();
		 System.out.println("fft bin "+selectedBin + " freq: " + freq + " cyc: " + cycles);
	}
	
	public void stopProcessing() { 
		running = false;
		//source.drain();
	}
	
	private void init() {
		fftSamples = SourceIQ.FFT_SAMPLES;
		done = false;
		running = true;
		psd = new double[fftSamples+1];
		title.setText("FFT: " +  fftSamples);
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("FFTPanel");
		double[] buffer = null;
		while(running) {
			if (iqSource != null) {
				if (fftSamples != SourceIQ.FFT_SAMPLES) {
					fftSamples = SourceIQ.FFT_SAMPLES;
					psd = new double[fftSamples+1];
					title.setText("FFT: " +  fftSamples);
				}
				buffer = iqSource.getPowerSpectralDensity();
				centerFreqX = iqSource.getCenterFreqkHz();
				//selectedBin = Config.selectedBin;
				rfData = iqSource.getRfData();
			}
			try {
				Thread.sleep(1000/30); // 30Hz
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
			if (buffer != null) {
				psd = buffer;
				liveData = true;
				this.repaint();
			} else {
				liveData = false;
			}
			if (rfData != null) {
				if (!Config.foxTelemCalcsDoppler)
					retune();		
			}
		}
		done = true;
	}

	int avgBin = 0;
	int avgNum = 0;
	private void retune() {
		// auto tune
        int selectedBin = iqSource.getSelectedBin();
        int targetBin = 0;

		//if (rfData != null)
		//Log.println("TRACK: " + Config.trackSignal + " live: " + liveData + " sig: " + rfData.getAvg(RfData.PEAK_SIGNAL_IN_FILTER_WIDTH));
		if (iqSource.getMode() != SourceIQ.MODE_PSK_COSTAS)
		if (Config.trackSignal && liveData && rfData.getAvg(RfData.PEAK_SIGNAL_IN_FILTER_WIDTH) > TRACK_SIGNAL_THRESHOLD) {
			//if (Config.passManager.getState() == PassManager.DECODE || 
			//		Config.passManager.getState() == PassManager.ANALYZE ||
			//		Config.passManager.getState() == PassManager.FADED)
				targetBin = rfData.getBinOfPeakSignalInFilterWidth();  // peak is the strongest signal in the filter width
			//else
			//	targetBin = rfData.getBinOfStrongestSignalInSatBand(); // strongest is the strongest signal in the sat band

			avgBin = avgBin + targetBin;
			avgNum++;
			
			if (iqSource.getMode() == SourceIQ.MODE_PSK_NC) {
				TUNE_THRESHOLD = 180; // 6 second
				if (Config.passManager.getState() == PassManager.FADED) 
					tuneDelay = 0; // dont tune
				else
					tuneDelay++; // tune slowly
			} else {
				TUNE_THRESHOLD = 100; // ~3 second average time
				if (Config.passManager.getState() == PassManager.DECODE ) {
					tuneDelay++;
				} else if (Config.passManager.getState() == PassManager.ANALYZE ) {
					tuneDelay += 20;
				} else if (Config.passManager.getState() == PassManager.FADED) {
					// Don't tune
					tuneDelay = 0;
				} else {
					// Tune at maximum speed
					//tuneDelay = TUNE_THRESHOLD;
					tuneDelay = TUNE_THRESHOLD;
				} 
			}
			if (tuneDelay >= TUNE_THRESHOLD) {
				targetBin = avgBin/avgNum;
				avgNum = 0;
				avgBin = 0;
				tuneDelay = 0;
				// move half the distance to the bin

				//if ((selectedBin > Config.fromBin && selectedBin < Config.toBin) && (targetBin > Config.fromBin && targetBin < Config.toBin)) {

				int move = targetBin - selectedBin;
				//System.out.println("MOVE: "+ move);

				/*
				// THIS NEEDS TO UNDERSTAND THE WRAP OF THE BINS AROUND ZERO!
				// PERHAPS CONVERT BIN TO FREQ.  ADJUST FREQ.  THEN CONVERT BACK...
				long targetFreq = iqSource.getFrequencyFromBin(targetBin);
				long currentFreq = iqSource.getFrequencyFromBin(selectedBin);

				long move = currentFreq-targetFreq;
				Log.println("MOVE: "+ move);
				if (targetFreq < currentFreq) {
					if (move < -100)
						currentFreq -= 50;
					else
						if (move < -25)
							currentFreq -= 12;
						else if (move < -5)
							currentFreq -= 5;
						else if (move < -2)
							currentFreq -= 2;
						else
							currentFreq--;
				}
				if (targetFreq > currentFreq) {
					if (move > 100)
						currentFreq += 50;
					else if (move > 25)
						currentFreq += 12;
					else if (move > 5)
						currentFreq += 5;
					else if (move > 2)
						currentFreq += 2;
					else
						currentFreq++;
				}
				selectedBin = iqSource.getBinFromFreqHz(currentFreq);
				 */
				if (targetBin < selectedBin) {
					if (move < -100)
						selectedBin -= 50;
					else
						if (move < -25)
							selectedBin -= 12;
						else if (move < -5)
							selectedBin -= 5;
						else if (move < -2)
							selectedBin -= 2;
						else
							selectedBin--;
				}
				if (targetBin > selectedBin) {
					if (move > 100)
						selectedBin += 50;
					else if (move > 25)
						selectedBin += 12;
					else if (move > 5)
						selectedBin += 5;
					else if (move > 2)
						selectedBin += 2;
					else
						selectedBin++;
				}

				if (targetBin != 0) // to avoid startup timing issue
					if (Config.findSignal) {
						//Log.println("FS TUNE to: " + selectedBin + " from: "+ Config.selectedBin + " range: " + Config.fromBin + " - " + Config.toBin);
						if ((selectedBin > Config.fromBin && selectedBin < Config.toBin) && (targetBin > Config.fromBin && targetBin < Config.toBin)) {
							iqSource.setSelectedBin(targetBin);
							//Config.selectedBin = targetBin;
						}
					} else {
						//Log.println("TUNE to: " + selectedBin + " from: "+ Config.selectedBin + " range: " + Config.fromBin + " - " + Config.toBin);
						iqSource.setSelectedBin(selectedBin);
						//Config.selectedBin = selectedBin;
					}
			}

		}
	}
		
	public static int littleEndian2(byte b[]) {
		byte b1 = b[0];
		byte b2 = b[1];
		int value =  ((b2 & 0xff) << 8)
				| ((b1 & 0xff) << 0);
		if (value > (32768-1)) value = -65536 + value;
		return value;
	}

	private int getSelectionFromBin(int bin) {
		int selection;
		if (bin < fftSamples/2)
			selection = bin + fftSamples/2;
		else
			selection = bin - fftSamples/2;

		return selection;
	}
		
	public void paintComponent(Graphics g) {				
		super.paintComponent( g ); // call superclass's paintComponent  
		if (iqSource == null) return;
		
		sideBorder = 3 * Config.graphAxisFontSize;
		topBorder = Config.graphAxisFontSize;
		labelWidth = 4 * Config.graphAxisFontSize;

		if (!running) return;
		if (psd == null) return;
		Graphics2D g2 = ( Graphics2D ) g; // cast g to Graphics2D  
		g2.setColor(graphAxisColor);

		graphHeight = getHeight() - topBorder*2;
		graphWidth = getWidth() - sideBorder*2; // width of entire graph
		
		
		int minTimeValue = (int) (centerFreqX-iqSource.IQ_SAMPLE_RATE/2000);//96;
		int maxTimeValue = (int) (centerFreqX+iqSource.IQ_SAMPLE_RATE/2000);//96;
		int numberOfTimeLabels = graphWidth/labelWidth;
		int zeroPoint = graphHeight;
		
		if (zoomFactor != 1) {
			// we zoom around the tuned frequency
			
		}
		
		float maxValue = 10;
		float minValue = -120;

		int labelHeight = 14;
		int sideLabel = 3;
		// calculate number of labels we need on vertical axis
		int numberOfLabels = graphHeight/labelHeight;
		
		// calculate the label step size
		double[] labels = GraphPanel.calcAxisInterval(minValue, maxValue, numberOfLabels, false);
		// check the actual number
		numberOfLabels = labels.length;
		
		DecimalFormat f1 = new DecimalFormat("0.0");
		DecimalFormat f2 = new DecimalFormat("0");
	
		// Draw vertical axis - always in the same place
		g2.drawLine(sideBorder, getHeight()-topBorder, sideBorder, 0);
		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));

		for (int v=0; v < numberOfLabels; v++) {
			
			int pos = getRatioPosition(minValue, maxValue, labels[v], graphHeight);
			pos = graphHeight-pos;
			String s = null;
			if (labels[v] == Math.round(labels[v]))
				s = f2.format(labels[v]);
			else
				s = f1.format(labels[v]);
			if (v < numberOfLabels-1 && !(v == 0 && pos > graphHeight)) {
				g2.setColor(graphTextColor);
				g.drawString(s, sideLabel, pos+topBorder+4); // add 4 to line up with tick line
				g2.setColor(graphAxisColor);
				g.drawLine(sideBorder-5, pos+topBorder, sideBorder+5, pos+topBorder);
			}
		}
		
		if (iqSource != null) {
			// Draw the current selected frequency to decode
			// Only show half the filter width because of the taper of the filter shape
			//if (iqSource.getMode() == SourceIQ.MODE_PSK) {
			//	int bin  = iqSource.getBinFromOffsetFreqHz((long) iqSource.getCostasFrequency());
			//	selection = getSelectionFromBin(bin);
			//} else
				//selection = getSelectionFromBin(iqSource.getSelectedBin());
				selection = getSelectionFromBin(iqSource.getSelectedBin());

			int c = getRatioPosition(0, fftSamples, selection, graphWidth);
			int lower = getRatioPosition(0, fftSamples, selection-iqSource.getFilterWidth()/2, graphWidth);
			int upper = getRatioPosition(0, fftSamples, selection+iqSource.getFilterWidth()/2, graphWidth);


			g2.setColor(graphAxisColor);
			g2.drawLine(c+sideBorder, topBorder, c+sideBorder, zeroPoint);
			
			// draw line either side of signal
			g2.setColor(Color.gray);
			g2.drawLine(lower+sideBorder, topBorder, lower+sideBorder, zeroPoint);
			g2.drawLine(upper+sideBorder, topBorder, upper+sideBorder, zeroPoint);

			
			if (Config.findSignal || Config.foxTelemCalcsDoppler) {

				if (Config.findSignal)
					if (fox != null) {
						g.drawString(Config.passManager.getStateName() + ": "+fox.name, graphWidth-5*Config.graphAxisFontSize, 4*Config.graphAxisFontSize  );
					} else
						g.drawString("Scanning..", graphWidth-5*Config.graphAxisFontSize, 4*Config.graphAxisFontSize );
				
				for (int s=0; s < Config.satManager.spacecraftList.size(); s++) {
					Spacecraft sat = Config.satManager.spacecraftList.get(s);
					if (sat.track) {
						int fromSatBin = iqSource.getBinFromFreqHz((long) (sat.minFreqBoundkHz*1000));
						int toSatBin = iqSource.getBinFromFreqHz((long) (sat.maxFreqBoundkHz*1000));
					
						if (fromSatBin > SourceIQ.FFT_SAMPLES/2 && toSatBin < SourceIQ.FFT_SAMPLES/2) {
							toSatBin = 0;
						}
						
						g2.setColor(Config.PURPLE);

						int upperSelection = getSelectionFromBin(toSatBin);
						int lowerSelection = getSelectionFromBin(fromSatBin);

						if (upperSelection != lowerSelection) {
							int c1 = getRatioPosition(0, fftSamples, upperSelection, graphWidth);
							g2.drawLine(c1+sideBorder, topBorder+5, c1+sideBorder, zeroPoint);
							int c2 = getRatioPosition(0, fftSamples, lowerSelection, graphWidth);
							g2.drawLine(c2+sideBorder, topBorder+5, c2+sideBorder, zeroPoint);
							int c3 = (c1 + c2)/2;
							c3 = c3 - sat.name.length()/3*Config.graphAxisFontSize;
							g.drawString(sat.name, c3+sideBorder, topBorder + 15 );
						}
					}
				}
			}
			
			if (iqSource.getMode() == SourceIQ.MODE_PSK_COSTAS) {
				int lock = (int) Math.round(iqSource.getLockLevel());
				if (lock > SourceIQ.LOCK_LEVEL_THRESHOLD) {
					g2.setColor(Color.BLUE);
//					g.drawString("Locked", graphWidth-5*Config.graphAxisFontSize, (int) ( graphHeight/2+ 3*Config.graphAxisFontSize)  );
					g.drawString("Locked "+lock, graphWidth-5*Config.graphAxisFontSize, (int) ( graphHeight/2+ 2*Config.graphAxisFontSize)  );
				} else {
					g2.setColor(Color.gray);
					g.drawString("Lock: " + lock, graphWidth-5*Config.graphAxisFontSize, (int) ( graphHeight/2+ 2*Config.graphAxisFontSize)  );
				}
				g2.setColor(Color.gray);
				g.drawString("Costas Error: " + Math.round(iqSource.getError()*1E3), graphWidth-5*Config.graphAxisFontSize, (int) ( graphHeight/2+ Config.graphAxisFontSize)  );
				g.drawString("Carrier: " + Math.round(iqSource.getCostasFrequency()), graphWidth-5*Config.graphAxisFontSize, (int) ( graphHeight/2 )  );

			}
			
			if (rfData != null) {
				g2.setColor(Config.AMSAT_BLUE);
				//int width = 10;
				int posPeakSignalInFilterWidth = getRatioPosition(maxValue, minValue, rfData.getAvg(RfData.AVGSIG_IN_FILTER_WIDTH)+topBorder, graphHeight);
//				int peakSignalInFilterWidth = getRatioPosition(minValue, maxValue, -60, graphHeight);
			//	posPeakSignalInFilterWidth=posPeakSignalInFilterWidth-topBorder;
				
				int binOfPeakSignalInFilterWidth = 0;
				if (rfData.getBinOfPeakSignalInFilterWidth() < fftSamples/2) {
					binOfPeakSignalInFilterWidth = getRatioPosition(0, fftSamples/2, rfData.getBinOfPeakSignalInFilterWidth(), graphWidth/2);
					binOfPeakSignalInFilterWidth = binOfPeakSignalInFilterWidth + sideBorder + graphWidth/2;
				} else {
					binOfPeakSignalInFilterWidth = getRatioPosition(0, fftSamples/2, rfData.getBinOfPeakSignalInFilterWidth()-fftSamples/2, graphWidth/2);
					binOfPeakSignalInFilterWidth = binOfPeakSignalInFilterWidth + sideBorder;
				}
	
				double snrStrongestSigInSatBand = GraphPanel.roundToSignificantFigures(rfData.rfStrongestSigSNRInSatBand,3);
//				double valueOfpeakSignalInFilterWidth = GraphPanel.roundToSignificantFigures(rfData.getAvg(RfData.PEAK_SIGNAL_IN_FILTER_WIDTH),3);

				double snr = GraphPanel.roundToSignificantFigures(rfData.rfSNRInFilterWidth,3);
				String s = Double.toString(snr) + "";
				String ss = Double.toString(snrStrongestSigInSatBand) + "";
				//long f = iqSource.getFrequencyFromBin(iqSource.getSelectedBin());  //rfData.getPeakFrequency();
				double f = ( iqSource.getCenterFreqkHz()*1000 + iqSource.getSelectedFrequency());  //rfData.getPeakFrequency();
				DecimalFormat d3 = new DecimalFormat("0.000");
				g.drawString("| " /*+ rfData.getBinOfPeakSignal()*/ , binOfPeakSignalInFilterWidth, posPeakSignalInFilterWidth  );

				g2.drawLine(binOfPeakSignalInFilterWidth-5 , (int)posPeakSignalInFilterWidth-3, binOfPeakSignalInFilterWidth+5, (int)posPeakSignalInFilterWidth-3);
				if (Config.showSNR) 
					g.drawString("snr: " + s + "dB", binOfPeakSignalInFilterWidth+10, posPeakSignalInFilterWidth  );
				else
					g.drawString("" + ss + "dB", binOfPeakSignalInFilterWidth+10, posPeakSignalInFilterWidth  );
				g.drawString("Freq:"+d3.format(f/1000), graphWidth-5*Config.graphAxisFontSize, 2*Config.graphAxisFontSize  );

				if (Config.findSignal && Config.debugSignalFinder) {
					int strongestSigInSatBand = getRatioPosition(minValue, maxValue, rfData.getAvg(RfData.STRONGEST_SIGNAL_IN_SAT_BAND), graphHeight);
					strongestSigInSatBand=graphHeight-strongestSigInSatBand-topBorder;
					
					int binOfStrongestSigInSatBand = 0;
					if (rfData.getBinOfStrongestSignalInSatBand() < fftSamples/2) {
						binOfStrongestSigInSatBand = getRatioPosition(0, fftSamples/2, rfData.getBinOfStrongestSignalInSatBand(), graphWidth/2);
						binOfStrongestSigInSatBand = binOfStrongestSigInSatBand + sideBorder + graphWidth/2;
					} else {
						binOfStrongestSigInSatBand = getRatioPosition(0, fftSamples/2, rfData.getBinOfStrongestSignalInSatBand()-fftSamples/2, graphWidth/2);
						binOfStrongestSigInSatBand = binOfStrongestSigInSatBand + sideBorder;
					}
					
					g.drawString("^ " , binOfStrongestSigInSatBand, strongestSigInSatBand - 5  );
				}
				
			} else {
				Log.println("RF DATA NULL");
			}
				
		}

		
		int lastx = sideBorder+1; 
		int lasty = graphHeight;
		int x = 0;
		int y = 0;
		//int skip = 0;
		g2.setColor(graphColor);
		
		//192000/2 samples is too long for any window so we skip some of it
		int stepSize = Math.round((fftSamples - 1)/graphWidth);
		
		// Draw the graph, one half at a time
		for (int i=fftSamples/2; i< (fftSamples); i+= stepSize) {
			
			
			x = getRatioPosition(0, fftSamples/2, i-fftSamples/2, graphWidth/2);
			x = x + sideBorder;
			
			if (i >= psd.length) return; // the FFT was resized and we are in the middle of a paint
			y = getRatioPosition(minValue, maxValue, psd[i], graphHeight);
			
			// psd 
			y=graphHeight-y+topBorder;
			if (i == 1) {
				lastx=x;
				lasty=y;
			}
			g2.drawLine(lastx, lasty, x, y);
			lastx = x;
			lasty = y;
			
			//g2.drawLine((10+(pos*2-1)), (int)(getHeight()- baseline-(psd[x-1]*gain)), 10+pos*2, (int)(getHeight()-baseline -(psd[x]*gain)));
		
		}
		
		// We want 0 - fftSamples/2 to get the real part		
		for (int i=1; i< (fftSamples/2); i+= stepSize) {
						
			x = getRatioPosition(0, fftSamples/2, i, graphWidth/2);
			x = x + sideBorder + graphWidth/2;

			if (i >= psd.length) return; // the FFT was resized and we are in the middle of a paint
			y = getRatioPosition(minValue, maxValue, psd[i], graphHeight);
			
			// psd 
			y=graphHeight-y+topBorder;
			if (i == 1) {
				lastx=x;
				lasty=y;
			}
			g2.drawLine(lastx, lasty, x, y);
			lastx = x;
			lasty = y;
			
			//g2.drawLine((10+(pos*2-1)), (int)(getHeight()- baseline-(psd[x-1]*gain)), 10+pos*2, (int)(getHeight()-baseline -(psd[x]*gain)));
		
		}
		
		// Draw the horizontal axis
		double[] freqlabels = GraphPanel.calcAxisInterval(minTimeValue, maxTimeValue, numberOfTimeLabels, false);

		DecimalFormat d = new DecimalFormat("0");
		for (int v=0; v < freqlabels.length; v++) {
			int timepos = getRatioPosition(minTimeValue, maxTimeValue, freqlabels[v], graphWidth);

			// dont draw the label if we are too near the start or end
			if ((timepos) > 2 && (graphWidth - timepos) > labelWidth/6) {
				String s = d.format(freqlabels[v]);

				g2.setColor(graphTextColor);
				g.drawString(s, timepos+sideBorder+2-labelWidth/2, zeroPoint+Config.graphAxisFontSize );

				g2.setColor(graphAxisColor);
				g.drawLine(timepos+sideBorder, zeroPoint, timepos+sideBorder, zeroPoint+5);
			}
		}
		
		g2.setColor(graphAxisColor);
		g2.drawLine(0, zeroPoint, getWidth(), zeroPoint);

		
	}

	public void setFox(Spacecraft spacecraft) {
		fox = spacecraft;
	}
	
	public void startProcessing(SourceIQ d) {
		iqSource = d;
		init();
		//title.setText("Sample rate: " +  iqSource.IQ_SAMPLE_RATE);
		running = true;
	}
	

	/**
	 * Given a minimum and maximum and the length of a dimension, usually in pixels, return the pixel position when passed
	 * a value between the min and max
	 * @param min
	 * @param max
	 * @param value
	 * @param dimension
	 * @return
	 */
	private int getRatioPosition(double min, double max, double value, int dimension) {
		double ratio = (max - value) / (max - min);
		int position = (int)Math.round(dimension * ratio);
		return dimension-position;
	}

	private double getCycles() {
		int selectedBin = iqSource.getSelectedBin();

		double binBW = 192000f / 2 / SourceIQ.FFT_SAMPLES; // nyquist freq / fft length
		double freq = (double)selectedBin * binBW;
		double samples = 192000f / freq; // number of samples in one period of this freq
		double cycles = (double)SourceIQ.FFT_SAMPLES / samples; // how many complete cycles this freq makes in the fft length
		return cycles;
	}

	@SuppressWarnings("unused")
	private int getRequiredBin(int bin) {
		double binBW = 192000f / 2 / SourceIQ.FFT_SAMPLES; // nyquist freq / fft length
		double freq = (double)bin * binBW;
		
		//int actBin = (int) (Math.round(SourceIQ.FFT_SAMPLES * freq / (30 * 192000)) * 30);
		int dc = 0;
		if (bin < SourceIQ.FFT_SAMPLES/2) dc = +2; else dc = -1;
		int actBin = (int) (Math.round(bin / 30) * 30 + dc) ; // -1 because we translate to the first bin after DC??
		return actBin;
	}

	
	@Override
	public void mouseClicked(MouseEvent e) {
    //	int selectedBin = iqSource.getSelectedBin();
    	int selectedBin = iqSource.getSelectedBin();

		int x=e.getX();
	    //int y=e.getY();
	    x = x - sideBorder;
		selection = getRatioPosition(0, graphWidth, x, fftSamples );
		if (selection >= fftSamples/2) 
			selectedBin = selection - fftSamples/2;
		else
			selectedBin = selection + fftSamples/2;
		System.out.println(x+" is fft bin "+selectedBin);//these co-ords are relative to the component
	
		//double cyc = getCycles();
//		System.out.println("Trying bin: " + selectedBin);
	//	selectedBin = getRequiredBin(selectedBin);
//		System.out.println("Trying bin: " + selectedBin);
				
		iqSource.setSelectedBin(selectedBin);
//		Config.selectedBin = selectedBin;
		if (rfData != null)
			rfData.reset(); // reset the average calcs so the UI is more responsive
//		printBin();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		
		
	}
}
