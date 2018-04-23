package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.util.Arrays;

/**
 * Efficient replacement for {@code ArrayList<Integer>}.
 * 
 * We intentionally do <em>not</em> implement {@code Collection}!
 *
 * @author Erich Schubert
 */
public class IntArrayList {
	/** Data storage */
	int[] data;
	/** Used storage */
	int len;

	/**
	 * Constructor.
	 */
	public IntArrayList() {
		this(11);
	}

	/**
	 * Constructor.
	 *
	 * @param capacity
	 *                Capacity
	 */
	public IntArrayList(int capacity) {
		data = new int[capacity];
	}

	/**
	 * Size of list.
	 *
	 * @return size
	 */
	public int size() {
		return len;
	}

	/**
	 * Get an entry.
	 *
	 * @param p
	 *                Position
	 * @return Value
	 */
	public int get(int p) {
		assert (p < len);
		return data[p];
	}

	/**
	 * Append a value.
	 *
	 * @param v
	 *                Value to append.
	 */
	public void add(int v) {
		if (data.length == len) {
			int newlen = len < 5 ? 11 : (len + (len >> 1)) | 1;
			data = Arrays.copyOf(data, newlen);
		}
		data[len++] = v;
	}

	/**
	 * Sort the list.
	 */
	public void sort() {
		Arrays.sort(data, 0, len);
	}

	/**
	 * Sort and remove duplicates.
	 */
	public void sortRemoveDuplicates() {
		if (len == 0)
			return;
		int prev = data[0];
		int p = 1;
		for (int i = 1; i < len; i++) {
			int next = data[i];
			if (next == prev)
				continue;
			prev = data[p++] = next;
		}
		len = p;
	}

	/**
	 * Perform a binary search (array must be sorted).
	 *
	 * @param key
	 *                Key to search
	 * @return Position. Negative values indicate insertion positions.
	 */
	public int binarySearch(int key) {
		return Arrays.binarySearch(data, 0, len, key);
	}

	/**
	 * Clear the list.
	 */
	public void clear() {
		len = 0;
	}

	/**
	 * Check if the list is empty.
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return len == 0;
	}
}