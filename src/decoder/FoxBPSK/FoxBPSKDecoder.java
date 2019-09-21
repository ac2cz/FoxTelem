package decoder.FoxBPSK;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Performance;
import common.Spacecraft;
import decoder.CodePRN;
import decoder.Decoder;
import decoder.SourceAudio;
import filter.AGCFilter;
import filter.DcRemoval;
import gui.MainWindow;
import telemetry.Frame;
import telemetry.FoxBPSK.FoxBPSKFrame;
import telemetry.FoxBPSK.FoxBPSKHeader;

public class FoxBPSKDecoder extends Decoder {
	public static final int BITS_PER_SECOND_1200 = 1200;
	public static final int WORD_LENGTH = 10;
//	public static final int SYNC_WORD_LENGTH = 15;
//	public static final int SYNC_WORD_LENGTH = 31;
	private int clockOffset = 0;
	private double[] cosTab;
	private double[] sinTab;
	DcRemoval audioDcFilter;

	double[] pskAudioData;

	/**
	 * This holds the stream of bits that we have not decoded. Once we have several
	 * SYNC words, this is flushed of processed bits.
	 */
	protected FoxBPSKBitStream bitStream = null;  // Hold bits until we turn them into decoded frames

	public FoxBPSKDecoder(SourceAudio as, int chan) {
		super("1200bps BPSK", as, chan);
		init();
	}

