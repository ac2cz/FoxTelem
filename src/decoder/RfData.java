package decoder;

import common.Log;

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
 * Measurements made against the RF spectrum
 * 
 *
 */
public class RfData extends DataMeasure {

    public static final int PEAK_SIGNAL_IN_FILTER_WIDTH = 0; // The peak signal within the filter width
    public static final int BIN_OF_PEAK_SIGNAL_IN_FILTER_WIDTH = 1; // The bin that the peak signal is in within the filter width
    public static final int NOISE_OUTSIDE_FILTER_WIDTH = 2; // The average level of the noise by sampling half the filter width either side of the filter
    public static final int AVGSIG_IN_FILTER_WIDTH = 3; // The average signal in the filter width.
    public static final int STRONGEST_SIGNAL_IN_SAT_BAND = 4;
    public static final int BIN_OF_STRONGEST_SIGNAL_IN_SAT_BAND = 5;
    public double rfSNRInFilterWidth; // Average SNR in the filter band of the receiver
    public double rfStrongestSigSNRInSatBand; // A quick and dirty estimate of the SNR of a strong signal outside the filter band
        
    double binBandwidth;
    SourceIQ iqSource;
    
    public RfData(SourceIQ iq) {
    	AVERAGE_PERIOD = 100; //1000 = 1 sec average time.  Each FFT window is processed in 2ms
    	MEASURES = 6;
    	iqSource = iq;
    	init();
    }
    
	public int getBinOfPeakSignalInFilterWidth() {
		return (int)getAvg(BIN_OF_PEAK_SIGNAL_IN_FILTER_WIDTH);
	}

	public int getBinOfStrongestSignalInSatBand() {
		return (int)getAvg(BIN_OF_STRONGEST_SIGNAL_IN_SAT_BAND);
	}

	
	public long getFrequencyOfPeakSignalInFilterWidth() {
		return iqSource.getFrequencyFromBin(getBinOfPeakSignalInFilterWidth());
	}

	public long getFrequencyOfStrongestSignalInSatBand() {
		return iqSource.getFrequencyFromBin(getBinOfStrongestSignalInSatBand());
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("RfData");
		while(running) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				Log.println(e.getMessage());
			}
			if (readyToAverage()) {
				runAverage();
				calcAverages();
			}
		}
		
	}
	
	public void stopProcessing() {
		running = false;
		Log.println("RfData STOPPING");
	}

	private void calcAverages() {    		
			if (getAvg(AVGSIG_IN_FILTER_WIDTH) != 0 && getAvg(NOISE_OUTSIDE_FILTER_WIDTH) != 0) {
				double p = getAvg(AVGSIG_IN_FILTER_WIDTH);
				double n = getAvg(NOISE_OUTSIDE_FILTER_WIDTH);
				if (p < 10 && p > -150)
					if (n < 10 && n > -150)
						if (p > n) // we don't store negative values as the signal we are after has to be above the noise
							rfSNRInFilterWidth = (p - n);  // these are in dB so subtract rather than divide
			}
			if (getAvg(STRONGEST_SIGNAL_IN_SAT_BAND) != 0 && getAvg(NOISE_OUTSIDE_FILTER_WIDTH) != 0) {
				double p = getAvg(STRONGEST_SIGNAL_IN_SAT_BAND); 
				double n = getAvg(NOISE_OUTSIDE_FILTER_WIDTH);
				
				if (p < 10 && p > -150)
					if (n < 10 && n > -150)
						if (p > n)
							rfStrongestSigSNRInSatBand = (p - n);  // these are in dB so subtract rather than divide
			}
    }

    /**
     * Store paramaters about the peak signal in the filter passband
     * @param p
     * @param b
     * @param sig
     * @param n
     */
	public void setPeakSignalInFilterWidth(double p, int b, double sig, double n) {
		setValue(PEAK_SIGNAL_IN_FILTER_WIDTH, p);
		setValue(BIN_OF_PEAK_SIGNAL_IN_FILTER_WIDTH, b);
		setValue(AVGSIG_IN_FILTER_WIDTH, sig);
		setValue(NOISE_OUTSIDE_FILTER_WIDTH, n);
	}
	
	public void setStrongestSignal(double sig, int b) {
		setValue(STRONGEST_SIGNAL_IN_SAT_BAND, sig);
		setValue(BIN_OF_STRONGEST_SIGNAL_IN_SAT_BAND, b);
		
	}



	
}
