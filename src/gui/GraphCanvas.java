package gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.DecimalFormat;

import common.Config;
import common.Spacecraft;
import measure.SatMeasurementStore;
import predict.PositionCalcException;
import telemetry.BitArray;
import telemetry.BitArrayLayout;
import telemetry.Conversion;
import telemetry.ConversionStringLookUpTable;
import telemetry.FramePart;
import telemetry.FramePart;
import telemetry.PayloadStore;
import telemetry.RadiationPacket;
import uk.me.g4dpz.satellite.SatPos;

@SuppressWarnings("serial")
public abstract class GraphCanvas extends MapPanel {
	Spacecraft fox;
	double[][][] graphData = null;
	double[][][] graphData2 = null;
	String title = "Test Graph";
	GraphFrame graphFrame;
	protected String conversion;  
//	protected int payloadType; // This is RT, MAX, MIN, RAD, Measurement etc
	boolean drawGraph2 = false;
	Color graphAxisColor = Color.BLACK;
	Color graphTextColor = Color.DARK_GRAY;
	

	int topBorder = (int)(Config.graphAxisFontSize*2); //Config.graphAxisFontSize; // The distance from the top of the drawing surface to the graph.  The title goes in this space
	int bottomBorder = (int)(Config.graphAxisFontSize*2.5); // The distance from the bottom of the drawing surface to the graph
	int sideBorder = (int)(Config.graphAxisFontSize *5); // left side border.  The position that the axis is drawn and the graph starts
	int sideLabelOffset = (int)(Config.graphAxisFontSize *0.5); // offset before we draw a label on the vertical axis
	static int labelWidth = (int)(6 * Config.graphAxisFontSize);  // 40 was a bit too tight for 6 digit uptime
	static int labelHeight = (int)(Config.graphAxisFontSize * 1.4);

	protected static int MAX_TICKS = 4096/labelWidth;
	
	int freqOffset = 0;

	Graphics2D g2;
	Graphics g;

	GraphCanvas(String t, GraphFrame gf, Spacecraft fox2) {
		title = t;
		//this.fieldName = fieldName;
		graphFrame = gf;
		
		fox = fox2;
		
	}
	
	public void updateGraphData(BitArrayLayout lay, String by) {
		int showDialogThreshold = 999999;
		ProgressPanel fileProgress = null;
		if (graphFrame.SAMPLES > showDialogThreshold) {
			fileProgress = new ProgressPanel(Config.mainWindow, "Loading Spacecraft data, please wait ...", false);
			fileProgress.setVisible(true);
		}
		
		int totalFields = graphFrame.fieldName.length;
		if (graphFrame.fieldName2 != null)
			totalFields += graphFrame.fieldName2.length;
		boolean reverse=false;
		graphData = new double[graphFrame.fieldName.length][][];
		for (int i=0; i<graphFrame.fieldName.length; i++) {
			if (graphFrame.SAMPLES > showDialogThreshold)
				fileProgress.updateProgress((int)(100*i/totalFields));
			if (graphFrame.showLatest == GraphFrame.SHOW_LIVE)
				reverse=true;
		
			boolean includePositionData = false;
			if (lay.isWOD() || lay.isWODExperiment())
				includePositionData = true;
			
			if  (graphFrame.payloadType == SatMeasurementStore.RT_MEASUREMENT_TYPE) 
				graphData[i] = Config.payloadStore.getMeasurementGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, reverse);
			else if  (graphFrame.payloadType == SatMeasurementStore.PASS_MEASUREMENT_TYPE) 
				graphData[i] = Config.payloadStore.getPassMeasurementGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, reverse);
			else
				graphData[i] = Config.payloadStore.getGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, 
					graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, lay.name, includePositionData, reverse);
								
			if (graphFrame.plotType == GraphFrame.EARTH_PLOT)
				graphData[i] = addPositionData(graphData[i]);
		}
		
		graphData2 = null;
		if (graphFrame.fieldName2 != null && graphFrame.fieldName2.length > 0) {
			graphData2 = new double[graphFrame.fieldName2.length][][];
			for (int i=0; i<graphFrame.fieldName2.length; i++) {
				
				if (graphFrame.SAMPLES > showDialogThreshold)
					fileProgress.updateProgress((int)(100*i+graphFrame.fieldName.length/totalFields));
				
				if  (graphFrame.payloadType == SatMeasurementStore.RT_MEASUREMENT_TYPE) 
					graphData2[i] = Config.payloadStore.getMeasurementGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, reverse);
				else if  (graphFrame.payloadType == SatMeasurementStore.PASS_MEASUREMENT_TYPE) 
					graphData2[i] = Config.payloadStore.getPassMeasurementGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, reverse);
				else
					graphData2[i] = Config.payloadStore.getGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, 
						graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, lay.name, false, reverse);
		
			}
		}
		
		if (graphFrame.SAMPLES > showDialogThreshold)
			fileProgress.updateProgress(100);
		
		//System.err.println("-repaint by: " + by);
		if (graphData != null && graphData[0] != null)
			this.repaint();
	}

