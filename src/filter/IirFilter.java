package filter;


/**
* An IIR (infinite impulse response) filter based in Engineerings and Scientists Guide to DSP Ch 19
* 
* <p>
* Formula:
* <pre>
*    y[n] = x[n] * a[0]  +  x[n-1] * a[1]  +  x[n-2] * a[2]  +  ...
*                        +  y[n-1] * b[1]  +  y[n-2] * b[2]  -  ...
* </pre> (x = input, y = output, a and b = filter coefficients)
*/
public class IirFilter {

 private int n1;       // size of input delay line
 private int n2;       // size of output delay line
 private double[] a;  
 private double[] b;  

 private double[] buf1;                         // input signal delay line (ring buffer)
 private double[] buf2;                         // output signal delay line (ring buffer)
 private int pos1;                         // current ring buffer position in buf1
 private int pos2;                         // current ring buffer position in buf2

 /**
  * Creates an IIR filter.
  *     
* @param coeffs The A and B coefficients. b[0] must be 1. It is not used.
  *
  */
 public IirFilter(double[] a, double[] b) {
	 this.a = a;
	 this.b = b;
     if (a.length < 1 || b.length < 1 || b[0] != 1.0) {
         throw new IllegalArgumentException("Invalid coefficients.");
     }
     n1 = a.length - 1;
     n2 = b.length - 1;
     buf1 = new double[n1];
     buf2 = new double[n2];
 }

 /**
  * Processes one input signal value and returns the next output signal
  * value.
  */
 public double filterDouble(double in) {
     double out = a[0] * in;
     
     for (int j = 1; j <= n1; j++) {
         int p = (pos1 + n1 - j) % n1;
         out += a[j] * buf1[p];
     }
     
     for (int j = 1; j <= n2; j++) {
         int p = (pos2 + n2 - j) % n2;
         out += b[j] * buf2[p];
     }
     if (n1 > 0) {
         buf1[pos1] = in;
         pos1 = (pos1 + 1) % n1;
     }
     if (n2 > 0) {
         buf2[pos2] = out;
         pos2 = (pos2 + 1) % n2;
     }
     return out;
 }

}

