package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import javax.swing.JLabel;

import common.Config;
import common.Log;
import decoder.Decoder;
import decoder.SourceIQ;
import decoder.FoxBPSK.FoxBPSKCostasDecoder;
import decoder.FoxBPSK.FoxBPSKDecoder;
import decoder.FoxBPSK.FoxBPSKDotProdDecoder;

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
 * A panel that displays a graph of the data that it reads from the decoder
 * 
 */
@SuppressWarnings("serial")
public class AudioGraphPanel extends JPanel implements Runnable {

//	JLabel lblIqbalance;
	String fileName = null;
	boolean running = true;
	boolean done = false;
	int centerFreqX = 220;
	Decoder foxDecoder;
	double[] audioData = null;
	double[] pskAudioData = null;
	double[] pskQAudioData = null;
	int AUDIO_DATA_SIZE = 1024;
	int currentDataPosition = 0;
	JLabel sample;
	int s = 0;
	JLabel title;
	int bitValue = 10;
	
	boolean showFilteredAudio = false;
	
	long bufferCapacityAvg;
	int bufferCapacitySample;
	final int BUFFER_CAP_SAMPLE_NO = 100;
	int bufferCapacity;
	
	public AudioGraphPanel() {
		//decoder = d;
		title = new JLabel("Sample rate: ");// + Integer.toString(decoder.getCurrentSampleRate()));
		title.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		add(title);
		//sample  = new JLabel("sample: " + 0);
		//add(sample);
		
		/* to debug costas loop
		String TUNE_LEFT = "up";
		String TUNE_RIGHT = "down";
		String TUNE_LEFT_MILI = "q";
		String TUNE_RIGHT_MILI = "a";
		InputMap inMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inMap.put(KeyStroke.getKeyStroke("UP"), TUNE_LEFT);
		inMap.put(KeyStroke.getKeyStroke("DOWN"), TUNE_RIGHT);
		inMap.put(KeyStroke.getKeyStroke('q'), TUNE_LEFT_MILI);
		inMap.put(KeyStroke.getKeyStroke('a'), TUNE_RIGHT_MILI);
		ActionMap actMap = this.getActionMap();
		actMap.put(TUNE_LEFT, new AbstractAction() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	                System.out.println("TUNE UP");
	        	((FoxBPSKDecoder)foxDecoder).incFreq();
	        }
	    });
		actMap.put(TUNE_RIGHT, new AbstractAction() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	                System.out.println("TUNE DOWN");
		        	((FoxBPSKDecoder)foxDecoder).decFreq();
	        	
	        }
	    });
		actMap.put(TUNE_LEFT_MILI, new AbstractAction() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	                System.out.println("TUNE UP MILLI");
	        	((FoxBPSKDecoder)foxDecoder).incMiliFreq();
	        }
	    });
		actMap.put(TUNE_RIGHT_MILI, new AbstractAction() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	                System.out.println("TUNE DOWN MILLI");
		        	((FoxBPSKDecoder)foxDecoder).decMiliFreq();
	        	
	        }
	    });
	    */
	}
	
	public void updateFont() {
		title.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
	}
	
	public void showFilteredAudio() {
		showFilteredAudio = true;
	}
	
