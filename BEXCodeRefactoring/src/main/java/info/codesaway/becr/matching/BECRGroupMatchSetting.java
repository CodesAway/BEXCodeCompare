package info.codesaway.becr.matching;

import java.util.HashMap;
import java.util.Map;

/**
 * BECR group match settings
 *
 * <p>Instances of this class are immutable and cached.
 * Methods which turn on or off flags return a different settings object (from cache when possible) with the specified settings changed</p>
 */
final class BECRGroupMatchSetting {
	private static final Map<Integer, BECRGroupMatchSetting> CACHE = new HashMap<>();

	private final int flags;

	static final BECRGroupMatchSetting DEFAULT = new BECRGroupMatchSetting(0);

	static final int STOP_WHEN_VALID = 0x01;
	static final int OPTIONAL = 0x02;
	static final int MATCH_ANGLE_BRACKETS = 0x04;

	private BECRGroupMatchSetting(final int flags) {
		this.flags = flags;
	}

	BECRGroupMatchSetting valueOf(final int flags) {
		return CACHE.computeIfAbsent(flags, BECRGroupMatchSetting::new);
	}

	public boolean isDefault() {
		return this.flags == 0;
	}

	public boolean shouldStopWhenValid() {
		return this.has(STOP_WHEN_VALID);
	}

	public boolean isOptional() {
		return this.has(OPTIONAL);
	}

	public boolean shouldMatchAngleBrackets() {
		return this.has(MATCH_ANGLE_BRACKETS);
	}

	/**
	 * Turns on the specified flag
	 *
	 * @param flag the flag to turn on
	 * @return a different settings object (from cache when possible) with the specified settings turned on
	 */
	BECRGroupMatchSetting turnOn(final int flag) {
		return this.valueOf(this.flags | flag);
	}

	/**
	 * Turns off the specified flag
	 *
	 * @param flag the flag to turn off
	 * @return a different settings object (from cache when possible) with the specified settings turned off
	 */
	BECRGroupMatchSetting turnOff(final int flag) {
		return this.valueOf(this.flags & ~flag);
	}

	/**
	 * Indicates whether a particular flag is set or not.
	 *
	 * @param flag
	 *            the flag to test
	 * @return <code>true</code> if the particular flag is set
	 */
	boolean has(final int flag) {
		return (this.flags & flag) != 0;
	}
}
