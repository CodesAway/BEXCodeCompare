package info.codesaway.bex;

import java.util.Objects;

/**
 * Value with an index.
 *
 * <p>Instances of this class are immutable if the generic class type is also immutable</p>
 *
 * @param <T> the value's type
 * @since 0.13
 */
public final class IndexedValue<T> implements Indexed<T> {
	// Referenced https://github.com/poetix/protonpack/blob/master/src/main/java/com/codepoetics/protonpack/Indexed.java

	private final int index;
	private final T value;

	public IndexedValue(final int index, final T value) {
		this.index = index;
		this.value = value;
	}

	@Override
	public int getIndex() {
		return this.index;
	}

	@Override
	public T getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.index + " " + this.value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.index, this.value);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		IndexedValue<?> other = (IndexedValue<?>) obj;
		return this.index == other.index && Objects.equals(this.value, other.value);
	}
}