	public void showUnFilteredAudio() {
		showFilteredAudio = false;
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("AudioGraphPanel");
		done = false;
		running = true;
//		int bytesRead =0;
//		int decimate=0;
		currentDataPosition = 0;
		//int border = 5;
		//int AUDIO_DATA_SIZE = getWidth() - border*2;
		
		while(running) {
			
			try {
				Thread.sleep(1000/60); // approx 1/60 sec refresh
			} catch (InterruptedException e) {
				Log.println("ERROR: Audiograph thread interrupted");
				//e.printStackTrace();
			} 
			
			double[] buffer;
			if (foxDecoder != null) {
				if (showFilteredAudio)
					buffer = foxDecoder.getFilteredData();
				else
					buffer = foxDecoder.getAudioData();
			
				if (buffer != null) {
					audioData = buffer;		
//					if (foxDecoder instanceof FoxBPSKDecoder) {
//						pskAudioData = ((FoxBPSKDecoder)foxDecoder).getBasebandData();	
//						pskQAudioData = ((FoxBPSKCostasDecoder)foxDecoder).getBasebandQData();	
//					}
					if (foxDecoder instanceof FoxBPSKCostasDecoder ) {
						pskAudioData = ((FoxBPSKCostasDecoder)foxDecoder).getBasebandData();
						pskQAudioData = ((FoxBPSKCostasDecoder)foxDecoder).getBasebandQData();	
					}
					if (foxDecoder instanceof FoxBPSKDotProdDecoder) {
						pskAudioData = ((FoxBPSKDotProdDecoder)foxDecoder).getBasebandData();
						pskQAudioData = ((FoxBPSKDotProdDecoder)foxDecoder).getBasebandQData();	
					}
				}

				this.repaint();
			}
		}			
	}
	
	public void startProcessing(Decoder decoder1) {
		foxDecoder = decoder1;
		if (foxDecoder != null)
			title.setText("Sample rate: " + Integer.toString(foxDecoder.getCurrentSampleRate()) + " | Symbols: " + foxDecoder.getSampleWindowLength());
		
		running = true;
	}
	
	public void stopProcessing() { 
		running = false;
		//source.drain();
	}
	