//	public void updateXGraphData(String by) {
//		int showDialogThreshold = 999999;
//		ProgressPanel fileProgress = null;
//		if (graphFrame.SAMPLES > showDialogThreshold) {
//			fileProgress = new ProgressPanel(Config.mainWindow, "Loading Spacecraft data, please wait ...", false);
//			fileProgress.setVisible(true);
//		}
//		
//		int totalFields = graphFrame.fieldName.length;
//		if (graphFrame.fieldName2 != null)
//			totalFields += graphFrame.fieldName2.length;
//		boolean reverse=false;
//		graphData = new double[graphFrame.fieldName.length][][];
//		for (int i=0; i<graphFrame.fieldName.length; i++) {
//			if (graphFrame.SAMPLES > showDialogThreshold)
//				fileProgress.updateProgress((int)(100*i/totalFields));
//			if (graphFrame.showLatest == GraphFrame.SHOW_LIVE)
//				reverse=true;
//			if (payloadType == FoxFramePart.TYPE_REAL_TIME)
//				graphData[i] = Config.payloadStore.getRtGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
//			else if (payloadType == FoxFramePart.TYPE_MAX_VALUES)
//				graphData[i] = Config.payloadStore.getMaxGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
//			else if (payloadType == FoxFramePart.TYPE_MIN_VALUES)
//				graphData[i] = Config.payloadStore.getMinGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
//			else if (payloadType == FoxFramePart.TYPE_RAD_EXP_DATA)
//				graphData[i] = Config.payloadStore.getGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, Spacecraft.RAD_LAYOUT, true, reverse);
//			else if (payloadType == FoxFramePart.TYPE_RAD_TELEM_DATA)
//				graphData[i] = Config.payloadStore.getRadTelemGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
//			else if (payloadType == FoxFramePart.TYPE_HERCI_SCIENCE_HEADER)
//				graphData[i] = Config.payloadStore.getHerciScienceHeaderGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
//			else if  (payloadType == SatMeasurementStore.RT_MEASUREMENT_TYPE) 
//				graphData[i] = Config.payloadStore.getMeasurementGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, reverse);
//			else if  (payloadType == SatMeasurementStore.PASS_MEASUREMENT_TYPE) 
//				graphData[i] = Config.payloadStore.getPassMeasurementGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, reverse);
//			else if  (payloadType == FoxFramePart.TYPE_WOD) 
//				graphData[i] = Config.payloadStore.getGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, Spacecraft.WOD_LAYOUT, true, reverse);
//			else if  (payloadType == FoxFramePart.TYPE_WOD_RAD_TELEM_DATA) 
//				graphData[i] = Config.payloadStore.getGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, Spacecraft.WOD_RAD2_LAYOUT, false, reverse);
//			else if (payloadType == FoxFramePart.TYPE_WOD_RAD)
//				graphData[i] = Config.payloadStore.getGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, Spacecraft.WOD_RAD_LAYOUT, true, reverse);
//			else if (payloadType == FoxFramePart.TYPE_UW_EXPERIMENT)
//				graphData[i] = Config.payloadStore.getGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, graphFrame.layout.name, true, reverse);
//				//graphData[i] = Config.payloadStore.getGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, Spacecraft.CAN_LAYOUT, true, reverse);
//			else if (payloadType == FoxFramePart.TYPE_UW_WOD_EXPERIMENT)
//				graphData[i] = Config.payloadStore.getGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, graphFrame.layout.name, true, reverse);
//				//graphData[i] = Config.payloadStore.getGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, Spacecraft.WOD_CAN_LAYOUT, true, reverse);
//			
//			if (graphFrame.plotType == GraphFrame.EARTH_PLOT)
//				graphData[i] = addPositionData(graphData[i]);
//		}
//
//		graphData2 = null;
//		if (graphFrame.fieldName2 != null && graphFrame.fieldName2.length > 0) {
//			graphData2 = new double[graphFrame.fieldName2.length][][];
//			for (int i=0; i<graphFrame.fieldName2.length; i++) {
//				if (graphFrame.SAMPLES > showDialogThreshold)
//					fileProgress.updateProgress((int)(100*i+graphFrame.fieldName.length/totalFields));
//				if (payloadType == FoxFramePart.TYPE_REAL_TIME)
//					graphData2[i] = Config.payloadStore.getRtGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
//				else if (payloadType == FoxFramePart.TYPE_MAX_VALUES)
//					graphData2[i] = Config.payloadStore.getMaxGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
//				else if (payloadType == FoxFramePart.TYPE_MIN_VALUES)
//					graphData2[i] = Config.payloadStore.getMinGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
//				else if (payloadType == FoxFramePart.TYPE_RAD_TELEM_DATA)
//					graphData2[i] = Config.payloadStore.getRadTelemGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
//				else if (payloadType == FoxFramePart.TYPE_HERCI_SCIENCE_HEADER)
//					graphData2[i] = Config.payloadStore.getHerciScienceHeaderGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
//				else if  (payloadType == SatMeasurementStore.RT_MEASUREMENT_TYPE) 
//					graphData2[i] = Config.payloadStore.getMeasurementGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, reverse);
//				else if  (payloadType == SatMeasurementStore.PASS_MEASUREMENT_TYPE) 
//					graphData2[i] = Config.payloadStore.getPassMeasurementGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, reverse);
//				else if (payloadType == FoxFramePart.TYPE_WOD)
//					graphData2[i] = Config.payloadStore.getGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, Spacecraft.WOD_LAYOUT, true, reverse);
//				else if (payloadType == FoxFramePart.TYPE_WOD_RAD_TELEM_DATA)
//					graphData2[i] = Config.payloadStore.getGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, Spacecraft.WOD_RAD2_LAYOUT, false, reverse);		
//				else if (payloadType == FoxFramePart.TYPE_UW_EXPERIMENT)
//					graphData2[i] = Config.payloadStore.getGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, graphFrame.layout.name, false, reverse);		
//				else if (payloadType == FoxFramePart.TYPE_UW_WOD_EXPERIMENT)
//					graphData2[i] = Config.payloadStore.getGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, graphFrame.layout.name, false, reverse);		
//			}
//		}
//		
//		
//		if (graphFrame.SAMPLES > showDialogThreshold)
//			fileProgress.updateProgress(100);
//		
//		//System.err.println("-repaint by: " + by);
//		if (graphData != null && graphData[0] != null)
//			this.repaint();
//	
//	}
	
	/**
	 * Add Lat Lon from stored data or by calculation if needed
	 * Ask the user if its a lot of data
	 */
	private double[][] addPositionData(double[][] coreGraphData) {
		//Log.println("ADDING position data to graph");
		int showDialogThreshold = 9999;
		ProgressPanel fileProgress = null;
		if (graphFrame.SAMPLES > showDialogThreshold) {
			fileProgress = new ProgressPanel(graphFrame, "Calculating Spacecraft positions, please wait ...", false);
			fileProgress.setVisible(true);
		}

		double[][] newGraphData = new double[PayloadStore.LON_COL+1][]; // make room for the lat/lon	
		newGraphData[PayloadStore.RESETS_COL] = coreGraphData[PayloadStore.RESETS_COL];
		newGraphData[PayloadStore.UPTIME_COL] = coreGraphData[PayloadStore.UPTIME_COL];
		newGraphData[PayloadStore.DATA_COL] = coreGraphData[PayloadStore.DATA_COL];
		newGraphData[PayloadStore.LAT_COL] = new double[coreGraphData[PayloadStore.RESETS_COL].length];
		newGraphData[PayloadStore.LON_COL] = new double[coreGraphData[PayloadStore.RESETS_COL].length];
		for (int i=0; i< coreGraphData[PayloadStore.RESETS_COL].length; i++) {
			// Calculate the position
			if (graphFrame.SAMPLES > showDialogThreshold)
				fileProgress.updateProgress((int)(100*i/coreGraphData[PayloadStore.RESETS_COL].length));
			SatPos pos = null;
			double satLatitude = FramePart.NO_TLE;
			double satLongitude = FramePart.NO_TLE;
			try {
				pos = fox.getSatellitePosition((int)newGraphData[PayloadStore.RESETS_COL][i], (long)newGraphData[PayloadStore.UPTIME_COL][i]);
				if (pos != null) {
					satLatitude = FramePart.latRadToDeg (pos.getLatitude());
					satLongitude = FramePart.lonRadToDeg(pos.getLongitude());
				}
				//Log.println("POS: " + (int)newGraphData[PayloadStore.RESETS_COL][i] + "," + (long)newGraphData[PayloadStore.UPTIME_COL][i] + " at "
				//						+ satLatitude + ", " + satLongitude) ;
			} catch (PositionCalcException e) {
				if (e.errorCode == FramePart.NO_TLE) {
					// we just store the default values for NO_TLE
				}
			}	
			newGraphData[PayloadStore.LAT_COL][i] = satLatitude;
			newGraphData[PayloadStore.LON_COL][i] = satLongitude;

		}
		if (graphFrame.SAMPLES > showDialogThreshold)
			fileProgress.updateProgress(100);

		return newGraphData;
	}

	public boolean checkDataExists() {
		if (graphData == null) return false;
		if (graphData[0] == null) return false;
		if (graphData[0][0] == null) return false;
		if (graphData[0][0].length == 0) return false;
		if (graphData[0][PayloadStore.RESETS_COL] == null) return false;
		if (graphData[0][PayloadStore.UPTIME_COL] == null) return false;

		drawGraph2 = true;
		if (graphData2 == null) drawGraph2 = false;
		else if (graphData2[0] == null) drawGraph2 = false;
		else if (graphData2[0][0] == null) drawGraph2 = false;
		else if (graphData2[0][0].length == 0) drawGraph2 = false;
		return true;
	}
	
	public void paintComponent(Graphics gr) {
		super.paintComponent( gr ); // call superclass's paintComponent  
		
		if (graphFrame.showUTCtime && !graphFrame.hideUptime) {
			bottomBorder = (int)(Config.graphAxisFontSize*3.5);
		} else {
			bottomBorder = (int)(Config.graphAxisFontSize*2.5);
		}
			
		g2 = ( Graphics2D ) gr; // cast g to Graphics2D  
		g = gr;
		
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);	
	}
	
	protected double[] plotVerticalAxis(int axisPosition, int graphHeight, int graphWidth, double[][][] graphData, boolean showHorizontalLines, String units, int conversionType, Conversion conversion) {
		int graphType = conversionType;
		String lastConv = "";
		if (conversion != null)
			lastConv = Conversion.getLastConversionInPipeline(conversion.getName());
		
		// Draw vertical axis - always in the same place
		g2.drawLine(sideBorder + axisPosition, getHeight()-bottomBorder, sideBorder+axisPosition, topBorder);
		
		//Analyze the vertical data first because it also determines where we draw the baseline		 
				double maxValue = 0;
				double minValue = 99E99;
			//	double maxValueAxisTwo = 0;
			//	double minValueAxisTwo = 99E99;
				boolean intStep = false;
				
				for (int j=0; j < graphData.length; j++)
					for (int i=0; i < graphData[j][0].length; i++) {
						if (conversionType == BitArrayLayout.CONVERT_MPPT_SOLAR_PANEL_TEMP && graphData[j][PayloadStore.DATA_COL][i] == BitArray.ERROR_VALUE) {
							// do not treat as the maximum.  Set to a default value
							graphData[j][PayloadStore.DATA_COL][i] = FramePart.MPPT_DEFAULT_TEMP;
						}
						if (graphData[j][PayloadStore.DATA_COL][i] >= maxValue) maxValue = graphData[j][PayloadStore.DATA_COL][i];
						if (graphData[j][PayloadStore.DATA_COL][i] <= minValue) minValue = graphData[j][PayloadStore.DATA_COL][i];
					}

				if (maxValue == minValue) {
					if (graphType == BitArrayLayout.CONVERT_INTEGER || lastConv.equalsIgnoreCase(Conversion.FMT_INT)) 
						maxValue = maxValue + 10;
					else
						maxValue = maxValue + 1;
				}
				
				if (conversion != null && conversion instanceof ConversionStringLookUpTable) {
					intStep = true;
					ConversionStringLookUpTable cslt = (ConversionStringLookUpTable)conversion;
					maxValue = cslt.getMaxKey()+1;
					minValue = cslt.getMinKey();
				}
				
				if (graphType == BitArrayLayout.CONVERT_SPIN) {
					maxValue = 8;
					minValue = -8;
							
				}

				if (graphType == BitArrayLayout.CONVERT_VULCAN_STATUS) {
					maxValue = 5;
					minValue = 0;
							
				}

				if (graphType == BitArrayLayout.CONVERT_ANTENNA || graphType == BitArrayLayout.CONVERT_STATUS_ENABLED 
						|| graphType == BitArrayLayout.CONVERT_STATUS_BIT || graphType == BitArrayLayout.CONVERT_BOOLEAN) {
					maxValue = 2;
					minValue = 0;		
				}
			
				if (graphType == BitArrayLayout.CONVERT_FREQ) {
					maxValue = maxValue - freqOffset;
					minValue = minValue - freqOffset;
				}
				
				if (graphFrame.plotDerivative) {
					// We want to include zero in the graph
					if (minValue > 0) minValue = maxValue * -0.1;
					if (maxValue < 0) maxValue = minValue * -0.1;
				}
				// Scale the max and min values so we do not crash into the top and bottom of the window
			//	if (graphType == BitArrayLayout.CONVERT_FREQ) {

//				} else {
				
				// FIXME - SCALE - Should be based on a percentage of the range
				
				if (maxValue < 0)
					maxValue = maxValue - maxValue * 0.20;
				else
					maxValue = maxValue + maxValue * 0.20;
				if (minValue < 0)
					minValue = minValue + minValue * 0.20;
				else
					minValue = minValue - minValue * 0.20;
			//	}
				// calculate number of labels we need on vertical axis
				int numberOfLabels = (graphHeight)/labelHeight;
				
				if (graphType == BitArrayLayout.CONVERT_INTEGER || graphType == BitArrayLayout.CONVERT_VULCAN_STATUS 
						|| graphType == BitArrayLayout.CONVERT_ANTENNA || graphType == BitArrayLayout.CONVERT_STATUS_ENABLED
						|| graphType == BitArrayLayout.CONVERT_BOOLEAN
						|| graphType == BitArrayLayout.CONVERT_STATUS_BIT)
					intStep = true;
				
				if (Config.displayRawValues)
					intStep = true;
				// calculate the label step size
				double[] labels = calcAxisInterval(minValue, maxValue, numberOfLabels, intStep);
				// check the actual number
				numberOfLabels = labels.length;
				
				boolean foundZeroPoint = false;
				int zeroPoint = graphHeight + topBorder; // 10 is the default font size

				DecimalFormat f1 = new DecimalFormat("0.0");
				DecimalFormat f3 = new DecimalFormat("0.00");
				DecimalFormat f2 = new DecimalFormat("0");
				
				int fudge = 0; // fudge factor so that labels on the right axis are offset by the side border, otherwise they would be drawn to left of axis
				if (axisPosition > 0) fudge = sideBorder + (int)(Config.graphAxisFontSize);
				
				g2.setColor(graphTextColor);
				if (Config.displayRawValues) {
					g2.drawString("(RAW)", fudge+axisPosition+sideLabelOffset, topBorder -(int)(Config.graphAxisFontSize/2)); 
				} else
					g2.drawString("("+units+")", fudge+axisPosition+sideLabelOffset, topBorder -(int)(Config.graphAxisFontSize/2)); 
			
				for (int v=0; v < numberOfLabels; v++) {
					
					int pos = getRatioPosition(minValue, maxValue, labels[v], graphHeight);
					pos = graphHeight-pos;
					String s = null;
					if (labels[v] == Math.round(labels[v]))
						s = f2.format(labels[v]);
					else if (f1.format(labels[v]).equalsIgnoreCase(""+labels[v]))
						s = f1.format(labels[v]);
					else
						s = f3.format(labels[v]);

					boolean drawLabel = true;
					// dont draw a label at the zero point or just below it because we have axis labels there, unless
					// this is the second axis
					if ( v < numberOfLabels
							&& !((axisPosition == 0) && (labels[v] == 0.0 || ( v < numberOfLabels-1 && labels[v+1] == 0.0)))
							&& !(v == 0 && pos > graphHeight)
							) {

						if (!Config.displayRawValues) {
							if (conversion != null && conversion instanceof ConversionStringLookUpTable) {
								ConversionStringLookUpTable cslt = (ConversionStringLookUpTable)conversion;
								s = cslt.calculateString(labels[v]-1); // we offset by 1 so the last label is not on the axis
								if (s.equalsIgnoreCase(ConversionStringLookUpTable.ERROR))
									drawLabel = false;
								else
									drawLabel = true;
							}

							if (graphType == BitArrayLayout.CONVERT_ANTENNA) {
								drawLabel = false;
								if (labels[v] == 1) {
									s = "STWD";
									drawLabel = true;
								}
								if (labels[v] == 2) {
									s = "DEP";
									drawLabel = true;
								}

							} 
							if (graphType == BitArrayLayout.CONVERT_STATUS_BIT) {
								drawLabel = false;
								if (labels[v] == 1) {
									s = "OK";
									drawLabel = true;
								}
								if (labels[v] == 2) {
									s = "FAIL";
									drawLabel = true;
								}

							} 

							if (graphType == BitArrayLayout.CONVERT_VULCAN_STATUS) {
								drawLabel = false;
								if (labels[v] == 1) {
									s = RadiationPacket.radPacketStateShort[0];
									drawLabel = true;
								}
								if (labels[v] == 2) {
									s = RadiationPacket.radPacketStateShort[1];
									drawLabel = true;
								}
								if (labels[v] == 3) {
									s = RadiationPacket.radPacketStateShort[2];
									drawLabel = true;
								}
								if (labels[v] == 4) {
									s = RadiationPacket.radPacketStateShort[3];
									drawLabel = true;
								}
								if (labels[v] == 5) {
									s = RadiationPacket.radPacketStateShort[4];
									drawLabel = true;
								}

							} 

							if (graphType == BitArrayLayout.CONVERT_STATUS_ENABLED) {
								drawLabel = false;
								if (labels[v] == 2) {
									s = "Enabled";
									drawLabel = true;
								}
								if (labels[v] == 1) {
									s = "Disabled";
									drawLabel = true;
								}	
							} 

							if (graphType == BitArrayLayout.CONVERT_BOOLEAN) {
								drawLabel = false;
								if (labels[v] == 2) {
									s = "TRUE";
									drawLabel = true;
								}
								if (labels[v] == 1) {
									s = "FALSE";
									drawLabel = true;
								}

							} 
						}
						
						if (drawLabel && pos > 0 && (pos < graphHeight-Config.graphAxisFontSize/2))	{ 
							g2.setColor(graphTextColor);
							
							g2.drawString(s, fudge+axisPosition+sideLabelOffset, pos+topBorder+(int)(Config.graphAxisFontSize/2)); // add 4 to line up with tick line
							g2.setColor(graphAxisColor);
							if (showHorizontalLines) {
								g2.setColor(Color.GRAY);
								g2.drawLine(sideBorder-5, pos+topBorder, graphWidth+sideBorder, pos+topBorder);
								g2.setColor(graphTextColor);
							} else
								g.drawLine(sideBorder+axisPosition-5, pos+topBorder, sideBorder+axisPosition+5, pos+topBorder);
						}
					}
					
					if (!foundZeroPoint) {
						if (labels[v] >= 0) {
							foundZeroPoint = true;
							if ( v ==0 )
								zeroPoint = graphHeight+topBorder; // 10 is the default font size;
							else
								zeroPoint = pos+topBorder;
						}
					}
				}
				double[] axisPoint = new double[3];
				axisPoint[0] = zeroPoint;
				axisPoint[1] = minValue;
				axisPoint[2] = maxValue;
				return axisPoint;
	}
	
	/**
	 * Given a number of ticks across a window and the range, calculate the tick size
	 * and return an array of tick values to use on an axis.  It will have one of the step sizes
	 * calculated by the stepFunction
	 * 
	 * @param range
	 * @param ticks
	 * @return
	 */
	public static double[] calcAxisInterval(double min, double max, int ticks, boolean intStep) {
		double range = max - min;
		if (ticks ==0) ticks = 1;
		double step = 0.0;

		// From the range and the number of ticks, work out a suitable tick size
		step = getStep(range, ticks, intStep);
		
		// We don't want labels that plot off the end of the graph, so reduce the ticks if needed
		ticks = (int) Math.ceil(range / step);
		// Now find the first value before the minimum.
		double startValue = roundToSignificantFigures(Math.round(min/step) * step, 6);

		// We want ticks that go all the way to the end, so resize the tick list if needed

//		int newticks = (int)((max - startValue) / step + 1);
//		if (newticks < ticks * 3 && newticks > ticks) {
//			ticks = newticks;
//			step = getStep(range, ticks);
//		}
		if (ticks > MAX_TICKS) {
			ticks = MAX_TICKS;  // safety check
			step = getStep(range, ticks, intStep);
		}
		if (ticks < 0) {
			ticks = 1;
			step = getStep(range, ticks, intStep);
		}
		
		double[] tickList = new double[ticks];

		if (ticks == 1) { // special case where we do not have room for a label so only some labels are plotted
			//double midValue = roundToSignificantFigures(Math.round(range/2/step) * step, 6);
			tickList[0] = startValue;
		} else 	if (ticks == 2) {
			double midValue = roundToSignificantFigures(startValue + step, 6);
			tickList[0] = startValue;
			tickList[1] = midValue;
		} else
		if (ticks > 0)
			tickList[0] = startValue;
		for (int i=1; i< ticks; i++) {
			startValue = roundToSignificantFigures(startValue + step, 6);
			//val = Math.round(val/step) * step;
			tickList[i] = startValue;
		}
		
		return tickList;
	}

	private static double getStep(double range, int ticks, boolean intStep) {
		double step = 0;
		
		if (!intStep && range/ticks <= 0.01) step = 0.01d;
		else if (!intStep && range/ticks <= 0.1) step = 0.10d;
		else if (!intStep && range/ticks <= 0.2) step = 0.20d;
		else if (!intStep && range/ticks <= 0.25) step = 0.25d;
		else if (!intStep && range/ticks <= 0.33) step = 0.33d;
		else if (!intStep && range/ticks <= 0.5) step = 0.50d;
		else if (range/ticks <= 1) step = 1.00d;
		else if (range/ticks <= 2) step = 2.00d;
		else if (!intStep && range/ticks <= 2.5) step = 2.50d;
		else if (!intStep && range/ticks <= 3.3) step = 3.33d;
		else if (range/ticks <= 5) step = 5.00d;
		else if (range/ticks <= 10) step = 10.00d;
		else if (range/ticks <= 25) step = 25.00d;
		else if (!intStep && range/ticks <= 33) step = 33.33d;
		else if (range/ticks <= 50) step = 50.00d;
		else if (range/ticks <= 100) step = 100.00d;
		else if (range/ticks <= 200) step = 200.00d;
		else if (range/ticks <= 250) step = 250.00d;
		else if (!intStep && range/ticks <= 333) step = 333.33d;
		else if (range/ticks <= 500) step = 500.00d;
		else if (range/ticks <= 1000) step = 1000.00d;
		else if (range/ticks <= 2000) step = 2000.00d;
		else if (range/ticks <= 2500) step = 2500.00d;
		else if (!intStep && range/ticks <= 3333) step = 3333.33d;
		else if (range/ticks <= 5000) step = 5000.00d;
		else if (range/ticks <= 10000) step = 10000.00d;
		else if (range/ticks <= 20000) step = 20000.00d;
		else if (range/ticks <= 25000) step = 25000.00d;
		else if (!intStep && range/ticks <= 33333) step = 33333.33d;
		else if (range/ticks <= 50000) step = 50000.00d;
		else if (range/ticks <= 100000) step = 100000.00d;
		else if (range/ticks <= 250000) step = 250000.00d;
		else if (!intStep && range/ticks <= 333333) step = 333333.33d;
		else if (range/ticks <= 500000) step = 500000.00d;
		else if (range/ticks <= 1000000) step = 1000000.00d;
		else if (range/ticks <= 2000000) step = 2000000.00d;
		else if (range/ticks <= 2500000) step = 2500000.00d;
		else if (!intStep && range/ticks <= 3333333) step = 3333333.33d;
		else if (range/ticks <= 5000000) step = 5000000.00d;
		else if (range/ticks <= 10000000) step = 10000000.00d;
		return step;
	}

	
	
	public static double roundToSignificantFigures(double num, int n) {
	    if(num == 0) {
	        return 0;
	    }

	    final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
	    final int power = n - (int) d;

	    final double magnitude = Math.pow(10, power);
	    final long shifted = Math.round(num*magnitude);
	    return shifted/magnitude;
	}
	
	public static int getRatioPosition(double min, double max, double value, int dimension) {
		if (max == min) return 0;
		double ratio = (max - value) / (max - min);
		int position = (int)Math.round(dimension * ratio);
		return dimension-position;
	}

}
