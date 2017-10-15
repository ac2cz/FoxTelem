package FuncubeDecoder;
//Java port of Phil Karn's and J R Miller's AO-40 FEC decoder
//Author: Phil Ashby, May 2013

//This source is released under the terms of the Creative Commons
//Non-Commercial Share Alike license:
//http://creativecommons.org/licenses/by-nc-sa/3.0/

//Author: Phil Ashby, based on previous work by Howard Long (G6LVB)
//Duncan Hills, Phil Karn (KA9Q) and J R Miller

public class FECDecoder {
	// Reed-Solomon decoder constants
	private static final int NN = 255;
	private static final int KK = 223;
	private static final int NROOTS = 32;
	private static final int FCR = 112;
	private static final int PRIM = 11;
	private static final int IPRIM = 116;
	private static final int A0 = NN;
	private static final int BLOCKSIZE = 256;
	private static final int RSBLOCKS = 2;
	private static final int RSPAD = 95;
	// Viterbi decoder constants
	private static final int K = 7;
	private static final int N = 2;
	private static final int CPOLYA = 0x4f;
	private static final int CPOLYB = 0x6d;
	private static final int NBITS = ((BLOCKSIZE+NROOTS*RSBLOCKS)*8+K-1);
	// Interleaver constants
	private static final int ROWS = 80;
	private static final int COLUMNS = 65;
	private static final int SYMPBLOCK = (ROWS*COLUMNS);
	// Sync vector polynomial
	private static final int SYNC_POLY = 0x48;

	/* ----------------------------------------------------------------------------------- */
	// 8-bit parity table
	private static final byte Partab[] = { // 256
		 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
		 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
		 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
		 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
		 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
		 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
		 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
		 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
		 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
		 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
		 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
		 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
		 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
		 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
		 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
		 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0
	};

	/* Tables for Viterbi r=1/2 k=7 decoder to CCSDS standard */
	/* ------------------------------------------------------ */
	
	/* Metric table, [sent sym][rx symbol] */
	/* This metric table is for an 8-bit ADC, which is total overkill!
	  Simplify later.  128-i and i-128 would probably do!  jrm */


	private static final int mettab[][]={{ // 2x256
		 20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,
		 20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,
		 20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,
		 20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,
		 20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,
		 20,   20,   20,   20,   20,   20,   20,   19,   19,   19,   19,   19,   19,   19,   19,   19,
		 19,   19,   18,   18,   18,   18,   18,   18,   17,   17,   17,   16,   16,   16,   15,   15,
		 14,   14,   13,   13,   12,   11,   10,   10,    9,    8,    7,    6,    5,    3,    2,    1,
		 -1,   -2,   -4,   -5,   -7,   -9,  -11,  -13,  -15,  -17,  -19,  -21,  -23,  -25,  -28,  -30,
		-32,  -35,  -37,  -40,  -42,  -45,  -47,  -50,  -52,  -55,  -58,  -60,  -63,  -66,  -68,  -71,
		-74,  -77,  -79,  -82,  -85,  -88,  -90,  -93,  -96,  -99, -102, -104, -107, -110, -113, -116,
		-119, -121, -124, -127, -130, -133, -136, -138, -141, -144, -147, -150, -153, -155, -158, -161,
		-164, -167, -170, -172, -175, -178, -181, -184, -187, -190, -192, -195, -198, -201, -204, -207,
		-210, -212, -215, -218, -221, -224, -227, -229, -232, -235, -238, -241, -244, -247, -249, -252,
		-255, -258, -261, -264, -267, -269, -272, -275, -278, -281, -284, -286, -289, -292, -295, -298,
		-301, -304, -306, -309, -312, -315, -318, -320, -324, -326, -329, -332, -335, -337, -341, -372 },
	{	-372, -341, -338, -335, -332, -329, -326, -324, -321, -318, -315, -312, -309, -306, -304, -301,
		-298, -295, -292, -289, -286, -284, -281, -278, -275, -272, -269, -267, -264, -261, -258, -255,
		-252, -249, -247, -244, -241, -238, -235, -232, -229, -227, -224, -221, -218, -215, -212, -210,
		-207, -204, -201, -198, -195, -192, -190, -187, -184, -181, -178, -175, -172, -170, -167, -164,
		-161, -158, -155, -153, -150, -147, -144, -141, -138, -136, -133, -130, -127, -124, -121, -119,
		-116, -113, -110, -107, -104, -102,  -99,  -96,  -93,  -90,  -88,  -85,  -82,  -79,  -77,  -74,
		-71,  -68,  -66,  -63,  -60,  -58,  -55,  -52,  -50,  -47,  -45,  -42,  -40,  -37,  -35,  -32,
		-30,  -28,  -25,  -23,  -21,  -19,  -17,  -15,  -13,  -11,   -9,   -7,   -5,   -4,   -2,   -1,
		  1,    2,    3,    5,    6,    7,    8,    9,   10,   10,   11,   12,   13,   13,   14,   14,
		 15,   15,   16,   16,   16,   17,   17,   17,   18,   18,   18,   18,   18,   18,   19,   19,
		 19,   19,   19,   19,   19,   19,   19,   19,   19,   20,   20,   20,   20,   20,   20,   20,
		 20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,
		 20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,
		 20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,
		 20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,
		 20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,   20
	}};


