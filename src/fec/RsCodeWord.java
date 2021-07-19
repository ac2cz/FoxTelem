package fec;

import common.Config;
import common.Log;

/**
 * 
 * This class is mostly based on an extract of a Java port of Phil Karn's and J R Miller's AO-40 FEC decoder
 * by Phil Ashby, May 2013.  He saved me from having to port Phil's code myself. Thank you. He released it under the 
 * Non-Commercial Share Alike license:
 * http://creativecommons.org/licenses/by-nc-sa/3.0/
 * 
 * @author chris.e.thompson ac2cz
 *
 */
public class RsCodeWord  {

	private byte[] rsCodeWord = new byte[NN];
	private int[] erasurePositions = null;
	private int numberOfErasures = 0;

	private int nextByte = 0; // incremented when bytes added 1 by 1
	
	// Reed-Solomon decoder constants
	public static final int NN = 255;
	public static final int DATA_BYTES = 223;
	public static final int NROOTS = 32;
	private static final int FCR = 112;
	private static final int PRIM = 11;
	private static final int IPRIM = 116;
	private static final int A0 = NN;

	private int RSPAD = 0;  // This is passed into the decoder.  For slow speed we have a frame of 64 Bytes + 32 FEC Bytes, so the RS codeword needs 159 zero of padding at the front

	private int numberOfCorrections = 0; // The number of corrections we made.  Set to -1 if we failed to decode the block
	
	/* Tables for RS decoder */
	/* Galois field log/antilog tables */
	private static final int ALPHA_TO[] ={ // 256
		0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x87, 0x89, 0x95, 0xad, 0xdd, 0x3d, 0x7a, 0xf4,
		0x6f, 0xde, 0x3b, 0x76, 0xec, 0x5f, 0xbe, 0xfb, 0x71, 0xe2, 0x43, 0x86, 0x8b, 0x91, 0xa5, 0xcd,
		0x1d, 0x3a, 0x74, 0xe8, 0x57, 0xae, 0xdb, 0x31, 0x62, 0xc4, 0x0f, 0x1e, 0x3c, 0x78, 0xf0, 0x67,
		0xce, 0x1b, 0x36, 0x6c, 0xd8, 0x37, 0x6e, 0xdc, 0x3f, 0x7e, 0xfc, 0x7f, 0xfe, 0x7b, 0xf6, 0x6b,
		0xd6, 0x2b, 0x56, 0xac, 0xdf, 0x39, 0x72, 0xe4, 0x4f, 0x9e, 0xbb, 0xf1, 0x65, 0xca, 0x13, 0x26,
		0x4c, 0x98, 0xb7, 0xe9, 0x55, 0xaa, 0xd3, 0x21, 0x42, 0x84, 0x8f, 0x99, 0xb5, 0xed, 0x5d, 0xba,
		0xf3, 0x61, 0xc2, 0x03, 0x06, 0x0c, 0x18, 0x30, 0x60, 0xc0, 0x07, 0x0e, 0x1c, 0x38, 0x70, 0xe0,
		0x47, 0x8e, 0x9b, 0xb1, 0xe5, 0x4d, 0x9a, 0xb3, 0xe1, 0x45, 0x8a, 0x93, 0xa1, 0xc5, 0x0d, 0x1a,
		0x34, 0x68, 0xd0, 0x27, 0x4e, 0x9c, 0xbf, 0xf9, 0x75, 0xea, 0x53, 0xa6, 0xcb, 0x11, 0x22, 0x44,
		0x88, 0x97, 0xa9, 0xd5, 0x2d, 0x5a, 0xb4, 0xef, 0x59, 0xb2, 0xe3, 0x41, 0x82, 0x83, 0x81, 0x85,
		0x8d, 0x9d, 0xbd, 0xfd, 0x7d, 0xfa, 0x73, 0xe6, 0x4b, 0x96, 0xab, 0xd1, 0x25, 0x4a, 0x94, 0xaf,
		0xd9, 0x35, 0x6a, 0xd4, 0x2f, 0x5e, 0xbc, 0xff, 0x79, 0xf2, 0x63, 0xc6, 0x0b, 0x16, 0x2c, 0x58,
		0xb0, 0xe7, 0x49, 0x92, 0xa3, 0xc1, 0x05, 0x0a, 0x14, 0x28, 0x50, 0xa0, 0xc7, 0x09, 0x12, 0x24,
		0x48, 0x90, 0xa7, 0xc9, 0x15, 0x2a, 0x54, 0xa8, 0xd7, 0x29, 0x52, 0xa4, 0xcf, 0x19, 0x32, 0x64,
		0xc8, 0x17, 0x2e, 0x5c, 0xb8, 0xf7, 0x69, 0xd2, 0x23, 0x46, 0x8c, 0x9f, 0xb9, 0xf5, 0x6d, 0xda,
		0x33, 0x66, 0xcc, 0x1f, 0x3e, 0x7c, 0xf8, 0x77, 0xee, 0x5b, 0xb6, 0xeb, 0x51, 0xa2, 0xc3, 0x00
	} ;

