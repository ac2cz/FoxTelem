package telemetry;

import java.util.StringTokenizer;

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
 * This is a payload containing University of Washington experiment results.  It consists of the following:
 * A flag byte - bit 1 indicates overflow
 * A set of CAN Packets
 * 
 *
 */
public class PayloadRagAdac extends FoxFramePart {	
	
	public PayloadRagAdac(BitArrayLayout lay, int id, long uptime, int resets) {
		super(TYPE_RAG_TELEM,lay);
		captureHeaderInfo(id, uptime, resets);
	}
	
	public PayloadRagAdac(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_RAG_TELEM, date, st, lay);
	}
	
	protected void init() { 
		// nothing extra to init here
	}
	

	@Override
	public String toString() {
		copyBitsToFields();
		String s = "RAG TELEM PAYLOAD\n";
		s = s + "RESET: " + resets;
		s = s + "  UPTIME: " + uptime;
		s = s + "  TYPE: " + type + "\n";

		return s;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

}
