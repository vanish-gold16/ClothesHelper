package com.example.clotheshelper.ui.theme;

import java.util.prefs.Preferences;

public class ThemePreferences {
    private static final String THEME_PREFERENCE_KEY = "theme";

    private final Preferences preferences;

    public ThemePreferences(Class<?> ownerClass) {
        this.preferences = Preferences.userNodeForPackage(ownerClass);
    }

    public AppTheme load() {
        String themeName = preferences.get(THEME_PREFERENCE_KEY, AppTheme.LIGHT.name());
        try {
            return AppTheme.valueOf(themeName);
        } catch (IllegalArgumentException exception) {
            return AppTheme.LIGHT;
        }
    }

    public void save(AppTheme theme) {
        preferences.put(THEME_PREFERENCE_KEY, theme.name());
    }
}
