package telemetry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
 * Sorted Array List to store Comparable objects
 */
@SuppressWarnings("serial")
public class SortedArrayList<T extends Comparable<T>> extends ArrayList<T> {


	public SortedArrayList(int i) {
		super(i);
	}

	public boolean addToEnd(T img) {
		return this.add(img);
	}
	
	/**
	 * Add an item to the array and insert it at the correct place
	 * Begin searching from the end in the hope that we are appending and that is fastest.
	 * 
	 * @param n - the item to add
	 * @return boolean - true if this was inserted correctly
	 */
	public boolean add(T img) {

		// find the index of the item with priority just less than this, starting at end
		// We hope this is the fastest way given we are usually appending items
		for (int i=this.size()-1; i>=0; i--) { 
			if (this.get(i).compareTo (img) > 0) { 
				// positive compare result means that the item in the array is higher priority (lower uptime) than the item we are comparing to
				// so add the new item after this one
				this.add(i+1, img);                
				return true;
			} 
		}
		this.add(0, img); // put it at the start if for some reason we did not add it already
		return true;
	}
	
	/**
     * Add an item to the array and insert it at the correct place
     * 
     * @param n - the item to add
     * @return boolean - true if this was inserted correctly
     */
    public boolean addSearchFromStart(T n) {

        // find the index of the item with priority just less than this, starting at beginning
        for (int i=0; i<this.size(); i++) { 
            if (this.get(i).compareTo (n) < 0) { 
                // add before this item, so the highest priority is always first
                this.add(i, n);                
                return true;
            } 
        }
        this.add(this.size(), n); // put it at the end
        return true;
    }
    
    /**
     * Add a list of items to the sorted array
     * @param list - the list to add
     */
    public void addAll(List<T> list) {
	for (Iterator<T>  i = list.listIterator() ; i.hasNext() ;) {
            this.add(i.next());
        }
    }


    /**
     * Add a list of items to the sorted array
     * @param list - the list to add
     */
    public void addAll(SortedArrayList<T> list) {
	for (Iterator<T>  i = list.iterator() ; i.hasNext() ;) {
            this.add(i.next());
        }
    }

}
