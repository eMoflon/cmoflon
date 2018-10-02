package org.cmoflon.ide.core.utilities;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.moflon.core.utilities.UtilityClassNotInstantiableException;

public class FormattingUtils {

	private FormattingUtils() {
		throw new UtilityClassNotInstantiableException();
	}

	/**
	 * @return one newlines
	 */
	public static String nl() {
		return "\n";
	}

	/**
	 * @return two newlines
	 */
	public static String nl2() {
		return nl() + nl();
	}

	/**
	 * @return level 1 indent
	 */
	public static String idt() {
		return "  ";
	}

	/**
	 * @return level 2 indent
	 */
	public static String idt2() {
		return idt() + idt();
	}

	/**
	 * Prepends to each line in the give string the given prefix
	 *
	 * This method uses {@link #nl()} to identify lines
	 *
	 * @param code
	 *            the string
	 * @param prefix
	 *            the prefix
	 * @return the modified string
	 */
	public static String prependEachLineWithPrefix(final String code, final String prefix) {
		return Arrays.asList(code.split(Pattern.quote(nl()))).stream().map(s -> prefix + s)
				.collect(Collectors.joining(nl()));
	}
}