	/* Table of symbol pair emitted for each state of the convolutional
	encoder's shift register */ 
	private static final int Syms[]={ // 128
		 1, 2, 3, 0, 2, 1, 0, 3, 2, 1, 0, 3, 1, 2, 3, 0,
		 1, 2, 3, 0, 2, 1, 0, 3, 2, 1, 0, 3, 1, 2, 3, 0,
		 0, 3, 2, 1, 3, 0, 1, 2, 3, 0, 1, 2, 0, 3, 2, 1,
		 0, 3, 2, 1, 3, 0, 1, 2, 3, 0, 1, 2, 0, 3, 2, 1,
		 2, 1, 0, 3, 1, 2, 3, 0, 1, 2, 3, 0, 2, 1, 0, 3,
		 2, 1, 0, 3, 1, 2, 3, 0, 1, 2, 3, 0, 2, 1, 0, 3,
		 3, 0, 1, 2, 0, 3, 2, 1, 0, 3, 2, 1, 3, 0, 1, 2,
		 3, 0, 1, 2, 0, 3, 2, 1, 0, 3, 2, 1, 3, 0, 1, 2 
	};


	/* Scramble byte table */
	private static final int Scrambler[]={ // 320
	  0xff, 0x48, 0x0e, 0xc0, 0x9a, 0x0d, 0x70, 0xbc, 0x8e, 0x2c, 0x93, 0xad, 0xa7, 0xb7, 0x46, 0xce,
	  0x5a, 0x97, 0x7d, 0xcc, 0x32, 0xa2, 0xbf, 0x3e, 0x0a, 0x10, 0xf1, 0x88, 0x94, 0xcd, 0xea, 0xb1,
	  0xfe, 0x90, 0x1d, 0x81, 0x34, 0x1a, 0xe1, 0x79, 0x1c, 0x59, 0x27, 0x5b, 0x4f, 0x6e, 0x8d, 0x9c,
	  0xb5, 0x2e, 0xfb, 0x98, 0x65, 0x45, 0x7e, 0x7c, 0x14, 0x21, 0xe3, 0x11, 0x29, 0x9b, 0xd5, 0x63,
	  0xfd, 0x20, 0x3b, 0x02, 0x68, 0x35, 0xc2, 0xf2, 0x38, 0xb2, 0x4e, 0xb6, 0x9e, 0xdd, 0x1b, 0x39,
	  0x6a, 0x5d, 0xf7, 0x30, 0xca, 0x8a, 0xfc, 0xf8, 0x28, 0x43, 0xc6, 0x22, 0x53, 0x37, 0xaa, 0xc7,
	  0xfa, 0x40, 0x76, 0x04, 0xd0, 0x6b, 0x85, 0xe4, 0x71, 0x64, 0x9d, 0x6d, 0x3d, 0xba, 0x36, 0x72,
	  0xd4, 0xbb, 0xee, 0x61, 0x95, 0x15, 0xf9, 0xf0, 0x50, 0x87, 0x8c, 0x44, 0xa6, 0x6f, 0x55, 0x8f,
	  0xf4, 0x80, 0xec, 0x09, 0xa0, 0xd7, 0x0b, 0xc8, 0xe2, 0xc9, 0x3a, 0xda, 0x7b, 0x74, 0x6c, 0xe5,
	  0xa9, 0x77, 0xdc, 0xc3, 0x2a, 0x2b, 0xf3, 0xe0, 0xa1, 0x0f, 0x18, 0x89, 0x4c, 0xde, 0xab, 0x1f,
	  0xe9, 0x01, 0xd8, 0x13, 0x41, 0xae, 0x17, 0x91, 0xc5, 0x92, 0x75, 0xb4, 0xf6, 0xe8, 0xd9, 0xcb,
	  0x52, 0xef, 0xb9, 0x86, 0x54, 0x57, 0xe7, 0xc1, 0x42, 0x1e, 0x31, 0x12, 0x99, 0xbd, 0x56, 0x3f,
	  0xd2, 0x03, 0xb0, 0x26, 0x83, 0x5c, 0x2f, 0x23, 0x8b, 0x24, 0xeb, 0x69, 0xed, 0xd1, 0xb3, 0x96,
	  0xa5, 0xdf, 0x73, 0x0c, 0xa8, 0xaf, 0xcf, 0x82, 0x84, 0x3c, 0x62, 0x25, 0x33, 0x7a, 0xac, 0x7f,
	  0xa4, 0x07, 0x60, 0x4d, 0x06, 0xb8, 0x5e, 0x47, 0x16, 0x49, 0xd6, 0xd3, 0xdb, 0xa3, 0x67, 0x2d,
	  0x4b, 0xbe, 0xe6, 0x19, 0x51, 0x5f, 0x9f, 0x05, 0x08, 0x78, 0xc4, 0x4a, 0x66, 0xf5, 0x58, 0xff,
	  0x48, 0x0e, 0xc0, 0x9a, 0x0d, 0x70, 0xbc, 0x8e, 0x2c, 0x93, 0xad, 0xa7, 0xb7, 0x46, 0xce, 0x5a,
	  0x97, 0x7d, 0xcc, 0x32, 0xa2, 0xbf, 0x3e, 0x0a, 0x10, 0xf1, 0x88, 0x94, 0xcd, 0xea, 0xb1, 0xfe,
	  0x90, 0x1d, 0x81, 0x34, 0x1a, 0xe1, 0x79, 0x1c, 0x59, 0x27, 0x5b, 0x4f, 0x6e, 0x8d, 0x9c, 0xb5,
	  0x2e, 0xfb, 0x98, 0x65, 0x45, 0x7e, 0x7c, 0x14, 0x21, 0xe3, 0x11, 0x29, 0x9b, 0xd5, 0x63, 0xfd
	} ;



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

