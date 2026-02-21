/*
 * AnKeyboard - A smart learning keyboard for Android
 * Copyright (C) 2026 AnerysRynz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.ankeyboard.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.List;

/**
 * Manages language preferences and settings
 */
public class LanguageManager {
    private static final String PREF_NAME = "AnKeyboard_Language";
    private static final String KEY_UI_LANGUAGE = "ui_language";
    private static final String KEY_TRANSLATE_ENABLED = "translate_enabled";
    private static final String KEY_TRANSLATE_LANGUAGE = "translate_language";
    private static final String KEY_THEME = "theme";
    
    private SharedPreferences prefs;
    
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_AUTO = "auto";
    
    public LanguageManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get list of supported languages
     */
    public static List<String> getSupportedLanguages() {
        return Arrays.asList(
                "Indonesian (Bahasa Indonesia)",
                "English",
                "Spanish (Español)",
                "French (Français)",
                "German (Deutsch)",
                "Chinese (中文)",
                "Japanese (日本語)"
        );
    }
    
    /**
     * Get language codes for API
     */
    public static String[] getLanguageCodes() {
        return new String[]{"id", "en", "es", "fr", "de", "zh-CN", "ja"};
    }
    
    /**
     * Set UI language
     */
    public void setUILanguage(String language) {
        prefs.edit().putString(KEY_UI_LANGUAGE, language).apply();
    }
    
    /**
     * Get UI language
     */
    public String getUILanguage() {
        return prefs.getString(KEY_UI_LANGUAGE, "id");
    }
    
    /**
     * Enable/disable translation
     */
    public void setTranslateEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_TRANSLATE_ENABLED, enabled).apply();
    }
    
    /**
     * Check if translation is enabled
     */
    public boolean isTranslateEnabled() {
        return prefs.getBoolean(KEY_TRANSLATE_ENABLED, false);
    }
    
    /**
     * Set translation target language
     */
    public void setTranslateLanguage(String language) {
        prefs.edit().putString(KEY_TRANSLATE_LANGUAGE, language).apply();
    }
    
    /**
     * Get translation target language
     */
    public String getTranslateLanguage() {
        return prefs.getString(KEY_TRANSLATE_LANGUAGE, "en");
    }
    
    /**
     * Set theme
     */
    public void setTheme(String theme) {
        prefs.edit().putString(KEY_THEME, theme).apply();
    }
    
    /**
     * Get theme
     */
    public String getTheme() {
        return prefs.getString(KEY_THEME, THEME_AUTO);
    }
    
    /**
     * Check if dark mode is enabled
     */
    public boolean isDarkMode(Context context) {
        String theme = getTheme();
        if (THEME_AUTO.equals(theme)) {
            return (context.getResources().getConfiguration().uiMode & 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                    android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        return THEME_DARK.equals(theme);
    }
    
    /**
     * Convert language name to ISO code
     */
    public static String languageNameToCode(String name) {
        if (name.contains("Indonesian")) return "id";
        if (name.contains("English")) return "en";
        if (name.contains("Spanish")) return "es";
        if (name.contains("French")) return "fr";
        if (name.contains("German")) return "de";
        if (name.contains("Chinese") || name.contains("中文")) return "zh-CN";
        if (name.contains("Japanese") || name.contains("日本語")) return "ja";
        return "id";
    }
}