	private static final int INDEX_OF[]={ // 256
		0xff, 0x00, 0x01, 0x63, 0x02, 0xc6, 0x64, 0x6a, 0x03, 0xcd, 0xc7, 0xbc, 0x65, 0x7e, 0x6b, 0x2a,
		0x04, 0x8d, 0xce, 0x4e, 0xc8, 0xd4, 0xbd, 0xe1, 0x66, 0xdd, 0x7f, 0x31, 0x6c, 0x20, 0x2b, 0xf3,
		0x05, 0x57, 0x8e, 0xe8, 0xcf, 0xac, 0x4f, 0x83, 0xc9, 0xd9, 0xd5, 0x41, 0xbe, 0x94, 0xe2, 0xb4,
		0x67, 0x27, 0xde, 0xf0, 0x80, 0xb1, 0x32, 0x35, 0x6d, 0x45, 0x21, 0x12, 0x2c, 0x0d, 0xf4, 0x38,
		0x06, 0x9b, 0x58, 0x1a, 0x8f, 0x79, 0xe9, 0x70, 0xd0, 0xc2, 0xad, 0xa8, 0x50, 0x75, 0x84, 0x48,
		0xca, 0xfc, 0xda, 0x8a, 0xd6, 0x54, 0x42, 0x24, 0xbf, 0x98, 0x95, 0xf9, 0xe3, 0x5e, 0xb5, 0x15,
		0x68, 0x61, 0x28, 0xba, 0xdf, 0x4c, 0xf1, 0x2f, 0x81, 0xe6, 0xb2, 0x3f, 0x33, 0xee, 0x36, 0x10,
		0x6e, 0x18, 0x46, 0xa6, 0x22, 0x88, 0x13, 0xf7, 0x2d, 0xb8, 0x0e, 0x3d, 0xf5, 0xa4, 0x39, 0x3b,
		0x07, 0x9e, 0x9c, 0x9d, 0x59, 0x9f, 0x1b, 0x08, 0x90, 0x09, 0x7a, 0x1c, 0xea, 0xa0, 0x71, 0x5a,
		0xd1, 0x1d, 0xc3, 0x7b, 0xae, 0x0a, 0xa9, 0x91, 0x51, 0x5b, 0x76, 0x72, 0x85, 0xa1, 0x49, 0xeb,
		0xcb, 0x7c, 0xfd, 0xc4, 0xdb, 0x1e, 0x8b, 0xd2, 0xd7, 0x92, 0x55, 0xaa, 0x43, 0x0b, 0x25, 0xaf,
		0xc0, 0x73, 0x99, 0x77, 0x96, 0x5c, 0xfa, 0x52, 0xe4, 0xec, 0x5f, 0x4a, 0xb6, 0xa2, 0x16, 0x86,
		0x69, 0xc5, 0x62, 0xfe, 0x29, 0x7d, 0xbb, 0xcc, 0xe0, 0xd3, 0x4d, 0x8c, 0xf2, 0x1f, 0x30, 0xdc,
		0x82, 0xab, 0xe7, 0x56, 0xb3, 0x93, 0x40, 0xd8, 0x34, 0xb0, 0xef, 0x26, 0x37, 0x0c, 0x11, 0x44,
		0x6f, 0x78, 0x19, 0x9a, 0x47, 0x74, 0xa7, 0xc1, 0x23, 0x53, 0x89, 0xfb, 0x14, 0x5d, 0xf8, 0x97,
		0x2e, 0x4b, 0xb9, 0x60, 0x0f, 0xed, 0x3e, 0xe5, 0xf6, 0x87, 0xa5, 0x17, 0x3a, 0xa3, 0x3c, 0xb7
	} ;


