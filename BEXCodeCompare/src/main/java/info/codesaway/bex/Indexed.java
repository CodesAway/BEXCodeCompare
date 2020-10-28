package info.codesaway.bex;

/**
 * Value with an index.
 *
 * @param <T> the value's type
 * @since 0.13
 */
public interface Indexed<T> {
	public int getIndex();

	public T getValue();
}