package decoder;

import common.Log;
import filter.CosOscillator;
import filter.DotProduct;
import filter.RootRaisedCosineFilter;
import filter.SinOscillator;

/**
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
 * Based on KA9Q PSK signal searcher algorithm
 * Repeatedly demodulate at a specified carrier frequency for all symbol offsets
 * record one that gives greatest demodulated energy
 * 
 * @author chris
 *
 */
public class PskSearcher implements Runnable {
	double energy;            // Total demodulator output energy (output)
	double[] samples;         // samples to be processed by this job (input)
	double cphase_inc_start;     // First carrier frequency to try (phase increment / sample, 2^32 = 2 pi radians) (input)
	double cphase_inc_step;      // Increment of carrier frequency to try (phase increment / sample, 2^32 = 2 pi radians) (input)
	int nfreq;                // Number of frequencies to search, starting at cphase_inc_start (input)
	double cphase_inc;           // Optimum carrier frequency increment (output)
	int symphase;             // Optimum Symbol timing offset (output)
	int Ntaps;
	int BUFFER_SIZE;
	int bucketSize;
	double frequency; // carrier frequency of max energy
	
	DotProduct matchedFilter;
	
	CosOscillator cos;
	SinOscillator sin;
	
	boolean running = true;
	public boolean done = false;

	public PskSearcher(double[] samples, double cphase_inc_start, double cphase_inc_step, int nfreq,
			int BUFFER_SIZE, int bucketSize, int sampleRate) {
		this.samples = samples;
		this.cphase_inc_start = cphase_inc_start;
		this.cphase_inc_step = cphase_inc_step;
		this.nfreq = nfreq;
		this.BUFFER_SIZE = BUFFER_SIZE;
		this.bucketSize = bucketSize;
		
		matchedFilter = new DotProduct();
		Ntaps = matchedFilter.getNumOfTaps();
		
		cos = new CosOscillator(sampleRate, cphase_inc_start);
		sin = new SinOscillator(sampleRate, cphase_inc_start);
		
		//Log.println("SEARCHER STARTED: Start Freq: " + cos.getFrequency());
		//Log.println("  searching: " + nfreq + " steps");
		double endInc = cphase_inc_step * nfreq;
		double endFreq = endInc * sampleRate / (2 * Math.PI);
		//Log.println("  to: " + (cos.getFrequency() + endFreq));
	}

	public synchronized double getEnergy() { return energy; }
	public synchronized int getNfreq() { return nfreq; }
	public synchronized double getCphaseInc() { return cphase_inc; }
	public synchronized int getSymphase() { return symphase; }
	public synchronized double getFrequency() { return frequency; }
	
	public void stop() {
		running = false;
	}

	@Override
	public void run() {

		double[] baseband_i = new double[BUFFER_SIZE];
		double[] baseband_q = new double[BUFFER_SIZE];
		double cphase_inc;
		int j;

		energy = 0;
		symphase = -1; // to keep compiler happy about uninitialized variables
		for(j=0,cphase_inc = cphase_inc_start; j < nfreq; j++,cphase_inc += cphase_inc_step){
			int i,offset;
			cos.setPhaseIncrement(cphase_inc);
			sin.setPhaseIncrement(cphase_inc);

			// Downconvert chunk of samples to baseband 
			cos.setPhase(0);
			sin.setPhase(0);
			for(i=0; i < BUFFER_SIZE; i++){
				baseband_i[i] = samples[i] * cos.nextSample();
				baseband_q[i] = samples[i] * sin.nextSample();
			}	

			// Perform demodulation for all possible symbol timings
			// looking for maximum energy
			for(offset=0; offset < bucketSize; offset++){ // For every symbol timing offset
				int bb_p;
				double tlast_i,tlast_q;
				double offsetEnergy = 0;

				tlast_i = 0;
				tlast_q = 0;

				for(bb_p = offset; bb_p + Ntaps < BUFFER_SIZE; bb_p += bucketSize){ // For every symbol in baseband buffer
					double fi,fq;
					double symbol;

					// Demodulate through matched filter, compute baseband energy
					fi = matchedFilter.dotprod(baseband_i, bb_p);
					fq = matchedFilter.dotprod(baseband_q, bb_p);

					// Dot product of previous and current center complex samples gives differentially demodulated symbol
					symbol = fi * tlast_i + fq * tlast_q;

					offsetEnergy += (double)symbol * symbol;
					tlast_i = fi;
					tlast_q = fq;
				}
				// Keep track of frequency and timing that gives maximum energy
				if(offsetEnergy >= this.energy){ // Guarantee at least one will be taken
					this.cphase_inc = cphase_inc;
					this.energy = offsetEnergy;
					this.symphase = offset;
					this.frequency = cos.getFrequency();
					//Log.println("  Searcher Inc:"+this.cphase_inc +" E:" + energy + " O:" + symphase);
				}
			}
			//Log.println("  PHASE:"+this.cphase_inc +" Freq: " + cos.getFrequency() + " E:" + energy);
		}

		done = true;
		running = false;
		// wait to allow data to be collected
		while(running) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//Log.println("SEARCHER ENDED: End Freq: " + cos.getFrequency());
		//Log.println("  Start Phase: "+cphase_inc_start+" Inc:"+this.cphase_inc +" E:" + energy + " O:" + symphase);
	}


}
