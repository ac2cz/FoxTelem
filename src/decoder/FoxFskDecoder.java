package decoder;

import common.Log;
import decoder.FoxBPSK.FormatBitStream;
import filter.RaisedCosineFilter;
import telemetry.Format.TelemFormat;

public class FoxFskDecoder extends FoxDecoder {
	public static final String HIGHSPEED_FSK = "FSK 9600bps (Fox)";
	public static final String DUV_FSK = "FSK 200bps (DUV)";
	
	public FoxFskDecoder(SourceAudio as, int chan, TelemFormat telemFormat) {
		super("DUV", as, chan, telemFormat);
		this.telemFormat = telemFormat;
	}
	
	public void init() {
		Log.println("Initializing Decoder with Format: " + telemFormat.name);
		setParameters();
		super.init();
		updateFilter();
	}
	
	/**
	 * Called if any of the filter params have changed
	 */
	private void updateFilter() {
		filter = new RaisedCosineFilter(audioSource.audioFormat, BUFFER_SIZE);
		filter.init(currentSampleRate, telemFormat.getInt(TelemFormat.BPS), bucketSize*2);
	}
	
	private void setParameters() {
		//decodedFrame = new SlowSpeedFrame();
		bitStream = new FormatBitStream(this, telemFormat, false);
		BITS_PER_SECOND = telemFormat.getInt(TelemFormat.BPS);
		//SAMPLE_WINDOW_LENGTH = 70; 
		bucketSize = currentSampleRate / BITS_PER_SECOND;
		//SAMPLE_WIDTH = bucketSize*SAMPLE_WIDTH_PERCENT/100;
		//if (SAMPLE_WIDTH < 1) SAMPLE_WIDTH = 1;
		//CLOCK_TOLERANCE = 10;
		//CLOCK_REOVERY_ZERO_THRESHOLD = 20;
		
		// HIGH SPEED TEST Gives the below as a good compromise between DUV and 9600
		SAMPLE_WIDTH = 1;
		SAMPLE_WINDOW_LENGTH = 90; 
		CLOCK_TOLERANCE = 10;
		CLOCK_REOVERY_ZERO_THRESHOLD = 20;
	}
}
