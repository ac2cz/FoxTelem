package predict;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import uk.me.g4dpz.satellite.TLE;

/**
 * FOX TLE will load and Save TLE to file.  It loads the 3 lines into strings and can then be saved as three strings to a file.  This also allows
 * two TLEs to be compared to see which is the most recent.  This is used in the sorted list of TLE
 * 
 * @author g0kla
 *
 */
@SuppressWarnings("serial")
public class FoxTLE extends TLE implements Comparable<FoxTLE> {
	String[] tleText = new String[3]; // The oritinal text so we can resave to file
	
	public FoxTLE(String[] tle) throws IllegalArgumentException {
		super(tle);
		for (int i=0; i<3; i++) 
			tleText[i] = new String(tle[i]);
		Date today = Calendar.getInstance().getTime();  
		this.setCreateddate(today);
	}
	
	public FoxTLE(TLE tle) throws IllegalArgumentException {
		super(tle);
		Date today = Calendar.getInstance().getTime();  
		this.setCreateddate(today);
	}
	

	public static SortedTleList importFoxSat(final InputStream fileIS) throws IOException {

        final SortedTleList importedSats = new SortedTleList(10);

        final BufferedReader buf = new BufferedReader(new InputStreamReader(fileIS));
        String readString;

        int j = 0;

        final String[] lines = new String[3];

        while ((readString = buf.readLine()) != null) {

            switch (j) {
                case 0:
                case 1:
                    lines[j] = readString;
                    j++;
                    break;
                case 2:
                    lines[j] = readString;
                    j = 0;
                    importedSats.add(new FoxTLE(lines));
                    break;
                default:
                    break;
            }
        }

        return importedSats;
    }
	
	public String toFileString() {
		String s = "";
		s = s + tleText[0] + "\n";
		s = s + tleText[1] + "\n";
		s = s + tleText[2] + "\n";
		return s;
	}

	@Override
	/**
	 * Duplicate records have the same sat name and EPOCH
	 * They are sorted by EPOCH
	 */
	public int compareTo(FoxTLE p) {
		if (this.getName().equalsIgnoreCase(p.getName()) && this.getEpoch() == p.getEpoch()) 
			return 0;
		else if (this.getEpoch() < p.getEpoch())
			return -1;
		else if (this.getEpoch() > p.getEpoch())
			return +1;
		return +1;
	}				
	
	
}
