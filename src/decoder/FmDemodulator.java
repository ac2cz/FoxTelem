package decoder;

import common.Config;

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
 * To decode FM, we convert the frequency deviation to phase and detect the phase change
 * We have the signal in phase and quadrature.  The amplitude of each of these correspond to the real and 
 * imaginary parts of the signal.  So, we can calculate the angle of the signal, theta using:
 *
 * tan(theta) = Q/I, theta = arctan (q/i)
 * 
 * 
 * The atan demodulator is based on code in SDR Trunk 
 * Copyright (C) 2014 Dennis Sheirer
 *     
 * SDR Trunk is also released under the GNU GPL, and details are below:
 *
 */
public class FmDemodulator {
	protected double[] i = new double[3];
	protected double[] q = new double[3];
	double lastPhase = 0d;
	double gain = 0.5;
	double limiti;
	double limitq;
	
	
	public FmDemodulator() {
		i[0] = 0.1f;
		q[0] = 0.1f;
		i[1] = 0.1f;
		q[1] = 0.1f;
		i[2] = 0.1f;
		q[2] = 0.1f;
	}
	
	
	public double demodulateAM(double in, double qn) {

		double mag = Math.sqrt(in*in + qn * qn);
		return mag;
	}

	/**
	 * Arctan method. We can calculate the instantaneous angle from I and Q
	 * @param in
	 * @param qn
	 * @return
	 */
	public double atanDemodulate(double in, double qn) {
		double I,Q; 
		double angle = 0;;
		
		/**
		 * Multiply the current sample against the complex conjugate of the 
		 * previous sample to derive the phase delta between the two samples
		 * 
		 * Negating the previous sample quadrature produces the conjugate
		 */
		I = in * i[0] - qn * -q[0];
		Q = qn * i[0] + in * -q[0];
		i[0] = in;
		q[0] = qn;
		
		if (I == 0) {
			I=1E-20; // small value to prevent divide by zero error
			//angle = 0;
		} 
		/**
		 * Use the arc-tangent of imaginary (q) divided by real (i) to
		 * get the phase angle (+/-) which was directly manipulated by the
		 * original message waveform during the modulation.  This value now
		 * serves as the instantaneous amplitude of the demodulated signal
		 */
		double denominator = 1.0d / I;
		angle = Math.atan( (double)Q * denominator );

		// If both real and imaginary parts are negative, need to subtract PI radians
		if (I < 0 && Q < 0) {
			angle = angle - Math.PI;
		}
		if (I < 0 && Q >= 0) {
			angle = angle + Math.PI;
		}

		return angle * gain;
	}

	/**
	 * We pass in the current values for i and q
	 * 
	 *  * To measure the change in frequency, we want d-theta/dt and we don’t want to use arctan because its hard to do at speed.
	 *
	 * r(t) = q(t)/i(t)
	 *
	 * Derivative of arctan is the identity 1/(1+r^2)
	 * Therefore d-theta/dt = 1/(1+r^2)( d[r(t)/d(t) )   (1)
	 * Also, d[r(t)]/dt = d[q(t)/i(t)/d(t) and there is an identity for the derivative of a ratio, so
	 * d[r(t)/d(t) = ( i(t) * d[q(t)]/d(t) - q(t) * d[i(t)]/d(t) ) /i^2(t)
	 * 
	 * So, using this result in equation (1) above gives
	 * d-theta/d(t) = (1/(1 + r^2)(  ( i(t) * d[q(t)]/d(t) - q(t) * d[i(t)]/d(t) ) /i^2(t) )
	 * 
	 * Replacing r(t) with q(t)/i(t) gives:
	 * d-theta/d(t) = (1/(1 + (q(t)^2/i(t)^2)(  ( i(t) * d[q(t)]/d(t) - q(t) * d[i(t)]/d(t) ) /i^2(t) )
	 * 
	 * Multiply by i^2(t)
	 * d-theta/d(t) = i(t)d[q(t)]/d(t) - q(t)d[i(t)]/d(t) 
	 *                   ------------------------------------
	 *                           i^2(t) + q^2(t) 
	 *
	 * Using the central difference method for differentiation gives:
	 * d-theta(n) = i(n-1) * (q(n) - q(n-2)) - q(n-1) * (i(n) - i(n-2))
	 *                 -----------------------------------------------------------
	 *                              2* (i(n-1)^2 + q(n-1)^2)
	 *
	 * If this is pure fm signal and is limited, then the denominator is probably a constant and does not need to be calculated for each sample.
	 *
	 * i(n) is i[2]
	 * i(n-1) is i[1]
	 * i(n-2) is i[0]
	 * @param i
	 * @param q
	 * @return
	 */
	public double demodulate(double in, double qn) {
		if (Config.useLimiter) {
		limiter(in, qn);
		
		}
		i[0] = i[1];
		q[0] = q[1];
		i[1] = i[2];
		q[1] = q[2];
		
		if (Config.useLimiter) {
		i[2] = limiti;
		q[2] = limitq;
		} else {
			i[2] = in;
			q[2] = qn;
		}
		
		// it simplifies to: Demodn={Qn*In-1  -  In*Qn-1}/{In2+Qn2}
		
		double gain = 0.5d; // magic number of 1/2 seems to work best
		double num = i[1] * ( q[2] - q[0] ) - q[1] * ( i[2] - i[0] );
		//double num = q[2] * i[1]  - i[2] * q[1];
		double den = (i[1]*i[1] + q[1]*q[1]); 
		
		double deltafreq =  gain* (num/den);
	
		if (Double.isNaN(deltafreq)) {
			//Log.println("FM NaN: num:"+num+" den:"+den);
			deltafreq = 0; // make sure we don't get locked in a bad position
		}
		return deltafreq;
	}
	
	/**
	 * FIXME:  Very inefficient limiter for testing...
	 * @param i
	 * @param q
	 */
	private void limiter(double i, double q) {
		double f = Math.atan2(q, i);
		limiti = Math.cos(f);
		limitq = Math.sin(f);
	}

}