	public RsCodeWord(byte[] in, int pad) {
		RSPAD = pad;
		for(int i = RSPAD; i < NN; i++){
			rsCodeWord[i] = in[i-RSPAD];
		}
		numberOfCorrections = 0;
	}
	
	public RsCodeWord(int pad) {
		RSPAD = pad;
		nextByte = pad;
		numberOfCorrections = 0;
	}
	
	public String toString() {
		String s = "RS SIZE: " + DATA_BYTES + "/" + NN + " PAD:" + RSPAD + "\n";
		for(int i = 0; i < NN; i++){
			s = s + i+ ": " + rsCodeWord[i] + " " ;
			if (i % 16 == 15) s = s + "\n";
		}
		return s;
	}

	public void addByte(byte b) throws ArrayIndexOutOfBoundsException {
		rsCodeWord[nextByte++] = b;
	}

	public byte getByte(int i) throws IndexOutOfBoundsException{
		if (i+RSPAD >= NN) {
			throw new IndexOutOfBoundsException("ERROR: Outside of data range for code word.  Is frame length wrong?");
		}
		return rsCodeWord[i+RSPAD];
	}
	
	public boolean validDecode() {
		if (numberOfCorrections == -1) return false;
		
		// Fail if too many errors - this should never happen because the decoder does this anyway
//		if (numberOfCorrections > NROOTS/2) return false;
		
		// Fail if too many erasures = this should never happen unless something goes very wrong
//		if (numberOfErasures > NROOTS) return false;
		
		// Fail if we have too many errors and erasures combined - should also never happen, but it has
//		int errors = numberOfCorrections - numberOfErasures;
//		if (2*errors + numberOfErasures >= NROOTS) return false;
		
		return true;
	}
	
	public int getNumberOfCorrections() { return numberOfCorrections; }
	
	/**
	 * Decode the rsCodeWord and return a frame of bytes, which have been corrected
	 * 
	 * @return
	 */
	public byte[] decode() {
		numberOfCorrections = decode_rs_8(rsCodeWord,erasurePositions,numberOfErasures);
		if (Config.debugFrames || Config.debugRS) Log.println("RS ERASURES: " + numberOfErasures + " ERRORS CORRECTED:" + numberOfCorrections);

		byte[] rawFrame = new byte[NN-RSPAD];
		for(int i = 0; i < rawFrame.length; i++){
			rawFrame[i] = rsCodeWord[i+RSPAD];
		}
		return rawFrame;
	}

	/**
	 * Set the erasure array and number of erasures.  This must be done before decode is called.  The pos array contains the erasures
	 * in the position that we find them in the downlinked frame.  This routine adds the number of padded zeros so that
	 * the positions are in the right place in the virtual frame.
	 * 
	 * @param pos
	 * @param n
	 */
	public void setErasurePositions(int[] pos, int n) {
		erasurePositions = pos;
		for (int i=0; i < numberOfErasures; i++)
			erasurePositions[i] += RSPAD;
		numberOfErasures = n;
	}
	
	/* This decoder has evolved extensively through the work of Phil Karn.  It draws
	 * on his own ideas and optimisations, and on the work of others.  The lineage
	 * is as below, and parts of the authors' notices are included here.  (JRM)

	 * Reed-Solomon decoder
	 * Copyright 2002 Phil Karn, KA9Q
	 * May be used under the terms of the GNU General Public License (GPL)
	 *
	 * Reed-Solomon coding and decoding
	 * Phil Karn (karn@ka9q.ampr.org) September 1996
	 *
	 * This file is derived from the program "new_rs_erasures.c" by Robert
	 * Morelos-Zaragoza (robert@spectra.eng.hawaii.edu) and Hari Thirumoorthy
	 * (harit@spectra.eng.hawaii.edu), Aug 1995
	 * --------------------------------------------------------------------------
	 *
	 * From the RM-Z & HT program:
	 * The encoding and decoding methods are based on the
	 * book "Error Control Coding: Fundamentals and Applications",
	 * by Lin and Costello, Prentice Hall, 1983, ISBN 0-13-283796-X
	 * Portions of this program are from a Reed-Solomon encoder/decoder
	 * in C, written by Simon Rockliff (simon@augean.ua.oz.au) on 21/9/89.
	 * --------------------------------------------------------------------------
	 *
	 * From the 1989/1991 SR program (also based on Lin and Costello):
	 * This program may be freely modified and/or given to whoever wants it.
	 * A condition of such distribution is that the author's contribution be
	 * acknowledged by his name being left in the comments heading the program,
	 *                               Simon Rockliff, 26th June 1991
	 *
	 */
	private int mod255(int x) {
		while (x >= 255) {
			x -= 255;
			x = (x >> 8) + (x & 255);
		}
		return x;
	}

