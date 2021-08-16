package common;

import decoder.Decoder;
import decoder.EyeData;
import decoder.RfData;
import decoder.SourceIQ;
import telemetry.frames.Frame;

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
 * A class that holds the details for a pass and stored in the pass manager.  We package these paramaters together
 * because we need one set for each decoder that is running.
 *
 */
public class PassParams {
	public SourceIQ iqSource;
	public Decoder foxDecoder;
	public RfData rfData;
	public EyeData eyeData;
	public Frame lastFrame;
	
	public void resetEyeData() {
		eyeData = foxDecoder.eyeData;
		if (eyeData != null)
			eyeData.reset();
//	
	}

}
