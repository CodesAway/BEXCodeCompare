package info.codesaway.bex;

import java.util.AbstractSet;
import java.util.Collection;

public abstract class AbstractImmutableSet<E> extends AbstractSet<E> {
	@Override
	public abstract int size();

	@Override
	public abstract boolean contains(final Object o);

	@Override
	public boolean containsAll(final Collection<?> c) {
		return c.stream().allMatch(this::contains);
	}

	@Override
	public final boolean add(final E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean remove(final Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean addAll(final Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean retainAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean removeAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void clear() {
		throw new UnsupportedOperationException();
	}
}