	/* ----------------------------------------------------------------------------------- */

	/* --------------- */
	/* Viterbi decoder */
	/* --------------- */

	/* Viterbi decoder for arbitrary convolutional code
	 * viterbi27 and viterbi37 for the r=1/2 and r=1/3 K=7 codes are faster
	 * Copyright 1997 Phil Karn, KA9Q
	 */

	/* This is a bare bones <2,7> Viterbi decoder, adapted from a general purpose model.
	 * It is not optimised in any way, neither as to coding (for example the memcopy should
	 * be achievable simply by swapping pointers), nor as to simplifying the metric table,
	 * nor as to using any machine-specific smarts.  On contemporary machines, in this application,
	 * the execution time is negligible.  Many ideas for optimisation are contained in PK's www pages.
	 * The real ADC is 8-bit, though in practice 4-bits are actually sufficient.
	 * Descriptions of the Viterbi decoder algorithm can be found in virtually any book
	 * entitled "Digital Communications".  (JRM)
	 */
	private int viterbi27(byte[] data, byte[] symbols, int nbits) {
     int bitcnt = 0;
     int beststate,i,j,k = 0, l = 0;
     long[] cmetric = new long[64];
     long[] nmetric = new long[64];    /* 2^(K-1) */
     long[] pp = new long[nbits*2];
     long m0,m1,mask;
     int[] mets = new int[4];                     /* 2^N */

     //pp = paths = (unsigned long *)calloc(nbits*2,sizeof(unsigned long));
     /* Initialize starting metrics to prefer 0 state */
     cmetric[0] = 0;
     for(i=1;i< 64;i++)
             cmetric[i] = -999999;

     for(;;){
             /* Read 2 input symbols and compute the 4 branch metrics */
             for(i=0;i<4;i++){
                     mets[i] = 0;
                     for(j=0;j<2;j++){
                             mets[i] += mettab[(i >> (1-j)) & 1][(int)symbols[j+k] & 0xff];
                     }
             }
             //symbols += 2;
             k += 2;
             mask = 1;
             for(i=0;i<64;i+=2){
                     int b1,b2;

                     b1 = mets[Syms[i]];
                     nmetric[i] = m0 = cmetric[i/2] + b1;
                     b2 = mets[Syms[i+1]];
                     b1 -= b2;
                     m1 = cmetric[(i/2) + (1<<(K-2))] + b2;
                     if(m1 > m0){
                             nmetric[i] = m1;
                             pp[l] |= mask;
                     }
                     m0 -= b1;
                     nmetric[i+1] = m0;
                     m1 += b1;
                     if(m1 > m0){
                             nmetric[i+1] = m1;
                             pp[l] |= mask << 1;
                     }
                     mask <<= 2;
                     if((mask & 0xffffffffL) == 0){
                             mask = 1;
                             l++;
                     }
             }
             if(mask != 1)
                     l++;
             if(++bitcnt == nbits){
                     beststate = 0;

                     break;
             }
             //memcpy(cmetric,nmetric,sizeof(cmetric));
             System.arraycopy(nmetric, 0, cmetric, 0, nmetric.length);
     }
     l -= 2;
     /* Chain back from terminal state to produce decoded data */
     //memset(data,0,nbits/8);
     for(i=0; i<nbits/8; i++)
     	data[i]=0;
     for(i=nbits-K;i >= 0;i--){
             if((pp[l+(beststate >> 5)] & (1L << (beststate & 31)))!=0){
                     beststate |= (1 << (K-1));
                     data[i>>3] |= 0x80 >> (i&7);
             }
             beststate >>= 1;
             l -= 2;
     }
     return 0;
	}
	
