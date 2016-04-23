package measure;

import com.pretty_tools.dde.DDEException;
import com.pretty_tools.dde.DDEMLException;
import com.pretty_tools.dde.client.DDEClientConversation;
import common.Log;

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
 */
public class SatPc32DDE {

	public static final String CONNECT_FAIL_ERROR_CODE = "0x400a";
	
	public String satellite;
	public double azimuth;
	public double elevation;
	public long downlinkFrequency;
	
	public boolean connect() {
		try
		{
		    final DDEClientConversation conversation = new DDEClientConversation();

		    conversation.connect("SatPC32", "SatPcDdeConv");
		    try
		    {
		        // Requesting DDE String
		        String ddeString = conversation.request("SatPcDdeItem");
		      //  Log.println("SatPC32: " + ddeString);
		        if (ddeString.length() > 0 && !ddeString.startsWith("**")) {
		        String parts[] = ddeString.split(" ");
		        satellite = parts[0].substring(2, parts[0].length());
		        azimuth = Double.parseDouble(parts[1].substring(2, parts[1].length()));
		        elevation = Double.parseDouble(parts[2].substring(2, parts[2].length()));
		        downlinkFrequency = Long.parseLong(parts[5].substring(2, parts[5].length()));
		        //System.out.println("Sat: " + satellite);
		        //System.out.println("Az: " + azimuth);
		        //System.out.println("El: " + elevation);
		        //System.out.println("Freq: " + downlinkFrequency);
		        
		        // Sending "close()" command
		        conversation.execute("[close()]");
		        return true;
		        } else {
		        	conversation.execute("[close()]");
			        return false;
			        	
		        }
		    }
		    finally
		    {
		        conversation.disconnect();
		    }
		}
		catch (DDEMLException e)
		{
			if (e.getErrorCode() == 0x400a)
				;// we can ignore failed connection error.  SatPC32 is not open
			else
		    Log.println("DDEMLException: 0x" + Integer.toHexString(e.getErrorCode())
		                       + " " + e.getMessage());
		    return false;
		}
		catch (DDEException e)
		{
		    Log.println("DDEException: " + e.getMessage());
		    return false;
		}
	}
}
