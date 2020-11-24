package decoder.FoxBPSK;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Performance;
import decoder.Decoder;
import decoder.SourceAudio;
import gui.MainWindow;
import telemetry.Frame;
import telemetry.TelemFormat;
import telemetry.FoxBPSK.FoxBPSKFrame;
import telemetry.FoxBPSK.FoxBPSKHeader;

public abstract class FoxBPSKDecoder extends Decoder {

	/**
	 * This holds the stream of bits that we have not decoded. Once we have several
	 * SYNC words, this is flushed of processed bits.
	 */
//	protected FoxBPSKBitStream bitStream = null;  // Hold bits until we turn them into decoded frames
	
	public FoxBPSKDecoder(String n, SourceAudio as, int chan, TelemFormat telemFormat) {
		super(n, as, chan);
		bitStream = new FoxBPSKBitStream(this, telemFormat);

	}
	
	public abstract double[] getBasebandData();

	public abstract double[] getBasebandQData();
	public abstract double[] getPhasorData();
	
	/**
	 * Determine if the bit sampling buckets are aligned with the data. This is calculated when the
	 * buckets are sampled
	 * 
	 */
	@Override
	public int recoverClockOffset() {

		return 0;//clockOffset;
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

	/**
	 *  Decode the frame
	 */
	protected void processPossibleFrame(ArrayList<Frame> frames) {

		FoxSpacecraft sat = null;
		for (Frame decodedFrame : frames) {
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
					int newReset = sat.getCurrentReset(header.resets, header.uptime);
					hsf.savePayloads(Config.payloadStore, sat.hasModeInHeader, newReset);

					// Capture measurements once per payload or every 5 seconds ish
					addMeasurements(header.id, newReset, header.uptime, decodedFrame, decodedFrame.rsErrors, decodedFrame.rsErasures);
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
	}

}


