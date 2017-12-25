package telemetry;

import java.util.StringTokenizer;

import org.joda.time.DateTime;

import common.Config;
import common.FoxSpacecraft;
import common.Spacecraft;
import decoder.FoxDecoder;
import predict.PositionCalcException;
import uk.me.g4dpz.satellite.SatPos;

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
		type = TYPE_WOD_RAD;
		fieldValue = new int[layout.NUMBER_OF_FIELDS];
	}

	@Override
	public void captureHeaderInfo(int id, long uptime, int resets) {
		copyBitsToFields();
		this.id = id;
		this.captureDate = fileDateStamp();
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
		/*
		for (int k=0; k<RadiationTelemetry.MAX_RAD_TELEM_BYTES; k++) { 
			radTelem.addNext8Bits(fieldValue[k]);
		}
		radTelem.copyBitsToFields();
		// Now we copy the extra Fox Fields at the end, but we put them directly in the fields.  Fox computer is little endian, but the data so far
		// was big endian.  We could remember that and convert each part correctly, or we can leverage the fact that the extra Fox Fields we already
		// converted correctly in the core radiation  record.
		// Note that subsequently calling copyBitsToFields will eradicate this copy, so we add a BLOCK COPY BITS boolean
		radTelem.blockCopyBits = true;
		copyFieldValue(EXP1_BOARD_NUM, radTelem);
		copyFieldValue(WOD_RESETS, radTelem);
		copyFieldValue(WOD_UPTIME, radTelem);
		copyFieldValue(WOD_CRC_ERROR, radTelem);
		*/
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
