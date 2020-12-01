package decoder;

import java.util.ArrayList;

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
 * This is a circular buffer that looks like an ArrayList.  This is not the most efficient implementation,
 * but was chosen because it was easy to port the existing code.  If this gives performance issues then it
 * may need porting in the future.
 * 
 * We use two pointers to remember where we are in the array.
 * At any point in time there is a virtual array that starts from the start/readPointer and ends at the end/writePointer.
 * The endPointer is the position of the last entry.  It is incremented when a new value is added.  This purges the last entry in the array
 * The startPointer is the beginning of the virtual array.  It is always "behind" the writePointer.  This is the position
 * that we read from if we read element [0].  We purge data from the array by increasing the startPointer.
 * 
 * @author chris.e.thompson g0kla/ac2cz
 *
 */
@SuppressWarnings("serial")
public class CircularBuffer extends ArrayList<Boolean> {
	int bufferSize = 0;; 
	int startPointer = 0;
	int endPointer = -1;
	
	
	public CircularBuffer(int initialSize) {
		super(initialSize);
		bufferSize = initialSize;
		startPointer = bufferSize -1; // initialize this to the end of the array, otherwise we can not write data to it
		for (int i=0; i< bufferSize; i++)
			super.add(false);
		Log.println("Created circular buffer with " + initialSize + " bits");
	}
	
	public int getStartPointer() { return startPointer; }
	public int getEndPointer() { return endPointer; }
	
	public boolean add(Boolean o) {
		endPointer++;
		if (endPointer == bufferSize)
			endPointer = 0;
		
		if (endPointer == startPointer) { // then we have caught up with it
			endPointer--;
			if (endPointer == -1)
				endPointer = bufferSize-1;
			throw new IndexOutOfBoundsException("End pointer has reached start pointer");
		}
		super.set(endPointer, o);
		return true;
	}
	
	private int incPointer(int pointer, int amount) {
		int p = pointer + amount;
		if (p >= bufferSize) {
			// We need to wrap aroung the array
			p = p % bufferSize;
		}
		return p;
	}
	
	/**
	 * This returns the ith element of the virtual array.  
	 */
	public Boolean get(int i) {
		if (i > size())
			throw new IndexOutOfBoundsException("Attempt to read past end pointer");
		
		int p = incPointer(startPointer, i);
		return super.get(p);
	}
	
	/** 
	 * Set the start position to a new point.
	 */
	public void incStartPointer(int amount) {
		startPointer = incPointer(startPointer, amount);
		
	}
	
	/**
	 * This returns the size of the virtual array.  It is mainly used to find the end point so that we
	 * can read backwards from the end in the bitStream
	 */
	public int size() {
		int size = 0;
		if (endPointer >= startPointer)
			size = endPointer - startPointer;
		else {
			size = bufferSize - startPointer; // distance from start to end of the real array
			size = size + endPointer;  //  add the distance from the start to the write pointer
		}
		return size;
		
	}
}
