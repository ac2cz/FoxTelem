package telemetry;

import java.util.StringTokenizer;

import common.Config;
import common.Log;
import common.Spacecraft;
import decoder.Decoder;
import decoder.FoxDecoder;
import telemetry.uw.CanPacket;

public class PayloadWODUwExperiment extends PayloadUwExperiment {

	public static final int WOD_RESETS_FIELD = 1;
	public static final int WOD_UPTIME_FIELD = 2;
	public static final String WOD_RESETS = "WODTimestampReset";
	public static final String WOD_UPTIME = "WODTimestampUptime";
	public static final int CRC_ERROR_FIELD = 3;
	public static final String CRC_ERROR = "crcError";
	public static final int PAD2_FIELD = 4;
	public static final String PAD2 = "pad2";
		
	public PayloadWODUwExperiment(BitArrayLayout lay) {
		super(lay);
	}

	public PayloadWODUwExperiment(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);	
	}
	
	@Override
	protected void init() {
		type = TYPE_UW_WOD_EXPERIMENT;
	}
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean savePayloads() {
		copyBitsToFields(); // make sure reset / uptime correct
		if (!Config.payloadStore.add(getFoxId(), getUptime(), getResets(), this))
			return false;
		for (CanPacket p : canPackets)
			if (!Config.payloadStore.add(getFoxId(), getUptime(), getResets(), p))
				return false;
		return true;

	}
	
	@Override
	public void copyBitsToFields() {
		super.copyBitsToFields();
		resets = getRawValue(WOD_RESETS);
		uptime = getRawValue(WOD_UPTIME);
	}
	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = "UW WOD EXPERIMENT PAYLOAD - " + canPackets.size() + " CAN PACKETS\n";
		s = s + "RESET: " + getRawValue(WOD_RESETS);
		s = s + "  UPTIME: " + getRawValue(WOD_UPTIME);
		s = s + "  OVERFLOW FLAG: " + rawBits[0] + "\n";
		//for (int p=0; p < canPackets.size(); p++) {
		//	s = s + canPackets.get(p).toString() + "    " ;
		//	if ((p+1)%3 == 0) s = s + "\n";
		//}
		//s=s+"\n";

		return s;
	}
	
	/**
	 * Load this framePart from a file, which has been opened by a calling method.  The string tokenizer contains a 
	 * set of tokens that represent the raw values to be loaded into the fields.
	 * The framePart header has already been loaded by the calling routine, which had to work out the type first
	 * @param st
	 */
	protected void load(StringTokenizer st) {
//		satAltitude = Double.valueOf(st.nextToken()).doubleValue();
		super.load(st);
	}
}
