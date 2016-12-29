package decoder;

import java.util.Arrays;

public class ComplexBuffer extends Buffer
{
	/**
	 * Wrapper around float array containing interleaved I/Q samples
	 */
	public ComplexBuffer( float[] samples )
	{
		super( samples );
	}

	/**
	 * Creates a deep copy of the buffer 
	 */
	public ComplexBuffer copyOf()
	{
		float[] copy = Arrays.copyOf( mSamples, mSamples.length );
		
		return new ComplexBuffer( copy );
	}
}