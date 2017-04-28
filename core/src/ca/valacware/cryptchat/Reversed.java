package ca.valacware.cryptchat;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

class Reversed<T> implements Iterable<T> {
	private final List<T> original;

	private Reversed(List<T> original) {
		this.original = original;
	}

	public Iterator<T> iterator() {
		final ListIterator<T> i = original.listIterator(original.size());

		return new Iterator<T>() {
			public boolean hasNext() { return i.hasPrevious(); }
			public T next() { return i.previous(); }
			public void remove() { i.remove(); }
		};
	}

	static <T> Reversed<T> reversed(List<T> original) {
		return new Reversed<T>(original);
	}
}