	/* ----------------------------------------------------------------------------------- */
	
	/* ---------- */
	/* RS Decoder */
	/* ---------- */

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
		    s[i] = data[0];
	
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

	/* ----------------------------------------------------------------------------------- */

/* ---------- */
/* Re-encoder */
/* ---------- */

/* Reference encoder for proposed coded AO-40 telemetry format - v1.0  7 Jan 2002
* Copyright 2002, Phil Karn, KA9Q
* This software may be used under the terms of the GNU Public License (GPL)
*/

/* Adapted from  the above enc_ref.c  as used by the spacecraft (JRM) */

	private int Nbytes;                        /* Byte counter for encode_data() */
	private int Bindex;                        /* Byte counter for interleaver */
	private int Conv_sr;             /* Convolutional encoder shift register state */
	private int[][] RS_block = new int[RSBLOCKS][NROOTS]; /* RS parity blocks */
	private byte[] reencode = new byte[SYMPBLOCK] ;       /* Re-encoded symbols */

	private static final int RS_poly[] = {
	249, 59, 66,  4, 43,126,251, 97, 30,  3,213, 50, 66,170,  5, 24
	};

/* Write one binary channel symbol into the interleaver frame and update the pointers */
	private void interleave_symbol(int c){
	  int row,col;
	  col=Bindex/COLUMNS;
	  row=Bindex%COLUMNS;
	  if(c!=0)
	    reencode[row*ROWS+col] = 1;
	  Bindex++;
}

/* Convolutionally encode and interleave one byte */
	private void encode_and_interleave(int c, int cnt){
	  while(cnt-- != 0){
	    Conv_sr = (Conv_sr << 1) | (c >> 7);
	    c <<= 1;
	    interleave_symbol( Partab[Conv_sr & CPOLYA]);
	    interleave_symbol(1-Partab[Conv_sr & CPOLYB]); /* Second encoder symbol is inverted */
	  }
	}

/* Scramble a byte, convolutionally encode and interleave into frame */
	private void scramble_and_encode(int c){
	  c ^= Scrambler[Nbytes];      /* Scramble byte */
	  encode_and_interleave(c,8);  /* RS encode and place into reencode buffer */
	}


/* Three user's entry points now follow.  They are:

init_encoder()                   Called once before using system.
encode_byte(unsigned char c)     Called with each user byte (i.e. 256 calls)
encode_parity()                  Called 64 times to finish off

*/

/* This function initializes the encoder. */
	private void local_init_encoder(){
	  int i,j,sr;
	
	  Nbytes  = 0;
	  Conv_sr = 0;
	  Bindex  = COLUMNS;            /* Sync vector is in first column; data starts here */
	
	  for(j=0;j<RSBLOCKS;j++)       /* Flush parity array*/
	    for(i=0;i<NROOTS;i++)
	      RS_block[j][i] = 0;
	
	  /* Clear re-encoded array */
	  for(i=0;i<5200;i++)
	    reencode[i] = 0;
	
	  /* Generate sync vector, interleave into re-encode array, 1st column */
	  sr = 0x7f;
	  for(i=0;i<65;i++){
	    if((sr & 64)!=0)
	      reencode[ROWS*i] = 1;      /* Every 80th symbol is a sync bit */
	    sr = (sr << 1) | Partab[sr & SYNC_POLY];
	  }
	}


/* This function is called with each user data byte to be encoded into the
* current frame. It should be called in sequence 256 times per frame, followed
* by 64 calls to encode_parity().
*/

