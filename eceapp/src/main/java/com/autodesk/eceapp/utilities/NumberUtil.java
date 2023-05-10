package com.autodesk.eceapp.utilities;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.beanutils.converters.FloatConverter;

public final class NumberUtil {
  private final static String DEFAULT_LOCALE = "en-US";

  private NumberUtil() {}

  public static Float convert(String input, String locale) {
    String[] languageTagArray = Optional.ofNullable(locale).orElse(DEFAULT_LOCALE).trim().split("_|-");
    if (languageTagArray.length != 2) {
      throw new RuntimeException(MessageFormat.format("Format of locale [{0}] looks incorrect", locale));
    }

    String formattedLanguageTag = languageTagArray[0].trim() + "-" + languageTagArray[1].trim();
    String formattedInput = input.replaceAll("[^\\d.,]", "").trim();

    FloatConverter floatConverter = new FloatConverter(null);
    floatConverter.setLocale(Locale.forLanguageTag(formattedLanguageTag));
    return floatConverter.convert(Float.class, formattedInput);
  }
}
