/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package decoder;

import java.io.Serializable;

import common.Log;


/**
 * Complex sample and related utility methods 
 */
public class Complex implements Serializable
{
	private static final long serialVersionUID = 1L;

    private double mLeft;
	private double mRight;
	
	public Complex( double left, double right )
	{
		mLeft = left;
		mRight = right;
	}
	
	public Complex()
	{
		this( 0.0f, 0.0f );
	}
	
	public void setInphase( double inphase )
	{
		mLeft = inphase;
	}
	
	public void setQuadrature( double quadrature )
	{
		mRight = quadrature;
	}
	
	public void setValues( double inphase, double quadrature )
	{
		mLeft = inphase;
		mRight = quadrature;
	}

	public Complex copy()
	{
		return new Complex( mLeft, mRight );
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "I:" );
		sb.append( mLeft );
		sb.append( " Q:" );
		sb.append( mRight );
		
		return sb.toString();
	}
	
	/**
	 * Returns a new sample representing the conjugate of this one
	 */
	public Complex conjugate()
	{
		return new Complex( mLeft, -mRight );
	}

	/**
	 * Multiplies this sample by the scalor value
	 */
	public void multiply( double scalor )
	{
		mLeft *= scalor;
		mRight *= scalor;
	}
	
	public static Complex multiply( Complex sample, double scalor )
	{
		return new Complex( sample.left() * scalor, 
								  sample.right() * scalor );
	}
	
	/**
	 * Calculates the inphase component of multiplying two complex numbers, A and B.
	 */
	public static double multiplyInphase( double inphaseA, double quadratureA, double inphaseB, double quadratureB )
	{
		return ( inphaseA * inphaseB ) - ( quadratureA * quadratureB );
	}
	
	/**
	 * Calculates the quadrature component of multiplying two complex numbers, A and B.
	 */
	public static double multiplyQuadrature( double inphaseA, double quadratureA, double inphaseB, double quadratureB )
	{
		return ( quadratureA * inphaseB ) + ( inphaseA * quadratureB );
	}
	
	/**
	 * Multiplies this sample by the multiplier sample
	 */
	public void multiply( Complex multiplier )
	{
		double inphase = multiplyInphase( inphase(), quadrature(), 
				multiplier.inphase(), multiplier.quadrature() );
		double quadrature = multiplyQuadrature( inphase(), quadrature(), 
				multiplier.inphase(), multiplier.quadrature() );
		
		mLeft = inphase;
		mRight = quadrature;
	}
	
	/**
	 * Multiplies both samples returning a new sample with the results
	 */
	public static Complex multiply( Complex sample1, Complex sample2 )
	{
		double inphase = multiplyInphase( sample1.inphase(), sample1.quadrature(), 
				sample2.inphase(), sample2.quadrature() );
		double quadrature = multiplyQuadrature( sample1.inphase(), sample1.quadrature(), 
				sample2.inphase(), sample2.quadrature() );

		return new Complex( inphase, quadrature );
	}

	public static Complex multiply( Complex sample, 
										  double inphase, double quadrature )
	{
		double i = multiplyInphase( sample.inphase(), sample.quadrature(), 
				inphase, quadrature );
		double q = multiplyQuadrature( sample.inphase(), sample.quadrature(), 
				inphase, quadrature );
		
		return new Complex( i, q );
	}
	
	public static Complex multiply( double inphase, double quadrature, Complex sample )
	{
		double i = multiplyInphase( inphase, quadrature, sample.inphase(), sample.quadrature() );
		double q = multiplyQuadrature( inphase, quadrature, sample.inphase(), sample.quadrature() );
		
		return new Complex( i, q );
	}

	public static Complex multiply( double inphaseA, double quadratureA, double inphaseB, double quadratureB )
	{
		double i = multiplyInphase( inphaseA, quadratureA, inphaseB, quadratureB );
		double q = multiplyQuadrature( inphaseA, quadratureA, inphaseB, quadratureB );
		
		return new Complex( i, q );
	}
	
	/**
	 * Adds the adder sample value to this sample
	 */
	public void add( Complex adder )
	{
		mLeft += adder.left();
		mRight += adder.right();
	}

	/**
	 * Adds the two complex samples returning a new complex sample with the result
	 */
	public static Complex add( Complex first, Complex second )
	{
		return new Complex( first.left() + second.left(), 
								  first.right() + second.right() );
	}

	/**
	 * Adds the two complex samples returning a new complex sample with the result
	 */
	public static Complex subtract( Complex first, Complex second )
	{
		return new Complex( first.left() - second.left(), 
								  first.right() - second.right() );
	}

	/**
	 * Magnitude of this sample
	 */
	public double magnitude()
	{
		return (double)Math.sqrt( magnitudeSquared() );
	}
	
	public static double magnitude( double inphase, double quadrature )
	{
		return (double)Math.sqrt( ( inphase * inphase  ) + ( quadrature * quadrature ) );
	}

	/**
	 * Magnitude squared of this sample
	 */
	public double magnitudeSquared()
	{
		return norm();
	}

	/**
	 * Norm of this sample = ( i * i ) + ( q * q )
	 */
	public double norm()
	{
		return (double)( ( inphase() * inphase() ) + 
				  ( quadrature() * quadrature() ) );
	}
	
	/**
	 * Returns the vector length to 1 (unit circle)
	 */
	public void normalize()
	{
		double magnitude = magnitude();
		
		if( magnitude != 0 )
		{
			multiply( (double)( 1.0f / magnitude() ) );
		}
	}

	/**
	 * Returns the vector length to 1 (unit circle),
	 * avoiding square root multiplication
	 */
	public void fastNormalize()
	{
		multiply( (double)( 1.95f - magnitudeSquared() ) );
	}
	
	public double left()
	{
		return mLeft;
	}
	
	public double right()
	{
		return mRight;
	}

	public double inphase()
	{
		return mLeft;
	}
	
	/**
	 * Absolute value of in-phase component
	 */
	public double inPhaseAbsolute()
	{
		return Math.abs( mLeft );
	}

	public double quadrature()
	{
		return mRight;
	}
	
	/**
	 * Absolute value of quadrature component
	 */
	public double quadratureAbsolute()
	{
		return Math.abs( mRight );
	}

	public double x()
	{
		return mLeft;
	}
	
	public double y()
	{
		return mRight;
	}
	
	public double real()
	{
		return mLeft;
	}

	/**
	 * Absolute value of real component
	 */
	public double realAbsolute()
	{
		return Math.abs( mLeft );
	}

	public double imaginary()
	{
		return mRight;
	}

	/**
	 * Absolute value of imaginary component
	 */
	public double imaginaryAbsolute()
	{
		return Math.abs( mRight );
	}

	/**
	 * Returns the greater absolute value between left and right values 
	 */
	public double maximumAbsolute()
	{
		if( Math.abs( mLeft ) > Math.abs( mRight ) )
		{
			return Math.abs( mLeft );
		}
		else
		{
			return Math.abs(  mRight );
		}
	}

	/**
	 * Creates a new complex sample representing the angle with unit circle
	 * magnitude
	 * @param angle in radians
	 * @return
	 */
	public static Complex fromAngle( double angle )
	{
		return new Complex( (double)Math.cos( angle ), 
								  (double)Math.sin( angle ) );
	}

	/**
	 * Angle of this sample in radians
	 */
	public double angle()
	{
		return (double)Math.atan2( y(), x() );
	}

	/**
	 * Constrains the i and q quantities to +/- value. 
	 * 
	 * @param value - maximum absolute value
	 */
	public void clip( double value )
	{
		if( mLeft > value )
		{
			mLeft = value;
		}
		else if( mLeft < -value )
		{
			mLeft = -value;
		}
		
		if( mRight > value )
		{
			mRight = value;
		}
		else if( mRight < -value )
		{
			mRight = -value;
		}
	}

	/**
	 * Angle of this sample in degrees
	 */
	public double polarAngle()
	{
		return (double)Math.toDegrees( angle() );
	}

	/**
	 * Provides an approximate magnitude value for this sample.
	 */
	public double envelope()
	{
		return envelope( mLeft, mRight );
	}
	
	public static double envelope( double inphase, double quadrature )
	{
		double inphaseAbsolute = Math.abs( inphase );
		double quadratureAbsolute = Math.abs( quadrature );
		
		if( inphaseAbsolute > quadratureAbsolute )
		{
			return inphaseAbsolute + ( 0.4f * quadratureAbsolute );
		}
		else
		{
			return quadratureAbsolute + ( 0.4f * inphaseAbsolute );
		}
	}
	
	public static void main( String[] args )
	{
		double angle = Math.PI;  //180 degrees
		
		Complex s1 = new Complex( (double)Math.sin( angle ), (double)Math.cos( angle ) );

		Complex tap = new Complex( 1.0f, -0.1f );

		Complex convolved = Complex.multiply( s1, tap );
		
		Log.println( "s: " + s1.toString() +
					" t: " + tap.toString() +
					" convolved: " + convolved.toString() );
		
	}
}