	private void local_encode_byte(int c){
	  int rsi;                /* RS block pointer */
	  int i;
	  int feedback;
	
	  /* Update the appropriate Reed-Solomon codeword */
	  rsi = Nbytes & 1;
	
	  /* Compute feedback term */
	  feedback = INDEX_OF[c ^ RS_block[rsi][0]];
	
	  /* If feedback is non-zero, multiply by each generator polynomial coefficient and
	   * add to corresponding shift register elements
	   */
	  if(feedback != A0){
	    int j;
	
	    /* This loop exploits the palindromic nature of the generator polynomial
	     * to halve the number of discrete multiplications
	     */
	    for(j=0;j<15;j++){
	      int t;
	
	      t = ALPHA_TO[mod255(feedback + RS_poly[j])];
	      RS_block[rsi][j+1] ^= t; RS_block[rsi][31-j] ^= t;
	    }
	    RS_block[rsi][16] ^= ALPHA_TO[mod255(feedback + RS_poly[15])];
	  }
	
	  /* Shift 32 byte RS register one position down */
	  for(i=0;i<31;i++)
	    RS_block[rsi][i] = RS_block[rsi][i+1];
	
	  /* Handle highest order coefficient, which is unity */
	  if(feedback != A0){
	    RS_block[rsi][31] = ALPHA_TO[feedback];
	  } else {
	    RS_block[rsi][31] = 0;
	  }
	  scramble_and_encode(c);
	  Nbytes++;
	}

/* This function should be called 64 times after the 256 data bytes
* have been passed to update_encoder. Each call scrambles, encodes and
* interleaves one byte of Reed-Solomon parity.
*/

	private void local_encode_parity() {
	  int c;
	
	  c =  RS_block[Nbytes & 1][(Nbytes-256)>>1];
	  scramble_and_encode(c);
	  if(++Nbytes == 320){
	    /* Tail off the convolutional encoder (flush) */
	    encode_and_interleave(0,6);
	  }
	}

/* Encodes the 256 byte source block RSdecdata[] into 5200 byte block of symbols
* reencode[].  It has the same format as an off-air received block of symbols.
*/

	private void encode_FEC40(
	   byte[] RSdecdata )           /* User's source data */
	{
	   int i;
	   local_init_encoder();
	   for(i=0;i<256;i++){
	     local_encode_byte((int)RSdecdata[i]&0xff) ;
	   }
	   for(i=0;i<64;i++){
	     local_encode_parity() ;
	   }
	}

/* ----------------------------------------------------------------------------------- */

	/****************************************************************************************************/
	/*                                                                                                  */
	/*   AMSAT AO-40 FEC Reference Decoder (by JRM)               Input                Output           */
	/*   ------------------------------------------               -----------------------------------   */
	/*   Step 1: De-interleave                                    raw[5200]       ->  symbols[5132]     */
	/*   Step 2: Viterbi decoder                                  symbols[5132]   ->  vitdecdata[320]   */
	/*   Step 3: RS decoder                                       vitdecdata[320] ->  RSdecdata[256]    */
	/*   Step 4: Option: Re-encode o/p and count channel errors   RSdecdata[256]  ->  reencode[5200]    */
	/*                                                                                                  */
	/****************************************************************************************************/

