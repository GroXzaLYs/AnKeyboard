/*
 * AnKeyboard - A smart learning keyboard for Android
 * Copyright (C) 2026 AnerysRynz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ankeyboard.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

/**
 * Settings activity for keyboard preferences
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            try {
                setPreferencesFromResource(R.xml.preferences, rootKey);
                
                // Language preference
                ListPreference languagePref = findPreference("ui_language");
                if (languagePref != null) {
                    languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                        LanguageManager langManager = new LanguageManager(requireContext());
                        langManager.setUILanguage((String) newValue);
                        return true;
                    });
                }
                
                // Theme preference
                ListPreference themePref = findPreference("theme");
                if (themePref != null) {
                    themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                        LanguageManager langManager = new LanguageManager(requireContext());
                        langManager.setTheme((String) newValue);
                        return true;
                    });
                }
                
                // Enable translation
                SwitchPreferenceCompat translateSwitch = findPreference("translate_enabled");
                if (translateSwitch != null) {
                    translateSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                        LanguageManager langManager = new LanguageManager(requireContext());
                        langManager.setTranslateEnabled((Boolean) newValue);
                        return true;
                    });
                }
                
                // Translation language preference
                ListPreference transLangPref = findPreference("translate_language");
                if (transLangPref != null) {
                    transLangPref.setOnPreferenceChangeListener((preference, newValue) -> {
                        LanguageManager langManager = new LanguageManager(requireContext());
                        langManager.setTranslateLanguage((String) newValue);
                        return true;
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
