package name.azzurite.mcserver.config;

import java.text.NumberFormat;
import java.util.Locale;

public class Constants {
	public static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance(Locale.ENGLISH);
	public static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.ENGLISH);
	static {
		PERCENT_FORMAT.setMinimumIntegerDigits(2);
		PERCENT_FORMAT.setMinimumFractionDigits(1);
		PERCENT_FORMAT.setMaximumFractionDigits(1);

		NUMBER_FORMAT.setMaximumFractionDigits(1);
	}
}
