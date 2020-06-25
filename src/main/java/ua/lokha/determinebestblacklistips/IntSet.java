package ua.lokha.determinebestblacklistips;

public interface IntSet {
	void clear();

	int size();

	boolean isEmpty();

	boolean contains(int var1);

	boolean remove(int var1);

	boolean add(int var1);

	IntIterator iterator();
}

