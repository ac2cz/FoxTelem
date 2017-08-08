package predict;

import java.util.Calendar;
import java.util.Date;
import uk.me.g4dpz.satellite.TLE;

/**
 * FOX TLE will load and Save TLE to file.  It loads the 3 lines into strings and can then be saved as three strings to a file.  This also allows
 * two TLEs to be compared to see which is the most recent.  This is used in the sorted list of TLE
 * 
 * @author g0kla
 *
 */
public class FoxTLE extends TLE {

	public FoxTLE(String[] tle) throws IllegalArgumentException {
		super(tle);
		Date today = Calendar.getInstance().getTime();  
		this.setCreateddate(today);
	}

	public String toFileString() {
		String s = "";
		s = s + this.getCreateddate() + ",";
		s = s + this.getArgper() + ",";
		s = s + this.getBstar() + ",";
		s = s + this.getCatnum() + ",";
		s = s + this.getDrag() + ",";
		s = s + this.getEccn() + ",";
		s = s + this.getEo() + ",";
		s = s + this.getEpoch() + ",";
		s = s + this.getIncl() + ",";
		s = s + this.getMeanan() + ",";
		s = s + this.getMeanmo() + ",";
		s = s + this.getName() + ",";
		s = s + this.getNddot6() + ",";
		s = s + this.getOmegao() + ",";
		s = s + this.getOrbitnum() + ",";
		s = s + this.getRaan() + ",";
		s = s + this.getRefepoch() + ",";
		s = s + this.getSetnum() + ",";
		s = s + this.getXincl() + ",";
		s = s + this.getXmo() + ",";
		s = s + this.getXndt2o() + ",";
		s = s + this.getXno() + ",";
		s = s + this.getXnodeo() + ",";
		s = s + this.getYear() + ",";
		s = s + this.isDeepspace();
		return s;
	}				
	
}
