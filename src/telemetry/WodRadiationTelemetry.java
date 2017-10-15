package telemetry;

import java.util.StringTokenizer;

public class WodRadiationTelemetry extends RadiationTelemetry {

	public WodRadiationTelemetry(int r, long u, BitArrayLayout l) {
		super(r, u, l);
		// TODO Auto-generated constructor stub
	}

	public WodRadiationTelemetry(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);	
	}
}
