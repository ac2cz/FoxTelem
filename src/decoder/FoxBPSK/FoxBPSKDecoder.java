package decoder.FoxBPSK;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import common.Config;
import common.Log;
import common.Performance;
import common.Spacecraft;
import decoder.Decoder;
import decoder.SourceAudio;
import gui.MainWindow;
import telemetry.Format.FormatFrame;
import telemetry.Format.FormatHeader;
import telemetry.Format.TelemFormat;
import telemetry.frames.Frame;

public abstract class FoxBPSKDecoder extends Decoder {

	/**
	 * This holds the stream of bits that we have not decoded. Once we have several
	 * SYNC words, this is flushed of processed bits.
	 */
//	protected FoxBPSKBitStream bitStream = null;  // Hold bits until we turn them into decoded frames
	
	public FoxBPSKDecoder(String n, SourceAudio as, int chan, TelemFormat telemFormat) {
		super(n, as, chan, telemFormat);
		bitStream = new FormatBitStream(this, telemFormat, true);
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

		Spacecraft sat = null;
		for (Frame decodedFrame : frames) {
			if (decodedFrame != null && !decodedFrame.corrupt) {
				Performance.startTimer("Store");
				// Successful frame
				eyeData.lastErasureCount = decodedFrame.rsErasures;
				eyeData.lastErrorsCount = decodedFrame.rsErrors;
				//eyeData.setBER(((bitStream.lastErrorsNumber + bitStream.lastErasureNumber) * 10.0d) / (double)bitStream.SYNC_WORD_DISTANCE);
				if (Config.storePayloads) {

					FormatFrame hsf = (FormatFrame)decodedFrame;
					FormatHeader header = hsf.getHeader();
					sat = Config.satManager.getSpacecraft(header.id);
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
				if (MainWindow.frame != null) // then we are in GUI mode
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() { MainWindow.setTotalDecodes();}
					});
				} catch (InvocationTargetException e1) {
					// Hopefully we never see this, but log it if we do
					e1.printStackTrace(Log.getWriter());
				} catch (InterruptedException e1) {
					// Hopefully we never see this, but log it if we do
					e1.printStackTrace(Log.getWriter());
				}
				Performance.endTimer("Store");
			} else {
				if (Config.debugBits) Log.println("SYNC marker found but frame not decoded\n");
				//clockLocked = false;
			}
		}
	}

}


