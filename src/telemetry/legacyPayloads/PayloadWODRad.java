package telemetry.legacyPayloads;

import java.util.StringTokenizer;

import common.Config;
import common.Spacecraft;
import decoder.FoxDecoder;
import telemetry.BitArrayLayout;

public class PayloadWODRad extends PayloadRadExpData {

	
	public PayloadWODRad(BitArrayLayout lay) {
		super(lay);
		// TODO Auto-generated constructor stub
	}

	public PayloadWODRad(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);	
	}
	
	@Override
	protected void init() {
		type = TYPE_WOD_EXP; // otherwise this will be just an expeiment record
		fieldValue = new int[layout.NUMBER_OF_FIELDS];
	}

	@Override
	public void captureHeaderInfo(int id, long uptime, int resets) {
		copyBitsToFields();
		this.id = id;
		this.reportDate = fileDateStamp();
	}
	
	@Override
	public void copyBitsToFields() {
		super.copyBitsToFields();
		resets = getRawValue(WOD_RESETS);
		uptime = getRawValue(WOD_UPTIME);
	}
	
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public WodRadiationTelemetry calculateTelemetryPalyoad() {
		WodRadiationTelemetry radTelem = new WodRadiationTelemetry(resets, uptime, Config.satManager.getLayoutByName(id, Spacecraft.WOD_RAD2_LAYOUT));
		calcFox1ETelemetry(radTelem);
		return radTelem;
		
	}
	
	

	@Override
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "WOD RADIATION EXPERIMENT DATA:\n";
		for (int i =0; i< PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE; i++) {
			s = s + FoxDecoder.hex(fieldValue[i]) + " ";
			// Print 8 bytes in a row
			if ((i+1)%8 == 0) s = s + "\n";
		}
		for (int i=PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE; i < layout.fieldName.length; i++) {
			s = s + layout.fieldName[i] + ": " + fieldValue[i] + ",   ";
			if ((i+1)%6 == 0) s = s + "\n";
		}
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