	/*
	 * Draw on a panel, where x is horizontal from left to right and y is vertical from top to bottom
	 * Draw a line segment for each sample, from the previous sample
	 * drawline x1,y1,y2,y2
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent( g ); // call superclass's paintComponent  
		Graphics2D g2 = ( Graphics2D ) g; // cast g to Graphics2D  
		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		// Have 5 pix border
		int border = 5;
		int graphHeight = getHeight() - border;
		int graphWidth = getWidth() - border*2;
		
		// Draw baseline with enough space for text under it
		//g2.drawLine(0, graphHeight-border, graphWidth, graphHeight-border);
		// Draw vertical axis
		g2.drawLine(border*2, getHeight()-border, border*2, border*4);
	
		int lastx = border*2+1; 
		int lasty = graphHeight/2;
		int x = border*2+1;
		int lastx2 = border*2+1; 
		int lasty2 = graphHeight/2;
		int x2 = border*2+1;
		
		int lastx3 = border*2+1; 
		int lasty3 = graphHeight/2;
		int x3 = border*2+1;
		
		
		
		int stepSize = 1;
		//int spaceSize = 1;
		try {	
		if (audioData != null && audioData.length > 0) {
			if (audioData.length > graphWidth) {
				stepSize = (int) Math.round((audioData.length)/graphWidth);
			} else {
				// we leave step size at 1 and plot all of the points, but space them out.
				//spaceSize = graphWidth/(audioData.length);
			}
			
			if (stepSize <= 0) stepSize = 1;
			int bucketPositionCount = 0;
			int bitCount = 0;
			for (int i=0; i < audioData.length-stepSize; i+=stepSize) { //// TODO stepSize was 2.  That is wrong for PSK. Not stereo.  Is it ever right?
				
				// data is stereo, but we want to decimate before display

				//int value = SourceAudio.getIntFromDouble(audioData[i]);
				if ( pskAudioData != null && i < pskAudioData.length && 
						(foxDecoder instanceof FoxBPSKDecoder || foxDecoder instanceof FoxBPSKDotProdDecoder ||
						    (foxDecoder instanceof FoxBPSKCostasDecoder && 
								 ((FoxBPSKCostasDecoder) foxDecoder).mode == FoxBPSKCostasDecoder.PSK_MODE
							) 
						)
					)
					g2.setColor(Color.BLACK);
				else
					g2.setColor(Color.BLUE);
				//x = (i*j/(Decoder.SAMPLE_WINDOW_LENGTH*Decoder.BUCKET_SIZE))*graphWidth;
				x = border*2 + i*(graphWidth-border*2)/audioData.length;

				
				if (Config.debugValues && foxDecoder != null) {
					if (pskAudioData[i] > 1000) {
					//	pskAudioData[i] = pskAudioData[i] - 10000; // rescale
						bitValue = 1;
					}
					if (pskAudioData[i] < -1000) {
					//	pskAudioData[i] = pskAudioData[i] + 10000;			
						bitValue = 0;
					}
					// If we are on a bucket boundry, draw a line and label the bit
					// We have foxDecoder.SAMPLE_WINDOW_LENGTH buckets
					// The audio data has decoder.bucketSize samples per bucket
					bucketPositionCount +=stepSize;
					if (bucketPositionCount >= foxDecoder.getBucketSize()) {
						g2.setColor(Color.BLACK);
						g2.drawLine(x, 0, x, graphHeight);
						g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize-2));
						g.drawString(""+(Config.windowStartBit+bitCount), x-25, graphHeight-20 );
						if (foxDecoder.middleSample[bitCount])
							bitValue = 1;
						else
							bitValue = 0;
						g2.setColor(Color.RED);
						g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize*2));
						g.drawString(""+bitValue,x-25, graphHeight-100);
							
						g2.setColor(Color.BLACK);
						g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
						bucketPositionCount = 0;
						bitCount++;
					}
				}
				// Calculate a value between -1 and + 1 and scale it to the graph height.  Center in middle of graph
				double y = 0.0d;
				
				if ((foxDecoder instanceof FoxBPSKDecoder || foxDecoder instanceof FoxBPSKDotProdDecoder || foxDecoder instanceof FoxBPSKCostasDecoder && 
						 ((FoxBPSKCostasDecoder) foxDecoder).mode == FoxBPSKCostasDecoder.PSK_MODE) || Config.debugValues)
					y = graphHeight/4+graphHeight/2.5*audioData[i] + border;
				else
					y = graphHeight/2+graphHeight/2.5*audioData[i] + border;
				//int y = 100;
				g2.drawLine(lastx, lasty, x, (int)y);
				lastx = x;
				lasty = (int)y;

				if ((foxDecoder instanceof FoxBPSKDecoder || foxDecoder instanceof FoxBPSKDotProdDecoder || foxDecoder instanceof FoxBPSKCostasDecoder && 
						 ((FoxBPSKCostasDecoder) foxDecoder).mode == FoxBPSKCostasDecoder.PSK_MODE) ) {
					if (foxDecoder instanceof FoxBPSKCostasDecoder && (((FoxBPSKCostasDecoder) foxDecoder).mode == FoxBPSKCostasDecoder.PSK_MODE ) && pskAudioData != null && i < pskAudioData.length) {
						int lock = (int)Math.round(((FoxBPSKCostasDecoder)foxDecoder).getLockLevel());
						if (lock > SourceIQ.LOCK_LEVEL_THRESHOLD) {
							g2.setColor(Color.BLUE);
							g.drawString("Locked " + lock, graphWidth-7*Config.graphAxisFontSize, (int) ( graphHeight/2+ 3*Config.graphAxisFontSize)  );
						} else {
							g2.setColor(Color.gray);
							g.drawString("Lock: " + lock, graphWidth-7*Config.graphAxisFontSize, (int) ( graphHeight/2+ 3*Config.graphAxisFontSize)  );
						}
						g2.setColor(Color.gray);
						g.drawString("Costas Error: " + Math.round(((FoxBPSKCostasDecoder)foxDecoder).getError()*100), graphWidth-7*Config.graphAxisFontSize, (int) ( graphHeight/2+ 2*Config.graphAxisFontSize)  );
						g.drawString("Carrier: " + Math.round(((FoxBPSKCostasDecoder)foxDecoder).getFrequency()), graphWidth-7*Config.graphAxisFontSize, (int) ( graphHeight/2 + Config.graphAxisFontSize)  );
					}
					if (foxDecoder instanceof FoxBPSKDotProdDecoder) {
						g2.setColor(Color.gray);
						g.drawString("Carrier: " + Math.round(((FoxBPSKDotProdDecoder)foxDecoder).getFrequency()), graphWidth-7*Config.graphAxisFontSize, (int) ( graphHeight/2 + Config.graphAxisFontSize)  );
						g.drawString("Offset: " + (((FoxBPSKDotProdDecoder)foxDecoder).getOffset()), graphWidth-7*Config.graphAxisFontSize, (int) ( graphHeight/2+ 2*Config.graphAxisFontSize)  );
						
					}
					if (pskAudioData != null && pskAudioData.length > 0) {
						g2.setColor(Color.BLUE);
						x2 = border*2 + i*(graphWidth-border*2)/pskAudioData.length;

						// Calculate a value between -1 and + 1 and scale it to the graph height.  Center in middle of graph
						double y2 = 0;
						try {
						y2 = 3*graphHeight/4-graphHeight/6*pskAudioData[i] + border;  // 3/4 is because its centered at bottom quarter of graph. 
						} catch (Exception e) {
							// likely because we switched decoders in the middle of a paint
						}
						//int y = 100;
						g2.drawLine(lastx2, lasty2, x2, (int)y2);
						lastx2 = x2;
						lasty2 = (int)y2;

						if ((foxDecoder instanceof FoxBPSKDotProdDecoder || (foxDecoder instanceof FoxBPSKCostasDecoder 
								&& (((FoxBPSKCostasDecoder) foxDecoder).mode == FoxBPSKCostasDecoder.PSK_MODE ))) && pskAudioData != null && i < pskAudioData.length) {
							// 2nd trace
							g2.setColor(Color.RED);
							x3 = border*2 + i*(graphWidth-border*2)/pskQAudioData.length;

							// Calculate a value between -1 and + 1 and scale it to the graph height.  Center in middle of graph
							double y3 = 3*graphHeight/4-graphHeight/6*pskQAudioData[i] + border;  // 3/4 is because its centered at bottom quarter of graph. 
							//int y = 100;
							g2.drawLine(lastx3, lasty3, x3, (int)y3);
							lastx3 = x3;
							lasty3 = (int)y3;
						}
					}
				}

			}
		}
		g2.setColor(Color.GRAY);
		// Center (decode) line
		if (foxDecoder instanceof FoxBPSKDecoder || foxDecoder instanceof FoxBPSKDotProdDecoder || (foxDecoder instanceof FoxBPSKCostasDecoder  &&
				((FoxBPSKCostasDecoder) foxDecoder).mode == FoxBPSKCostasDecoder.PSK_MODE) || Config.debugValues) {
			g2.drawLine(0, graphHeight/4+border, graphWidth, graphHeight/4+border);
			g2.drawLine(0, 3*graphHeight/4+border, graphWidth, 3*graphHeight/4+border);
		} else
			g2.drawLine(0, graphHeight/2+border, graphWidth, graphHeight/2+border);
		
		if (Config.debugValues) {
			if (foxDecoder !=null)
				g.drawString("Window: "+Config.windowsProcessed, 20, 20 );
		}
		
		if (Config.debugAudioGlitches) {
			Runtime rt = Runtime.getRuntime();
			long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
			g.drawString("Mem: "+usedMB, 10, 20 );
			if (foxDecoder !=null)
				if (foxDecoder.getFilter() != null) {
					g.drawString("Gain: "+GraphPanel.roundToSignificantFigures(foxDecoder.getFilter().getGain(),4), 70, 20 );
					bufferCapacityAvg += foxDecoder.getAudioBufferCapacity();
					bufferCapacitySample++;
					if (bufferCapacitySample == BUFFER_CAP_SAMPLE_NO) {
						bufferCapacity = (int) (bufferCapacityAvg / bufferCapacitySample);
						bufferCapacitySample=0;
						bufferCapacityAvg = 0;
					}
					g.drawString("Size: "+foxDecoder.getAudioBufferSize() + 
							" Capacity: "+foxDecoder.getAudioBufferCapacity(), getWidth()-200, 20 );

				}
		}
		} catch (ClassCastException e) {
			// ignore - likely the decoder switched just as we were painting the screen
		}
		//sample.setText("sample: " + s++);
	}

	
}