	@Override
	protected void init() {
		Log.println("Initializing 1200bps BPSK decoder: ");
		bitStream = new FoxBPSKBitStream(this, WORD_LENGTH, CodePRN.getSyncWordLength());
		BITS_PER_SECOND = BITS_PER_SECOND_1200;
		SAMPLE_WINDOW_LENGTH = 40; //40;  
		bucketSize = currentSampleRate / BITS_PER_SECOND; // Number of samples that makes up one bit

		BUFFER_SIZE =  SAMPLE_WINDOW_LENGTH * bucketSize;
		SAMPLE_WIDTH = 4;
		if (SAMPLE_WIDTH < 1) SAMPLE_WIDTH = 1;
		CLOCK_TOLERANCE = bucketSize/2;
		CLOCK_REOVERY_ZERO_THRESHOLD = 20;
		initWindowData();

		filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE));
		audioDcFilter = new DcRemoval(0.9999d);
		filter.init(currentSampleRate, 0, 0);
		//filter = new RaisedCosineFilter(audioSource.audioFormat, BUFFER_SIZE);
		//filter.init(currentSampleRate, 1200, 65);

		cosTab = new double[SINCOS_SIZE];
		sinTab = new double[SINCOS_SIZE];

		for (int n=0; n<SINCOS_SIZE; n++) {
			cosTab[n] = Math.cos(n*2.0*Math.PI/SINCOS_SIZE);
			sinTab[n] = Math.sin(n*2.0*Math.PI/SINCOS_SIZE);
		}
		pskAudioData = new double[BUFFER_SIZE];
	}

	protected void resetWindowData() {
		super.resetWindowData();

	}

	public double[] getBasebandData() {
		return pskAudioData;
	}

	/**
	 * Sample the buckets (one bucket per bit) to determine the change in phase and hence
	 * the bit that each bucket contains.  We use the following approach:
	 * Each sample in each bit is multiplied by the corresponding sample in the previous bit.  These multiplications
	 * These multiplications are Integrated (summed) over the bit
	 * If the total sum is positive then the phase did not change and we have a 1
	 * If the total sum is negative then the phase did change and we have a 0
	 * We store the data for the last bit so that we can use it as the "previous bit" for the first calculation next time
	 * 
	 * While we are sampling, we keep track of the clock offset in case we need to adjust it.
	 * 
	 */
	private double vcoPhase = 0.0;
	private static final double RX_CARRIER_FREQ = 1200.0;
	//private static final double VCO_PHASE_INC = 2.0*Math.PI*RX_CARRIER_FREQ/(double)48000;
	private static final int SINCOS_SIZE = 256;
	double gain = 1.0;
	
	protected void sampleBuckets() {
		int maxValue = 0;
		int minValue = 0;
		int DESIRED_RANGE =60000;
		
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			for (int s=0; s < bucketSize; s++) {
				//sampleWithVCO(dataValues[i][s], i, s);
				double value = dataValues[i][s]/ 32768.0;
				//////					value = audioDcFilter.filter(value);		
				RxDownSample(value, value, i);
				int eyeValue = (int) (Math.sqrt(energy1)-32768.0/2);
				if (eyeValue > maxValue) maxValue = eyeValue;
				if (eyeValue < minValue) minValue = eyeValue;
				eyeValue = (int) (eyeValue * gain); // gain from the last SAMPLE_WINDOW
				pskAudioData[i*bucketSize+s] = eyeValue/32768.0;	
				
///				pskAudioData[i*bucketSize+s] = energy1/dmEnergy[dmPeakPos]-1;
///				int eyeValue = (int)(32768*(energy1/dmEnergy[dmPeakPos]-1));
				eyeData.setData(i,s,eyeValue);
			}
			int offset = recoverClockOffset();
			if (middleSample[i] == false)
				eyeData.setOffsetLow(i, SAMPLE_WIDTH, offset );
			else
				eyeData.setOffsetHigh(i, SAMPLE_WIDTH, offset);
		}
		
		gain = DESIRED_RANGE / (1.0f * (maxValue-minValue));
		
		int offset = recoverClockOffset();
		eyeData.offsetEyeData(offset); // rotate the data so that it matches the clock offset

		//	Scanner scanner = new Scanner(System.in);
		//		System.out.println("Press enter");
		//	String username = scanner.next();
		
	}




	/**
	 * Determine if the bit sampling buckets are aligned with the data. This is calculated when the
	 * buckets are sampled
	 * 
	 */
	@Override
	public int recoverClockOffset() {

		return clockOffset;
	}

	protected double[] recoverClock(int factor) {

		return null;
	}

	@Override
	protected void processBitsWindow() {
		Performance.startTimer("findSync");
		ArrayList<Frame> frames = bitStream.findFrames(SAMPLE_WINDOW_LENGTH);
		Performance.endTimer("findSync");
		if (frames != null) {
			processPossibleFrame(frames);
		}
	}

	private Frame decodedFrame = null;
	protected void processPossibleFrame(ArrayList<Frame> frames) {

		FoxSpacecraft sat = null;
		for (Frame decodedFrame : frames)
			if (decodedFrame != null && !decodedFrame.corrupt) {
				Performance.startTimer("Store");
				// Successful frame
				eyeData.lastErasureCount = decodedFrame.rsErasures;
				eyeData.lastErrorsCount = decodedFrame.rsErrors;
				//eyeData.setBER(((bitStream.lastErrorsNumber + bitStream.lastErasureNumber) * 10.0d) / (double)bitStream.SYNC_WORD_DISTANCE);
				if (Config.storePayloads) {

					FoxBPSKFrame hsf = (FoxBPSKFrame)decodedFrame;
					FoxBPSKHeader header = hsf.getHeader();
					sat = (FoxSpacecraft) Config.satManager.getSpacecraft(header.id);
					hsf.savePayloads(Config.payloadStore, sat.hasModeInHeader);

					// Capture measurements once per payload or every 5 seconds ish
					addMeasurements(header, decodedFrame, decodedFrame.rsErrors, decodedFrame.rsErasures);
				}
				Config.totalFrames++;
				if (Config.uploadToServer)
					try {
						Config.rawFrameQueue.add(decodedFrame);
					} catch (IOException e) {
						// Don't pop up a dialog here or the user will get one for every frame decoded.
						// Write to the log only
						e.printStackTrace(Log.getWriter());
					}
				if (sat != null && sat.sendToLocalServer())
					try {
						Config.rawPayloadQueue.add(decodedFrame);
					} catch (IOException e) {
						// Don't pop up a dialog here or the user will get one for every frame decoded.
						// Write to the log only
						e.printStackTrace(Log.getWriter());
					}
				framesDecoded++;
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() { MainWindow.setTotalDecodes();}
					});
				} catch (InvocationTargetException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				Performance.endTimer("Store");
			} else {
				if (Config.debugBits) Log.println("SYNC marker found but frame not decoded\n");
				//clockLocked = false;
			}
	}

	/**
	 * The PSK demodulation algorithm is based on code by Howard and the Funcube team
	 */
	private static final int DOWN_SAMPLE_FILTER_SIZE = 27;
	private static final double[] dsFilter = {
			-6.103515625000e-004F,  /* filter tap #    0 */
			-1.220703125000e-004F,  /* filter tap #    1 */
			+2.380371093750e-003F,  /* filter tap #    2 */
			+6.164550781250e-003F,  /* filter tap #    3 */
			+7.324218750000e-003F,  /* filter tap #    4 */
			+7.629394531250e-004F,  /* filter tap #    5 */
			-1.464843750000e-002F,  /* filter tap #    6 */
			-3.112792968750e-002F,  /* filter tap #    7 */
			-3.225708007813e-002F,  /* filter tap #    8 */
			-1.617431640625e-003F,  /* filter tap #    9 */
			+6.463623046875e-002F,  /* filter tap #   10 */
			+1.502380371094e-001F,  /* filter tap #   11 */
			+2.231445312500e-001F,  /* filter tap #   12 */
			+2.518310546875e-001F,  /* filter tap #   13 */
			+2.231445312500e-001F,  /* filter tap #   14 */
			+1.502380371094e-001F,  /* filter tap #   15 */
			+6.463623046875e-002F,  /* filter tap #   16 */
			-1.617431640625e-003F,  /* filter tap #   17 */
			-3.225708007813e-002F,  /* filter tap #   18 */
			-3.112792968750e-002F,  /* filter tap #   19 */
			-1.464843750000e-002F,  /* filter tap #   20 */
			+7.629394531250e-004F,  /* filter tap #   21 */
			+7.324218750000e-003F,  /* filter tap #   22 */
			+6.164550781250e-003F,  /* filter tap #   23 */
			+2.380371093750e-003F,  /* filter tap #   24 */
			-1.220703125000e-004F,  /* filter tap #   25 */
			-6.103515625000e-004F   /* filter tap #   26 */
	};
	//		private static final double DOWN_SAMPLE_MULT = 0.9*32767.0;	// XXX: Voodoo from Howard?
	private static final int MATCHED_FILTER_SIZE = 65;
	private static final double[] dmFilter = {
			-0.0101130691F,-0.0086975143F,-0.0038246093F,+0.0033563764F,+0.0107237026F,+0.0157790936F,+0.0164594107F,+0.0119213911F,
			+0.0030315224F,-0.0076488191F,-0.0164594107F,-0.0197184277F,-0.0150109226F,-0.0023082460F,+0.0154712381F,+0.0327423589F,
			+0.0424493086F,+0.0379940454F,+0.0154712381F,-0.0243701991F,-0.0750320094F,-0.1244834076F,-0.1568500423F,-0.1553748911F,
			-0.1061032953F,-0.0015013786F,+0.1568500423F,+0.3572048240F,+0.5786381191F,+0.7940228249F,+0.9744923010F,+1.0945250059F,
			+1.1366117829F,+1.0945250059F,+0.9744923010F,+0.7940228249F,+0.5786381191F,+0.3572048240F,+0.1568500423F,-0.0015013786F,
			-0.1061032953F,-0.1553748911F,-0.1568500423F,-0.1244834076F,-0.0750320094F,-0.0243701991F,+0.0154712381F,+0.0379940454F,
			+0.0424493086F,+0.0327423589F,+0.0154712381F,-0.0023082460F,-0.0150109226F,-0.0197184277F,-0.0164594107F,-0.0076488191F,
			+0.0030315224F,+0.0119213911F,+0.0164594107F,+0.0157790936F,+0.0107237026F,+0.0033563764F,-0.0038246093F,-0.0086975143F,
			-0.0101130691F,
			-0.0101130691F,-0.0086975143F,-0.0038246093F,+0.0033563764F,+0.0107237026F,+0.0157790936F,+0.0164594107F,+0.0119213911F,
			+0.0030315224F,-0.0076488191F,-0.0164594107F,-0.0197184277F,-0.0150109226F,-0.0023082460F,+0.0154712381F,+0.0327423589F,
			+0.0424493086F,+0.0379940454F,+0.0154712381F,-0.0243701991F,-0.0750320094F,-0.1244834076F,-0.1568500423F,-0.1553748911F,
			-0.1061032953F,-0.0015013786F,+0.1568500423F,+0.3572048240F,+0.5786381191F,+0.7940228249F,+0.9744923010F,+1.0945250059F,
			+1.1366117829F,+1.0945250059F,+0.9744923010F,+0.7940228249F,+0.5786381191F,+0.3572048240F,+0.1568500423F,-0.0015013786F,
			-0.1061032953F,-0.1553748911F,-0.1568500423F,-0.1244834076F,-0.0750320094F,-0.0243701991F,+0.0154712381F,+0.0379940454F,
			+0.0424493086F,+0.0327423589F,+0.0154712381F,-0.0023082460F,-0.0150109226F,-0.0197184277F,-0.0164594107F,-0.0076488191F,
			+0.0030315224F,+0.0119213911F,+0.0164594107F,+0.0157790936F,+0.0107237026F,+0.0033563764F,-0.0038246093F,-0.0086975143F,
			-0.0101130691F
	};
	private static final int DOWN_SAMPLE_RATE = 9600;
	private static final int BIT_RATE = 1200;
	private static final int SAMPLES_PER_BIT = DOWN_SAMPLE_RATE/BIT_RATE;
	private static final double VCO_PHASE_INC = 2.0*Math.PI*RX_CARRIER_FREQ/(double)DOWN_SAMPLE_RATE;
	private static final double BIT_SMOOTH1 = 1.0/200.0;
	private static final double BIT_SMOOTH2 = 1.0/800.0;
	private static final double BIT_PHASE_INC = 1.0/(double)DOWN_SAMPLE_RATE;
	private static final double BIT_TIME = 1.0/(double)BIT_RATE;

	private double energy1, energy2;

	private double[][] dsBuf = new double[DOWN_SAMPLE_FILTER_SIZE][2];
	private int dsPos = DOWN_SAMPLE_FILTER_SIZE-1, dsCnt = 0;
	private double HOWARD_FUDGE_FACTOR = 0.9 * 32768.0;

	/**
	 * Down sample from input rate to DOWN_SAMPLE_RATE and low pass filter
	 * @param i
	 * @param q
	 * @param bucketNumber
	 */
	private void RxDownSample(double i, double q, int bucketNumber) {
		dsBuf[dsPos][0]=i;
		dsBuf[dsPos][1]=q;
		if (++dsCnt>=(int)currentSampleRate/DOWN_SAMPLE_RATE) {	// typically 48000/9600
			double fi = 0.0, fq = 0.0;
			// apply low pass FIR
			for (int n=0; n<DOWN_SAMPLE_FILTER_SIZE; n++) {
				int dsi = (n+dsPos)%DOWN_SAMPLE_FILTER_SIZE; 
				fi+=dsBuf[dsi][0]*dsFilter[n];
				fq+=dsBuf[dsi][1]*dsFilter[n];
			}
			dsCnt=0;
			// feed down sampled values to demodulator
			RxDemodulate(fi * HOWARD_FUDGE_FACTOR, fq * HOWARD_FUDGE_FACTOR, bucketNumber);
		}
		dsPos--;
		if (dsPos<0)
			dsPos=DOWN_SAMPLE_FILTER_SIZE-1;
	}

	private double[][] dmBuf = new double[MATCHED_FILTER_SIZE][2];
	private int dmPos = MATCHED_FILTER_SIZE-1;
	private double[] dmEnergy = new double[SAMPLES_PER_BIT+2];
	private int dmBitPos = 0, dmPeakPos = 0, dmNewPeak = 0;
	private double dmEnergyOut = 1.0;
	private int[] dmHalfTable = {4,5,6,7,0,1,2,3};
	private double dmBitPhase = 0.0;
	private double[] dmLastIQ = new double[2];
	
	//private int debugCount = 0;

	/**
	 * Demodulate the DBPSK signal, adjust the clock if needed to stay in sync.  Populate the bit buffer
	 * @param i
	 * @param q
	 * @param bucketNumber
	 */
	private void RxDemodulate(double i, double q, int bucketNumber) {
		vcoPhase += VCO_PHASE_INC;
		if (vcoPhase > 2.0*Math.PI)
			vcoPhase -= 2.0*Math.PI;
		// quadrature demodulate carrier to base band with VCO, store in FIR buffer
		dmBuf[dmPos][0]=i*cosTab[(int)(vcoPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE];
		dmBuf[dmPos][1]=q*sinTab[(int)(vcoPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE];
		// apply FIR (base band smoothing, root raised cosine)
		double fi = 0.0, fq = 0.0;
		for (int n=0; n<MATCHED_FILTER_SIZE; n++) {
			int dmi = (MATCHED_FILTER_SIZE-dmPos+n);
			fi+=dmBuf[n][0]*dmFilter[dmi];
			fq+=dmBuf[n][1]*dmFilter[dmi];
		}
		dmPos--;
		if (dmPos<0)
			dmPos=MATCHED_FILTER_SIZE-1;

		// store smoothed bit energy
		energy1 = fi*fi+fq*fq;
		dmEnergy[dmBitPos] = (dmEnergy[dmBitPos]*(1.0-BIT_SMOOTH1))+(energy1*BIT_SMOOTH1);

		// at peak bit energy? decode 
		if (dmBitPos==dmPeakPos) {
			dmEnergyOut = (dmEnergyOut*(1.0-BIT_SMOOTH2))+(energy1*BIT_SMOOTH2);
			double di = -(dmLastIQ[0]*fi + dmLastIQ[1]*fq);
			double dq = dmLastIQ[0]*fq - dmLastIQ[1]*fi;
			dmLastIQ[0]=fi;
			dmLastIQ[1]=fq;
			energy2 = Math.sqrt(di*di+dq*dq);

			if (energy2>100.0) {	// TODO: work out where these magic numbers come from!
				boolean bit = di<0.0;	// is that a 1 or 0 based on if we changed phase.  If it looks like the last bit, its a 1
				middleSample[bucketNumber] = bit;
				/*System.err.print(bit?1:0);
				if (debugCount++ == 80) {
					System.err.println("");
					debugCount=0;
				} */
				bitStream.addBit(bit);
			}
		}
		// half-way into next bit? reset peak energy point
		if (dmBitPos==dmHalfTable[dmPeakPos]) {
			dmPeakPos = dmNewPeak;
			clockOffset = 4*(dmNewPeak-6); // store the clock offset so we can display the eye diagram "triggered" correctly
			// = 4*(dmBitPos-6); // store the clock offset so we can display the eye diagram "triggered" correctly
		}
		dmBitPos = (dmBitPos+1) % SAMPLES_PER_BIT;
		// advance phase of bit position and get ready for the next bit
		dmBitPhase += BIT_PHASE_INC;
		if (dmBitPhase>=BIT_TIME) {
			dmBitPhase-=BIT_TIME;
			dmBitPos=0;	
			// rolled round another bit, measure new peak energy position
			double eMax = -1.0e10F;
			for (int n=0; n<SAMPLES_PER_BIT; n++) {
				if (dmEnergy[n]>eMax) {
					dmNewPeak=n;
					eMax=dmEnergy[n];
				}
			}
			
		}
	}

}