	private int decode_rs_8(byte[] data, int[] eras_pos, int no_eras) {
		int deg_lambda, el, deg_omega;
		int i, j, r,k;
		int u,q,tmp,num1,num2,den,discr_r;
		int[] lambda = new int[NROOTS+1], s = new int[NROOTS];   /* Err+Eras Locator poly and syndrome poly */
		int[] b = new int[NROOTS+1], t = new int[NROOTS+1], omega = new int[NROOTS+1];
		int[] root= new int[NROOTS], reg = new int[NROOTS+1], loc = new int[NROOTS];
		int syn_error, count = -1;

		try {
			/* form the syndromes; i.e., evaluate data(x) at roots of g(x) */
			for(i=0;i<NROOTS;i++)
				s[i] = data[0] & 0xff;

			for(j=1;j<NN;j++){
				for(i=0;i<NROOTS;i++){
					if(s[i] == 0){
						s[i] = data[j] & 0xff;
					} else {
						s[i] = (data[j] & 0xff) ^ ALPHA_TO[mod255(INDEX_OF[s[i]] + (FCR+i)*PRIM)];
					}
				}
			}

			/* Convert syndromes to index form, checking for nonzero condition */
			syn_error = 0;
			for(i=0;i<NROOTS;i++){
				syn_error |= s[i];
				s[i] = INDEX_OF[s[i]];
			}

			if (0==syn_error) {
				/* if syndrome is zero, data[] is a codeword and there are no
				 * errors to correct. So return data[] unmodified
				 */
				count = 0;
				return count;
			}
			//memset(&lambda[1],0,NROOTS*sizeof(lambda[0]));
			lambda[0] = 1;

			if (no_eras > 0) {
				/* Init lambda to be the erasure locator polynomial */
				lambda[1] = ALPHA_TO[mod255(PRIM*(NN-1-eras_pos[0]))];
				for (i = 1; i < no_eras; i++) {
					u = mod255(PRIM*(NN-1-eras_pos[i]));
					for (j = i+1; j > 0; j--) {
						tmp = INDEX_OF[lambda[j - 1]];
						if(tmp != A0)
							lambda[j] ^= ALPHA_TO[mod255(u + tmp)];
					}
				}
			}
			for(i=0;i<NROOTS+1;i++)
				b[i] = INDEX_OF[lambda[i]];

			/*
			 * Begin Berlekamp-Massey algorithm to determine error+erasure
			 * locator polynomial
			 */
			r = no_eras;
			el = no_eras;
			while (++r <= NROOTS) {       /* r is the step number */
				/* Compute discrepancy at the r-th step in poly-form */
				discr_r = 0;
				for (i = 0; i < r; i++){
					if ((lambda[i] != 0) && (s[r-i-1] != A0)) {
						discr_r ^= ALPHA_TO[mod255(INDEX_OF[lambda[i]] + s[r-i-1])];
					}
				}
				discr_r = INDEX_OF[discr_r];        /* Index form */
				if (discr_r == A0) {
					/* 2 lines below: B(x) <-- x*B(x) */
					//memmove(&b[1],b,NROOTS*sizeof(b[0]));
					System.arraycopy(b,0,b,1,NROOTS);
					b[0] = A0;
				} else {
					/* 7 lines below: T(x) <-- lambda(x) - discr_r*x*b(x) */
					t[0] = lambda[0];
					for (i = 0 ; i < NROOTS; i++) {
						if(b[i] != A0)
							t[i+1] = lambda[i+1] ^ ALPHA_TO[mod255(discr_r + b[i])];
						else
							t[i+1] = lambda[i+1];
					}
					if (2 * el <= r + no_eras - 1) {
						el = r + no_eras - el;
						/*
						 * 2 lines below: B(x) <-- inv(discr_r) *
						 * lambda(x)
						 */
						for (i = 0; i <= NROOTS; i++)
							b[i] = (lambda[i] == 0) ? A0 : mod255(INDEX_OF[lambda[i]] - discr_r + NN);
					} else {
						/* 2 lines below: B(x) <-- x*B(x) */
						//memmove(&b[1],b,NROOTS*sizeof(b[0]));
						System.arraycopy(b,0,b,1,NROOTS);
						b[0] = A0;
					}
					//memcpy(lambda,t,(NROOTS+1)*sizeof(t[0]));
					System.arraycopy(t,0,lambda,0,NROOTS+1);
				}
			}

			/* Convert lambda to index form and compute deg(lambda(x)) */
			deg_lambda = 0;
			for(i=0;i<NROOTS+1;i++){
				lambda[i] = INDEX_OF[lambda[i]];
				if(lambda[i] != A0)
					deg_lambda = i;
			}
			/* Find roots of the error+erasure locator polynomial by Chien search */
			//memcpy(&reg[1],&lambda[1],NROOTS*sizeof(reg[0]));
			System.arraycopy(lambda,1,reg,1,NROOTS);
			count = 0;            /* Number of roots of lambda(x) */
			for (i = 1,k=IPRIM-1; i <= NN; i++,k = mod255(k+IPRIM)) {
				q = 1; /* lambda[0] is always 0 */
				for (j = deg_lambda; j > 0; j--){
					if (reg[j] != A0) {
						reg[j] = mod255(reg[j] + j);
						q ^= ALPHA_TO[reg[j]];
					}
				}
				if (q != 0)
					continue; /* Not a root */
				/* store root (index-form) and error location number */
				root[count] = i;
				loc[count] = k;
				/* If we've already found max possible roots,
				 * abort the search to save time
				 */
				if(++count == deg_lambda)
					break;
			}
			if (deg_lambda != count) {
				/*
				 * deg(lambda) unequal to number of roots => uncorrectable
				 * error detected
				 */
				count = -1;
				return count;
			}
			/*
			 * Compute err+eras evaluator poly omega(x) = s(x)*lambda(x) (modulo
			 * x**NROOTS). in index form. Also find deg(omega).
			 */
			deg_omega = 0;
			for (i = 0; i < NROOTS;i++){
				tmp = 0;
				j = (deg_lambda < i) ? deg_lambda : i;
				for(;j >= 0; j--){
					if ((s[i - j] != A0) && (lambda[j] != A0))
						tmp ^= ALPHA_TO[mod255(s[i - j] + lambda[j])];
				}
				if(tmp != 0)
					deg_omega = i;
				omega[i] = INDEX_OF[tmp];
			}
			omega[NROOTS] = A0;

			/*
			 * Compute error values in poly-form. num1 = omega(inv(X(l))), num2 =
			 * inv(X(l))**(FCR-1) and den = lambda_pr(inv(X(l))) all in poly-form
			 */
			for (j = count-1; j >=0; j--) {
				num1 = 0;
				for (i = deg_omega; i >= 0; i--) {
					if (omega[i] != A0)
						num1  ^= ALPHA_TO[mod255(omega[i] + i * root[j])];
				}
				num2 = ALPHA_TO[mod255(root[j] * (FCR - 1) + NN)];
				den = 0;

				/* lambda[i+1] for i even is the formal derivative lambda_pr of lambda[i] */
				for (i = min(deg_lambda,NROOTS-1) & ~1; i >= 0; i -=2) {
					if(lambda[i+1] != A0)
						den ^= ALPHA_TO[mod255(lambda[i+1] + i * root[j])];
				}
				if (den == 0) {
					count = -1;
					return count;
				}
				/* Apply error to data */
				if (num1 != 0) {
					data[loc[j]] ^= ALPHA_TO[mod255(INDEX_OF[num1] + INDEX_OF[num2] + NN - INDEX_OF[den])];
				}
			}
			return count;
		} finally {
			if(eras_pos != null){
				for(i=0;i<count;i++)
					eras_pos[i] = loc[i];
			}
		}
	}

	private int min(int a, int b) {
		return (a<b) ? a : b;
	}

}
