package decoder;

import common.Log;
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
 * This is a circular buffer 
 * 
 * We use two pointers to remember where we are in the array.
 * At any point in time there is a virtual array that starts from the startPointer and ends at the endPointer.
 * The endPointer is the position of the next entry we will make.  It is incremented when a new value is added. 
 * The startPointer is the beginning of the virtual array.  It is always "behind" the endPointer.  This is the position
 * that we read from if we read element [0].  We purge data from the array by increasing the startPointer.
 * 
 * A virtual array of length 1 will have the start point and end pointer in adjacent values.  We can not have an array of
 * length zero except at start up.
 * 
 * We do not overwrite data.  If the data will be overwritten then we throw an exception and it is up to the calling routine to wait for
 * room in the buffer or to report an error.  In this way we keep track of glitches in the audio.
 * 
 * We MUST add or retrieve bytes two at a time, otherwise the integrity of the buffer can be compromised and the audio data no longer
 * makes sense.
 * 
 * @author chris.e.thompson g0kla/ac2cz
 *
 */

public class CircularByteBuffer {
	byte[] bytes;
	int bufferSize = 0;; 
	int startPointer = 0;
	int endPointer = 0;
	public static final int DEFAULT_SIZE = 48000*2; // 0.5 second
	int statusCount = 0;
	
	public CircularByteBuffer(int size) {
		bytes = new byte[size];
		bufferSize = size;
//		startPointer = bufferSize - 1; // initialize this to the end of the array, otherwise we can not write data to it
		Log.println("Created circular BYTE buffer with " + bufferSize + " bytes");
	}

	public int getStartPointer() { return startPointer; }
	public int getEndPointer() { return endPointer; }
	public int getCapacity() { // how many bytes can we add without the end pointer reaching the start pointer
		if (startPointer == 0 && endPointer == 0) {
			return bufferSize;  // Special case when we have not added any data at all
		}
		int size = 0;
		if (endPointer > startPointer)  // Then we have the distance to the end of the array plus the start pointer
			size = bufferSize - endPointer + startPointer;
		else {  // endPointer < StartPointer 
			// we only have the distance from the end pointer to the start pointer
			size = startPointer - endPointer;
		}
		return size;	
		
	}

	public boolean add(byte one, byte two, byte three, byte four) {
		if (endPointer+1 == startPointer) {
			throw new IndexOutOfBoundsException("End pointer has reached start pointer");
		} 
		if (endPointer+2 == startPointer) {
			throw new IndexOutOfBoundsException("End pointer one byte from start pointer");
		} 
		if (endPointer+3 == startPointer) {
			throw new IndexOutOfBoundsException("End pointer two bytes from start pointer");
		} 
		if (endPointer+4 == startPointer) {
			throw new IndexOutOfBoundsException("End pointer three bytes from start pointer");
		} 
		add(one);
		add(two);
		add(three);
		add(four);
		return true;
	}

	public boolean add(byte one, byte two) {
		if (endPointer+1 == startPointer) {
			throw new IndexOutOfBoundsException("End pointer has reached start pointer");
		} 
		if (endPointer+2 == startPointer) {
			throw new IndexOutOfBoundsException("End pointer one byte from start pointer");
		} 
		add(one);
		add(two);
		return true;
	}
	/**
	 * Add data from the end pointer.  This only changes the end pointer and throws and error if it reaches the start pointer
	 * @param o
	 * @return
	 */
	public boolean add(byte o) {
			bytes[endPointer] = o;

			endPointer++;
			statusCount++;
		/*
			if (statusCount > 1000000) {
				System.out.println("Buffer Size: "+size()+" of " + bufferSize);
				statusCount = 0;
			}
		*/
			if (endPointer == bufferSize)
				endPointer = 0;

			if (endPointer == startPointer) { // then we have caught up with it
				endPointer--;
				if (endPointer == -1)
					endPointer = bufferSize-1;
				throw new IndexOutOfBoundsException("End pointer has reached start pointer");
			}
		return true;
	}
	
	private int incPointer(int pointer, int amount) {

		int p = pointer + amount;
		if (p >= bufferSize) {
			// We need to wrap around the array
			p = p % bufferSize;
		}
		return p;
	}
	
	/**
	 * This returns the ith element of the virtual array starting from the startPointer.
	 * Typically the startPointer is then incremented if the data is "consumed"
	 * An error is generated if it will go past the end pointer
	 */
	public byte get(int i) {
		if (i > size())
			throw new IndexOutOfBoundsException("Attempt to read past end pointer");
		int p = incPointer(startPointer, i);
		return bytes[p];
	}
	
	
	/** 
	 * Set the start position to a new point.  
	 */
	public void incStartPointer(int amount) {
		// snapshot the value to avoid failing the check due to a race condition
		int e = endPointer;
		if (e > startPointer ) {
			// then the startPointer needs to remain less than the end pointer after the increment
			if (startPointer + amount >= e)
				throw new IndexOutOfBoundsException("Attempt to move start pointer " + startPointer + " past end pointer " + e);
		} else {
			// if it wraps then it needs to stay less, otherwise we are fine
			if (startPointer + amount >= bufferSize) {
				int testPointer = incPointer(startPointer, amount);
				if (testPointer >= endPointer)
					throw new IndexOutOfBoundsException("Attempt to wrap start pointer " + startPointer + " past end pointer " + e);
			}
		}
		startPointer = incPointer(startPointer, amount);
	}
	
	/**
	 * This returns the size of the virtual array.  It is mainly used to find the end point so that we
	 * can read backwards from the end in the bitStream
	 */
	public int size() {
		int size = 0;
		int e = endPointer; // snapshot the end pointer to avoid a race condition in the checks below.  The size can only grow if the end pointer moves, so this is safe
		if (e >= startPointer)
			size = e - startPointer;
		else {
			size = bufferSize - startPointer; // distance from start to end of the real array
			size = size + e;  //  add the distance from the start to the write pointer
		}
		return size;	
	}
}
