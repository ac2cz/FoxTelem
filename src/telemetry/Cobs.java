package telemetry;

/**
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
 * PPP Consistent Overhead Byte Stuffing (COBS)
 * 
 * Ported from:
 * 
 * Version 00
 * PPP Working Group
 * Internet Draft
 * November 1997
 * 
 * COBS encapsulation is a simple reversible transformation that elim-
   inates all instances of hex 7E from the frame to be transmitted.
   This COBS encoding procedure is logically a two-step process,
   although in real implementations both steps are performed in a single
   loop for the sake of efficiency.  The first step ("zero elimination")
   eliminates all occurrences of zeroes from the data, while guarantee-
   ing to add at most no more than 0.5% to the data size.  This results
   in a data packet containing only byte values hex 01 to hex FF, and no
   zeroes.  The second step ("7E substitution") replaces all occurrences
   of hex 7E with hex 00, thereby producing a packet that does contain
   zeroes but contains no instances of hex 7E.

   The zero elimination step encodes any data packet using a series of
   COBS code blocks.  Each COBS code block begins with a single code
   byte, followed by zero or more data bytes.  The code byte determines
   how many data bytes follow.  The codes and their meanings are deter-
   mined such that all possible data packets can be encoded as a valid
   series of code blocks, and furthermore, even in the worst possible
   case, there exists no valid encoding that adds more than 0.5% over-
   head to the packet size. There is no pre-set limit to the length of
   packet that may be encoded. The value zero is never used as a code
   byte, nor does it ever appear as a data byte, which is why the output
   of COBS zero elimination never contains any instances of the value
   zero.
   
   The 7E substitution step allows a linear code range to be used for
   octet counts without concern for the potential end-of-frame marker in
   the middle of the code space.

The PPP/COBS codes and their meanings are listed below:

   Code (n)  Followed by:    Meaning
   --------  ------------    -------
    00                       Unused (framing character placeholder)
    01-CF    n-1 data bytes  The data bytes, plus implicit trailing zero
    D0       n-1 data bytes  The data bytes, no implicit trailing zero
    D1                       Unused (resume preempted packet)
    D2                       Unused (reserved for future use)
    D3-DF    nothing         a run of (n-D0) zeroes
    E0-FE    n-E0 data bytes The data bytes, plus two trailing zeroes
    FF                       Unused (PPP error)

   Code byte hex 00 is never used, in order to provide the required
   "zero elimination" property.

   Code byte hex D1 is never used (although value D1 may appear as a
   data byte).  If a COBS receiver observes that the first byte after a
   framing marker is value D1, then it means that this new "packet" of
   PPP data resumes transmission of a previously preempted packet
   
   Code byte hex D2 is never used (although value D2 may appear as a
   data byte).  Code byte hex D2 is reserved for future use.

   Code byte hex FF is never used (although value FF may appear as a
   data byte).  If a COBS receiver observes that the first byte after a
   framing marker is value FF, then this indicates that an error has
   occurred.
   
   The COBS zero elimination procedure effectively searches the packet
   for the first occurrence of value zero.  To simplify the encoding
   procedure, all packets are treated as though they end with a trailing
   zero at the very end, after the standard CRC.  This "phantom" octet
   of hex 00 is automatically discarded after decoding at the receiving
   end to correctly reconstruct the original packet data.
   
   The number of octets up to and including the first zero determines
   the code to be output.  If this number is 207 or fewer, then this
   number is output as the code byte, followed by the actual non-zero
   bytes themselves.  The zero is skipped and not output; the receiver
   will automatically add the zero back in as part of the decoding pro-
   cess.  If there are 207 or more non-zero bytes, then code hex D0 is
   output, followed by the first 207 non-zero bytes.  This process is
   repeated until all of the bytes of the packet (including the phantom
   trailing zero at the end) have been encoded.
   
   As an optional optimization, if the receiver has indicated its desire
   to receive zero-pair and zero-run codes in its COBS Configuration
   Option, then the transmitter MAY elect to encode zero pairs and zero
   runs more efficiently.  If a pair of 00 octets are found in the input
   data after 0 to 30 non-zero octets, then the count of non-zero octets
   plus E0 is output, followed by the non-zero octets, and both 00
   octets in the input data are skipped.  If a run of three to fifteen
   00 octets are found in the input data, then the count of these 00
   octets plus D0 is output and the 00 octets in the input data are
   skipped.
    
 *
 */
public class Cobs {

	int Unused       = 0x00; // Unused (framing character placeholder)
	int DiffZero     = 0x01; // Range 0x01 - 0xCE:
	int DiffZeroMax  = 0xCF; // n-1 explicit characters plus a zero
	static int Diff         = 0xD0; // 207 explicit characters, no added zero
	static int Resume       = 0xD1; // Unused (resume preempted packet)
	static int Reserved     = 0xD2; // Unused (reserved for future use)
	static int RunZero      = 0xD3; // Range 0xD3 - 0xDF:
	int RunZeroMax   = 0xDF; // 3-15 zeroes
	static int Diff2Zero    = 0xE0; // Range 0xE0 - 0xFE:
	int Diff2ZeroMax = 0xFE; // 0-30 explicit characters plus 2 zeroes
	static int Error        = 0xFF; // Unused (PPP LCP renegotiation)

	
	private static boolean isDiff2Zero(int x) {
		if ((x & 0xE0) == (Diff2Zero & 0xE0)) return true;
		return false;
	}
			
	private static boolean isRunZero(int x) {
		 if ((x & 0xF0) == (RunZero   & 0xF0)) return true;
		 return false;
	}
	
	/*
	private int Tx(int x) {
	    if (x == 0x7E)
	    	return 0;
	    else return x;
	}
	 */
	
	private static int Rx(int x) {
	    if (x == 0)
	    	return 0x7E;
	    else return x;
	}
	
	/*
	* UnStuffData decodes data from the buffer
	* "ptr", writing the output to "dst". If the decoded data does not
	* fit within "dstlength" bytes or any other error occurs, then
	* UnStuffData returns NULL.
	*/
	public static int[] unStuffData(int[] ptr, int maxLength ) throws ArrayIndexOutOfBoundsException {
		int z = 0;
		int c = 0;
		int src_ptr = 0;
		int[] dst = new int[maxLength];
		int dst_ptr = 0;
		
		while (src_ptr < ptr.length) {
			c = Rx(ptr[src_ptr++]);
			if ( c == Error || c == Resume || c == Reserved) {
			            return null;
			} else if (c == Diff) {
			            z = 0;
			            c -= 1;
			} else if (isRunZero(c)) {
			            z = c & 0xF;
			            c = 0;
			} else if (isDiff2Zero(c)) {
			            z = 2;
			            c &= 0x1F;
			} else {
			            z = 1;
			            c -=1;
			}
			c -= 1;
			while (c >= 0) { // && dst_ptr < maxLength && src_ptr < ptr.length) {
				dst[dst_ptr++] = Rx(ptr[src_ptr++]); 
				c -=1;
			}
			z -= 1;
			while (z >= 0) {// && dst_ptr < maxLength) {
				dst[dst_ptr++] = 0;
				z -= 1;
			}
		}
		//dst = ''.join(chr(s) for s in dst[:-1]);
		if (dst_ptr >= maxLength) return null;
		if (dst_ptr < 2) return null;
		int[] out = new int[dst_ptr-1];
		for(int i=0; i< (dst_ptr-1); i++) // copy all but the last byte which is a dummy zero
			out[i] = dst[i];
		return out;
	}
}
