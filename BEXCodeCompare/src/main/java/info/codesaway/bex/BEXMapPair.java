package info.codesaway.bex;

import java.util.Map;
import java.util.function.Supplier;

/**
 * BEXPair containing a left and right map
 *
 * <p>This class has some helper methods to operate on both sides of the map together</p>
 *
 * @param <K> the type of keys in the maps
 * @param <V> the type of values in the maps
 * @since 0.3
 */
public class BEXMapPair<K, V> implements BEXPairCore<Map<K, V>> {
	private final BEXPair<Map<K, V>> pair;

	/**
	 *
	 * @param supplier the supplier used to initialize both the left and right maps (typically a constructor reference such as <code>HashMap::new</code>)
	 */
	public BEXMapPair(final Supplier<? extends Map<K, V>> supplier) {
		this(supplier.get(), supplier.get());
	}

	public BEXMapPair(final Map<K, V> left, final Map<K, V> right) {
		this(new BEXPair(left, right));
	}

	public BEXMapPair(final BEXPair<Map<K, V>> mapPair) {
		this.pair = mapPair;
	}

	/**
	 * Gets the left map
	 * @return the left map
	 */
	@Override
	public Map<K, V> getLeft() {
		return this.pair.getLeft();
	}

	/**
	 * Gets the right map
	 * @return the right map
	 */
	@Override
	public Map<K, V> getRight() {
		return this.pair.getRight();
	}

	/**
	 * Gets the map on the specified side
	 * @param side the side
	 * @return
	 */
	@Override
	public Map<K, V> get(final BEXSide side) {
		return this.pair.get(side);
	}

	public BEXPair<V> get(final BEXPair<K> keyPair) {
		return this.get(keyPair.getLeft(), keyPair.getRight());
	}

	public BEXPair<V> get(final K leftKey, final K rightKey) {
		V left = this.getLeft().get(leftKey);
		V right = this.getRight().get(rightKey);

		return new BEXPair<>(left, right);
	}
}
