package telemetry;

import java.util.StringTokenizer;

public class WodRadiationTelemetry extends RadiationTelemetry {

	public static int MAX_RAD_TELEM_BYTES = 78;
	
	
	public WodRadiationTelemetry(int r, long u, BitArrayLayout l) {
		super(r, u, l);
	}

	public WodRadiationTelemetry(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);	
	}
	
	
	@Override
	protected void init() {
		type = TYPE_WOD_RAD_TELEM_DATA;
		//rawBits = new boolean[MAX_BYTES*8];
		//fieldValue = new int[layout.NUMBER_OF_FIELDS];
	}
	
	
}
