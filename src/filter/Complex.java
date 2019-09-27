package filter;

public class Complex {
	double i;
	double q;
	
	public Complex(double i, double q) {
		this.i = i;
		this.q = q;
	}
	
	public double geti() { return i;}
	public double getq() { return q;}
	
	public double magnitude() {
		return Math.sqrt(i*i + q*q);
	}
	
	public void multiply(double value) {
		i = i * value;
		q = q * value;
	}
	
	public void normalize()
	{
		double magnitude = magnitude();
		
		if( magnitude != 0 )
		{
			multiply( (double)( 1.0f / magnitude() ) );
		}
	}
}
