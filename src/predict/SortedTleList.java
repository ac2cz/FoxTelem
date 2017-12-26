package predict;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTime;

import telemetry.SortedArrayList;

@SuppressWarnings("serial")
public class SortedTleList extends SortedArrayList<FoxTLE>{

	 /** The time at which we do all the calculations. */
    static final TimeZone TZ = TimeZone.getTimeZone("UTC:UTC");
    
	public SortedTleList(int i) {
		super(i);
		
	}

	 /**
     * Calculates the Julian Day of the Year.
     *
     * The function Julian_Date_of_Year calculates the Julian Date of Day 0.0 of {year}. This
     * function is used to calculate the Julian Date of any date by using Julian_Date_of_Year, DOY,
     * and Fraction_of_Day.
     *
     * Astronomical Formulae for Calculators, Jean Meeus, pages 23-25. Calculate Julian Date of 0.0
     * Jan aYear
     *
     * @param theYear the year
     * @return the Julian day number
     */
    protected static double julianDateOfYear(final double theYear) {

        final double aYear = theYear - 1;
        long i = (long)Math.floor(aYear / 100);
        final long a = i;
        i = a / 4;
        final long b = 2 - a + i;
        i = (long)Math.floor(365.25 * aYear);
        i += 30.6001 * 14;

        return i + 1720994.5 + b;
    }
	/**
     * The function Julian_Date_of_Epoch returns the Julian Date of an epoch specified in the format
     * used in the NORAD two-line element sets. It has been modified to support dates beyond the
     * year 1999 assuming that two-digit years in the range 00-56 correspond to 2000-2056. Until the
     * two-line element set format is changed, it is only valid for dates through 2056 December 31.
     *
     * @param epoch the Epoch
     * @return The Julian date of the Epoch
     */
    private static double juliandDateOfEpoch(final double epoch) {

        /* Modification to support Y2K */
        /* Valid 1957 through 2056 */
        double year = Math.floor(epoch * 1E-3);
        final double day = (epoch * 1E-3 - year) * 1000.0;

        if (year < 57) {
            year = year + 2000;
        }
        else {
            year = year + 1900;
        }

        return julianDateOfYear(year) + day;
    }
    
    /**
     * Read the system clock and return the number of days since 31Dec79 00:00:00 UTC (daynum 0).
     *
     * @param date the date we wan to get the offset for
     * @return the number of days offset
     */
    private static double calcCurrentDaynum(final DateTime date) {
        final long now = date.getMillis();
        final Calendar sgp4Epoch = Calendar.getInstance(TZ);
        sgp4Epoch.clear();
        sgp4Epoch.set(1979, Calendar.DECEMBER, 31, 0, 0, 0);
        final long then = sgp4Epoch.getTimeInMillis();
        final long millis = now - then;
        return millis / 1000.0 / 60.0 / 60.0 / 24.0;
    }

	/** 
	 * We want the most recent TLE that is before this date.  So we start with the most recent date and search backwards until
	 * we find a record with an earlier date.  We use the first record we find with an earlier date.  If we get to the end of the 
	 * list we use the last (earliest) record by default
	 * @param dateTime
	 * @return
	 */
	public FoxTLE getTleByDate(DateTime dateTime) {
		if (this.size() <= 0) return null;
		double julRequested = calcCurrentDaynum(dateTime) + 2444238.5;

		for (int i=size()-1; i>=0; i--) { 
			FoxTLE f = this.get(i);
			double julianDateEpoch = juliandDateOfEpoch(f.getEpoch());
			if (julianDateEpoch <= julRequested) {
					return get(i);
			}
		}
		return this.get(0);
	}
}
