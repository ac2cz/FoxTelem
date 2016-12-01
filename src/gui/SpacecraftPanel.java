package gui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import common.Spacecraft;

/**
* 
* FOX 1 Telemetry Decoder
* @author chris.e.thompson g0kla/ac2cz
*
* Copyright (C) 2016 amsat.org
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
* The SpacecraftPanel is a JPanel on the SourceTab (input tab).  It could also be displayed on the
* SpacecraftTab.  This contains the paramaters that can be changed on the decoder for a specific spacecraft
* Ideally it includes all of the following: 
*   Track - visible.  STARTS the decoder, as apposed to the Source
*   Lower Freq Bound, Upper Freq Bound  - THIS CAN START IN THE SPACECRAFT FRAME
*   Track Doppler
*   Find Signal - CAN BE UNIVERSAL INITIALLY, BUT SHOULD BE HERE
*   - Peak Threshold
*   - AvgPwr Threshold
*   - BitSNR Threshold
* 
* Note that this does NOT include the static data for the satellite, which is changed in the SpacecraftFrame window:
*   Frequency
*   MODE - which sets the demodulator (FM, PSK)
*   RATE - 200bps, 1200bps
*   DECODER - FOX-1 DUV, FOX-1 HS - once all the paramaters are set, this becomes the name only
*   Bandwidth 
*   Filter Type, Freq and Length
*     
* These are the paramaters that are specific to a given 
* channel where we are trying to decode a spacecraft.
* 
* Several classes will read the paramaters, so they will be saved in Config, with the spacecraft as part of the
* config.
* 
*/
public class SpacecraftPanel extends JPanel {
	Spacecraft fox;
	
	public SpacecraftPanel(Spacecraft s) {
		fox = s;
		JLabel title = new JLabel(fox.name);
		add(title);
	}
}
