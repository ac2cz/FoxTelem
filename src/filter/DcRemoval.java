package filter;

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
 * This is an IIR single-pole DC removal filter, as described by J M de Freitas in
 * 29Jan2007 paper at: 
 * 
 * http://www.mathworks.com/matlabcentral/fileexchange/downloads/72134/download
 * 
 * @param alpha 0.0 - 1.0 float - the closer alpha is to unity, the closer
 * the cutoff frequency is to DC.
 */
public class DcRemoval {

	private float mAlpha;
	private float mPreviousInput = 0.0f;
	private float mPreviousOutput = 0.0f;
	
	public DcRemoval( float a ) {
		mAlpha = a;
	}
    
	public float filter( float currentInput ) {
		float currentOutput = ( currentInput - mPreviousInput ) + 
							  ( mAlpha * mPreviousOutput );
		
		
		mPreviousInput = currentInput;
		mPreviousOutput = currentOutput;
		//if (Config.eliminateDC)
		return currentOutput;
		//else return currentInput;
    }
}
