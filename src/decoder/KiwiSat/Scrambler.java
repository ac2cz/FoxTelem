package decoder.KiwiSat;

/**
 * 
 * @author chris
 *
 * This is the scrambler for G3RUH 9600bps packet.  Note that even the Sync words are scrambled.  The scrambling polynomial is 1 + X^12 + X^17. 
 * This means the currently transmitted bit is the EXOR of the current data bit, with the bits that were transmitted 12 and 17 bits earlier. 
 * Likewise the unscrambling operation simply EXORs the bit received now with those sent 12 and 17 bits earlier. The unscrambler perforce requires 
 * 17 bits to synchronise.  In java, the EXOR operator is ^
 */
public class Scrambler {
	boolean[] bits = new boolean[18]; // Size is 1 greater than the shift register needed because we hold the current value in space 0.
	
	boolean decode(boolean value) {
		bits[0] = value;
		boolean result = bits[0] ^ bits[12] ^ bits[17];
		// shift the bits to the right
		for (int i=bits.length-1; i>0; i--)
			bits[i] = bits[i-1];
		return result;
	}
}