	public int FECDecode(byte[] raw, byte[] RSdecdata) {
		byte[] symbols = new byte[NBITS*2+65+3];   // de-interleaved sync+symbols
		byte[] vitdecdata = new byte[(NBITS-6)/8]; // array for Viterbi decoder output
		int nRC = 0;
		/* Step 1: De-interleaver */
	      {
	        /* Input  array:  raw   */
	        /* Output array:  symbols */
	        int col,row;
	        int coltop,rowstart;

			coltop=0;
	        for(col=1;col<ROWS;col++){          /* Skip first column as it's the sync vector */
	          rowstart=0;
	          for(row=0;row<COLUMNS;row++){
	            symbols[coltop+row]=raw[rowstart+col];  /* coltop=col*65 ; rowstart=row*80 */
	            rowstart+=ROWS;
	          }
	          coltop+=COLUMNS;
	        }
	      } /* end of de-interleaving */


	/* Step 2: Viterbi decoder */
	/* ----------------------- */
	      {
	        /* Input  array:  symbols  */
	        /* Output array:  vitdecdata */
	        viterbi27(vitdecdata,symbols,NBITS);
	      }

	/* Steps 3: RS decoder */
	/* ------------------- */

	/* There are two RS decoders, processing 128 bytes each.
	 *
	 * If both RS decoders are SUCCESSFUL
	 * On exit:
	 *   rs_failures = 0
	 *   rserrs[x]   = number of errors corrected;  range 0 to 16   (x= 0 or 1)
	 *   Data output is in array RSdecdata[256].
	 *
	 * If an RS decoder FAILS
	 * On exit:
	 *   rs_failures = 1 or 2   (i.e. != 0)
	 *   rserrs[x] contains -1
	 *   Data output should not be used.
	 */

	      {
	        /* Input  array:  vitdecdata */
	        /* Output array:  RSdecdata  */

	        byte[][] rsblocks = new byte[RSBLOCKS][NN];          /*  [2][255] */
	        int row,col,di,si;
	        int rs_failures ;                     /* Flag set if errors found */
	        int[] rserrs = new int[RSBLOCKS];                 /* Count of errors in each RS codeword */

	        //FILE *fp ;                            /* Needed if you save o/p to file */

	        /* Interleave into Reed Solomon codeblocks */
	        //memset(rsblocks,0,sizeof(rsblocks));                          /* Zero rsblocks array */
	        di = 0;
	        si = 0;
	        for(col=RSPAD;col<NN;col++){
	          for(row=0;row<RSBLOCKS;row++){
	            rsblocks[row][col] = (byte) (vitdecdata[di++] ^ Scrambler[si++]);  /* Remove scrambling */
	          }
	        }
	        /* Run RS-decoder(s) */
	        rs_failures = 0;
	        for(row=0;row<RSBLOCKS;row++){
	          rserrs[row] = decode_rs_8(rsblocks[row],null,0);
	          rs_failures += (rserrs[row] == -1) ? 1 : 0;
	        }

	        /* If frame decoded OK, deinterleave data from RS codeword(s), and save file */
	        if(0==rs_failures){
	          int j = 0;

	          for(col=RSPAD;col<KK;col++){
	            for(row=0;row<RSBLOCKS;row++){
	              RSdecdata[j++] = rsblocks[row][col];
	            }
	          }
	          /* and save out succesfully RS-decoded data */
	        }

	        /* Print RS-decode status summary 
	        {
						char sz[80];

	          if (Verbose)
						{
							sprintf(sz,"RS byte corrections: ");
							//_AppendText(sz);
						}
	          for(row=0;row<RSBLOCKS;row++)
						{
	            if(rserrs[row] != -1)
							{
								if (Verbose)
								{
									sprintf(sz," %d  ",rserrs[row]);
									//_AppendText(sz);
								}
							}	
	            else
							{
								if (Verbose)
								{
									sprintf(sz,"FAIL ");
									//_AppendText(sz);
								}
								nRC=0;
							}
	          }
	        } */
	        for(row=0;row<RSBLOCKS;row++) {
	        	if(rserrs[row] == -1)
	        		nRC = -1;	// FAIL condition
	        }
	      } /* end of rs section */


	/* Step 4: Optional: Re-encode o/p and count channel errors  */
	/* --------------------------------------------------------  */

		if (nRC>=0)
	     {  int errors = 0;
	        int i;
	        // Input  array:  RSdecdata
	        // Output array:  reencode

	        encode_FEC40(RSdecdata) ;  // Re-encode in AO-40 FEC format

	        // Count the channel errors
	        errors = 0;
	        for(i=0;i<SYMPBLOCK;i++)
	          if ( (reencode[i]&0xff) != ((raw[i]&0xff)>>7) ) {
	            errors++ ;
	          }
	          
	        nRC = errors;
	     }  //end of reencode section

	/* --------------------------------------------------------*/

     return nRC;
	}
}
