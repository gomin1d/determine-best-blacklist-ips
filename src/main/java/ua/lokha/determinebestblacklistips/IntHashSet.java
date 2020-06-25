package ua.lokha.determinebestblacklistips;


import java.util.Arrays;

public class IntHashSet implements IntSet {
	private static final int NBIT = 30;
	private static final int MAX_SIZE = 1073741824;
	private final int ndv;
	private int _nmax;
	private int _size;
	private int _nlo;
	private int _nhi;
	private int _shift;
	private int _mask;
	private int[] _values;

	public IntHashSet() {
		this(8, -2147483648);
	}

	public IntHashSet(int capacity) {
		this(capacity, -2147483648);
	}

	public IntHashSet(int capacity, int noDataValue) {
		this.ndv = noDataValue;
		this.setCapacity(capacity);
	}

	@Override
	public void clear() {
		this._size = 0;

		for(int i = 0; i < this._nmax; ++i) {
			this._values[i] = this.ndv;
		}

	}

	@Override
	public int size() {
		return this._size;
	}

	@Override
	public boolean isEmpty() {
		return this._size == 0;
	}

	public int[] getValues() {
		int index = 0;
		int[] values = new int[this._size];
		int[] var3 = this._values;
		int var4 = var3.length;

		for(int var5 = 0; var5 < var4; ++var5) {
			int _value = var3[var5];
			if (_value != this.ndv) {
				values[index++] = _value;
			}
		}

		return values;
	}

	@Override
	public boolean contains(int value) {
		return this._values[this.indexOf(value)] != this.ndv;
	}

	@Override
	public boolean remove(int value) {
		int i = this.indexOf(value);
		if (this._values[i] == this.ndv) {
			return false;
		} else {
			--this._size;

			while(true) {
				this._values[i] = this.ndv;
				int j = i;

				int r;
				do {
					do {
						do {
							i = i - 1 & this._mask;
							if (this._values[i] == this.ndv) {
								return true;
							}

							r = this.hash(this._values[i]);
						} while(i <= r && r < j);
					} while(r < j && j < i);
				} while(j < i && i <= r);

				this._values[j] = this._values[i];
			}
		}
	}

	@Override
	public boolean add(int value) {
		if (value == this.ndv) {
			throw new IllegalArgumentException("Can't add the 'no data' value");
		} else {
			int i = this.indexOf(value);
			if (this._values[i] == this.ndv) {
				++this._size;
				this._values[i] = value;
				if (this._size > 1073741824) {
					throw new RuntimeException("Too many elements (> 1073741824)");
				} else {
					if (this._nlo < this._size && this._size <= this._nhi) {
						this.setCapacity(this._size);
					}

					return true;
				}
			} else {
				return false;
			}
		}
	}

	private int hash(int key) {
		return 1327217885 * key >> this._shift & this._mask;
	}

	private int indexOf(int value) {
		int i;
		for(i = this.hash(value); this._values[i] != this.ndv; i = i - 1 & this._mask) {
			if (this._values[i] == value) {
				return i;
			}
		}

		return i;
	}

	private void setCapacity(int capacity) {
		if (capacity < this._size) {
			capacity = this._size;
		}

		int nbit = 1;

		int nmax;
		for(nmax = 2; nmax < capacity * 4 && nmax < 1073741824; nmax *= 2) {
			++nbit;
		}

		int nold = this._nmax;
		if (nmax != nold) {
			this._nmax = nmax;
			this._nlo = nmax / 4;
			this._nhi = 268435456;
			this._shift = 31 - nbit;
			this._mask = nmax - 1;
			this._size = 0;
			int[] values = this._values;
			this._values = new int[nmax];
			Arrays.fill(this._values, this.ndv);
			if (values != null) {
				for(int i = 0; i < nold; ++i) {
					int value = values[i];
					if (value != this.ndv) {
						++this._size;
						this._values[this.indexOf(value)] = value;
					}
				}
			}

		}
	}

	@Override
	public IntIterator iterator() {
		return new IntHashSet.IntHashSetIterator();
	}

	public int hashCode() {
		int h = 936247625;

		for(IntIterator it = this.iterator(); it.hasNext(); h += it.next()) {
		}

		return h;
	}

	public static IntHashSet of(int... members) {
		IntHashSet is = new IntHashSet(members.length);
		int[] var2 = members;
		int var3 = members.length;

		for(int var4 = 0; var4 < var3; ++var4) {
			int i = var2[var4];
			is.add(i);
		}

		return is;
	}

	private class IntHashSetIterator implements IntIterator {
		private int i = 0;

		IntHashSetIterator() {
		}

		@Override
		public boolean hasNext() {
			while(this.i < IntHashSet.this._values.length) {
				if (IntHashSet.this._values[this.i] != IntHashSet.this.ndv) {
					return true;
				}

				++this.i;
			}

			return false;
		}

		@Override
		public int next() {
			return IntHashSet.this._values[this.i++];
		}
	}
}
