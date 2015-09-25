package test;

import java.io.IOException;

import org.junit.Test;

import common.Log;
import decoder.CircularByteBuffer;

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
 */
public class CircularByteBufferTest {

	
	@Test
	public void test2() {
		try {
			Log.init("test.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CircularByteBuffer a = new CircularByteBuffer(50);
		for (int i=0; i< 20; i++) {
			a.add((byte)i);
		}
				
		for (int i=0; i< 20; i++) {
			a.add((byte)i);
		}
		System.out.println("The first BYTES were:");
		for (int i=0; i< 20; i++) {
			System.out.println(a.get(0));
			a.incStartPointer(1);
		}

		
		for (int i=0; i< 20; i++) {
			a.add((byte)i);
		}

		System.out.println("The Second BYTES were:");
		for (int i=0; i< 20; i++) {
			System.out.println(a.get(0));
			a.incStartPointer(1);
		}
		
		System.out.println("The Third BYTES were:");
		for (int i=0; i< 19; i++) {
			System.out.println(a.get(0));
			a.incStartPointer(1);
		}
		System.out.println("Remaining at 0: " + a.get(0));

	}
	@Test
	public void testSize() {
		try {
			Log.init("test.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CircularByteBuffer a = new CircularByteBuffer(10);
		a.add((byte)10);
		a.add((byte)11);
		a.add((byte)12);
		a.add((byte)13);
		a.add((byte)14);  // position after inc by 5
		a.add((byte)15);
		a.add((byte)16);
		a.add((byte)17);
		a.add((byte)18);  
		
		// position of start pointer for first read is after the data in position 9

		System.out.println("Read 5 bits");
		for (int i=0; i<5; i++) {
			System.out.println(i + " is read as " +a.get(0));
			a.incStartPointer(1);
		}
		System.out.println("size is " +a.size());
		System.out.println("start " +a.getStartPointer());
		System.out.println("end " +a.getEndPointer());
		
//		a.incStartPointer(5);
		
		a.add((byte)19);		
		a.add((byte)20);
		a.add((byte)21);
		a.add((byte)22);
		a.add((byte)23);

		System.out.println("After 5 more added size is " +a.size());
		System.out.println("start " +a.getStartPointer());
		System.out.println("end " +a.getEndPointer());

		
		System.out.println("First 9 bits");
		for (int i=0; i < 8; i++) {
			System.out.println(i + " is now " +a.get(0));
			a.incStartPointer(1);
		}
		System.out.println("At 0 is remaining " +a.get(0));
		System.out.println("size is " +a.size());
		System.out.println("start " +a.getStartPointer());
		System.out.println("end " +a.getEndPointer());	
		
		
	}

	@Test
	public void largeTest() {
		try {
			Log.init("test.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CircularByteBuffer a = new CircularByteBuffer(50);
		for (int i=0; i< 20; i++) {
			a.add((byte)i);
		}
				
		for (int i=0; i< 20; i++) {
			a.add((byte)i);
		}
		System.out.println("The first BYTES were:");
		for (int i=0; i< 20; i++) {
			System.out.println(a.get(0));
			a.incStartPointer(1);
		}

		
		for (int i=0; i< 20; i++) {
			a.add((byte)i);
		}

		System.out.println("The Second BYTES were:");
		for (int i=0; i< 20; i++) {
			System.out.println(a.get(0));
			a.incStartPointer(1);
		}
		
		System.out.println("The Third BYTES were:");
		for (int i=0; i< 19; i++) {
			System.out.println(a.get(0));
			a.incStartPointer(1);
		}
		System.out.println("Remaining at 0: " + a.get(0));

	}
}
