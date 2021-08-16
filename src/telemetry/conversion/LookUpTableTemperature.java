package telemetry.conversion;

/**
 *  
 *  FOX 1 Telemetry Decoder
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
 *  Lookup table values for the thermistor AtoD
 *  For Transmit, Receive, PSU Card, EXP4 temperatures:
 * 
 *
 */
public class LookUpTableTemperature extends ConversionLookUpTable {
	
	//public static void main(String[] args) {
	//	TempLookUpTable tab = new TempLookUpTable();
	//	System.out.println(tab.lookupValue(2123));
	//}
	
	public LookUpTableTemperature() {
		super("LookUpTableTemperature");
		table.put(507,-55.0);
		table.put(510,-54.0);
		table.put(513,-53.0);
		table.put(517,-52.0);
		table.put(520,-51.0);
		table.put(524,-50.0);
		table.put(528,-49.0);
		table.put(533,-48.0);
		table.put(537,-47.0);
		table.put(542,-46.0);
		table.put(547,-45.0);
		table.put(553,-44.0);
		table.put(559,-43.0);
		table.put(565,-42.0);
		table.put(571,-41.0);
		table.put(577,-40.0);
		table.put(584,-39.0);
		table.put(592,-38.0);
		table.put(599,-37.0);
		table.put(607,-36.0);
		table.put(616,-35.0);
		table.put(625,-34.0);
		table.put(634,-33.0);
		table.put(644,-32.0);
		table.put(654,-31.0);
		table.put(664,-30.0);
		table.put(675,-29.0);
		table.put(687,-28.0);
		table.put(699,-27.0);
		table.put(711,-26.0);
		table.put(724,-25.0);
		table.put(738,-24.0);
		table.put(752,-23.0);
		table.put(766,-22.0);
		table.put(782,-21.0);
		table.put(797,-20.0);
		table.put(814,-19.0);
		table.put(830,-18.0);
		table.put(848,-17.0);
		table.put(866,-16.0);
		table.put(885,-15.0);
		table.put(904,-14.0);
		table.put(924,-13.0);
		table.put(944,-12.0);
		table.put(965,-11.0);
		table.put(987,-10.0);
		table.put(1009,-9.0);
		table.put(1032,-8.0);
		table.put(1056,-7.0);
		table.put(1080,-6.0);
		table.put(1104,-5.0);
		table.put(1130,-4.0);
		table.put(1156,-3.0);
		table.put(1182,-2.0);
		table.put(1209,-1.0);
		table.put(1237,0.0);
		table.put(1265,1.0);
		table.put(1293,2.0);
		table.put(1322,3.0);
		table.put(1352,4.0);
		table.put(1382,5.0);
		table.put(1413,6.0);
		table.put(1444,7.0);
		table.put(1475,8.0);
		table.put(1507,9.0);
		table.put(1539,10.0);
		table.put(1571,11.0);
		table.put(1604,12.0);
		table.put(1637,13.0);
		table.put(1670,14.0);
		table.put(1703,15.0);
		table.put(1737,16.0);
		table.put(1771,17.0);
		table.put(1805,18.0);
		table.put(1839,19.0);
		table.put(1873,20.0);
		table.put(1907,21.0);
		table.put(1941,22.0);
		table.put(1976,23.0);
		table.put(2010,24.0);
		table.put(2044,25.0);
		table.put(2078,26.0);
		table.put(2112,27.0);
		table.put(2146,28.0);
		table.put(2179,29.0);
		table.put(2213,30.0);
		table.put(2246,31.0);
		table.put(2279,32.0);
		table.put(2312,33.0);
		table.put(2344,34.0);
		table.put(2376,35.0);
		table.put(2408,36.0);
		table.put(2439,37.0);
		table.put(2471,38.0);
		table.put(2501,39.0);
		table.put(2532,40.0);
		table.put(2562,41.0);
		table.put(2591,42.0);
		table.put(2621,43.0);
		table.put(2649,44.0);
		table.put(2678,45.0);
		table.put(2706,46.0);
		table.put(2733,47.0);
		table.put(2760,48.0);
		table.put(2787,49.0);
		table.put(2813,50.0);
		table.put(2838,51.0);
		table.put(2863,52.0);
		table.put(2888,53.0);
		table.put(2912,54.0);
		table.put(2936,55.0);
		table.put(2959,56.0);
		table.put(2982,57.0);
		table.put(3004,58.0);
		table.put(3026,59.0);
		table.put(3048,60.0);
		table.put(3069,61.0);
		table.put(3089,62.0);
		table.put(3109,63.0);
		table.put(3129,64.0);
		table.put(3148,65.0);
		table.put(3167,66.0);
		table.put(3185,67.0);
		table.put(3203,68.0);
		table.put(3220,69.0);
		table.put(3237,70.0);
		table.put(3254,71.0);
		table.put(3270,72.0);
		table.put(3286,73.0);
		table.put(3301,74.0);
		table.put(3316,75.0);
		table.put(3331,76.0);
		table.put(3346,77.0);
		table.put(3360,78.0);
		table.put(3373,79.0);
		table.put(3386,80.0);
		table.put(3399,81.0);
		table.put(3412,82.0);
		table.put(3424,83.0);
		table.put(3436,84.0);
		table.put(3448,85.0);
		table.put(3459,86.0);
		table.put(3470,87.0);
		table.put(3481,88.0);
		table.put(3492,89.0);
		table.put(3502,90.0);
		table.put(3512,91.0);
		table.put(3522,92.0);
		table.put(3531,93.0);
		table.put(3540,94.0);
		table.put(3549,95.0);
		table.put(3558,96.0);
		table.put(3567,97.0);
		table.put(3575,98.0);
		table.put(3583,99.0);
		table.put(3591,100.0);
		table.put(3599,101.0);
		table.put(3606,102.0);
		table.put(3613,103.0);
		table.put(3620,104.0);
		table.put(3627,105.0);
		table.put(3634,106.0);
		table.put(3640,107.0);
		table.put(3647,108.0);
		table.put(3653,109.0);
		table.put(3659,110.0);
		table.put(3665,111.0);
		table.put(3671,112.0);
		table.put(3676,113.0);
		table.put(3682,114.0);
		table.put(3687,115.0);
		table.put(3692,116.0);
		table.put(3697,117.0);
		table.put(3702,118.0);
		table.put(3707,119.0);
		table.put(3711,120.0);
		table.put(3716,121.0);
		table.put(3720,122.0);
		table.put(3725,123.0);
		table.put(3729,124.0);
		table.put(3733,125.0);
		table.put(3737,126.0);
		table.put(3741,127.0);
		table.put(3744,128.0);
		table.put(3748,129.0);
		table.put(3752,130.0);
		table.put(3755,131.0);
		table.put(3759,132.0);
		table.put(3762,133.0);
		table.put(3765,134.0);
		table.put(3768,135.0);
		table.put(3771,136.0);
		table.put(3774,137.0);
		table.put(3777,138.0);
		table.put(3780,139.0);
		table.put(3783,140.0);
		table.put(3786,141.0);
		table.put(3788,142.0);
		table.put(3791,143.0);
		table.put(3793,144.0);
		table.put(3796,145.0);
		table.put(3798,146.0);
		table.put(3801,147.0);
		table.put(3803,148.0);
		table.put(3805,149.0);
		table.put(3807,150.0);

	}

}
