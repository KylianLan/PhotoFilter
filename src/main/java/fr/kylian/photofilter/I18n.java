package fr.kylian.photofilter;

import com.sun.jna.platform.unix.Resource;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class I18n {

    private static final String BUNDLE_BASE_NAME = "fr.kylian.photofilter.messages";
    private static final Preferences PREFS = Preferences.userNodeForPackage(I18n.class);
    private static final String PREF_KEY_LANG = "app_locale";

    private static Locale currentLocale;
    private static ResourceBundle bundle;

    static {
        String savedLang = PREFS.get(PREF_KEY_LANG, Locale.getDefault().getLanguage());
        setLocale(savedLang.equalsIgnoreCase("en") ? Locale.ENGLISH : Locale.FRENCH);
    }

    private I18n() {}

    public static void setLocale(Locale locale) {
        currentLocale = locale;
        Locale.setDefault(locale);
        bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
        PREFS.put(PREF_KEY_LANG, locale.getLanguage());
    }

    public static Locale getLocale() {
        return currentLocale;
    }
    public static ResourceBundle getBundle() {
        return bundle;
    }
    public static String get(String key) {
        return bundle.getString(key);
    }
    public static String get(String key, Object... args) {
        return MessageFormat.format(bundle.getString(key), args);
    }

}
