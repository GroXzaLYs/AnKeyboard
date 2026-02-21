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

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btnEnable;
    private MaterialButton btnSelect;
    private MaterialButtonToggleGroup languageToggleGroup;
    private MaterialButtonToggleGroup themeToggleGroup;
    private SwitchMaterial translateSwitch;
    private TextInputEditText translateLanguageInput;
    
    private LanguageManager languageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize language manager
        languageManager = new LanguageManager(this);
        
        setContentView(R.layout.activity_main);
        
        // Initialize views
        btnEnable = findViewById(R.id.btn_enable);
        btnSelect = findViewById(R.id.btn_select);
        languageToggleGroup = findViewById(R.id.language_toggle_group);
        themeToggleGroup = findViewById(R.id.theme_toggle_group);
        translateSwitch = findViewById(R.id.translate_switch);
        translateLanguageInput = findViewById(R.id.translate_language_input);
        
        setupButtonListeners();
        setupLanguageSelector();
        setupThemeSelector();
        setupTranslateSettings();
    }
    
    private void setupButtonListeners() {
        btnEnable.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                startActivity(intent);
                Toast.makeText(MainActivity.this, 
                        getString(R.string.enable_desc), 
                        Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, 
                        getString(R.string.error), 
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnSelect.setOnClickListener(v -> {
            try {
                InputMethodManager imeManager = (InputMethodManager) 
                        getSystemService(INPUT_METHOD_SERVICE);
                if (imeManager != null) {
                    imeManager.showInputMethodPicker();
                } else {
                    Toast.makeText(MainActivity.this, 
                            getString(R.string.keyboard_not_enabled), 
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, 
                        getString(R.string.error), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupLanguageSelector() {
        String currentLang = languageManager.getUILanguage();
        int checkedId = getLanguageButtonId(currentLang);
        
        if (checkedId > 0) {
            languageToggleGroup.check(checkedId);
        } else {
            languageToggleGroup.check(languageToggleGroup.getChildAt(0).getId());
        }
        
        languageToggleGroup.addOnButtonCheckedListener((group, checkedId1, isChecked) -> {
            if (isChecked) {
                String selectedLanguage = getLanguageFromButtonId(checkedId1);
                languageManager.setUILanguage(selectedLanguage);
                // Optionally restart activity to apply language
                recreate();
            }
        });
    }
    
    private void setupThemeSelector() {
        String currentTheme = languageManager.getTheme();
        int themeButtonId = 0;
        
        if (LanguageManager.THEME_LIGHT.equals(currentTheme)) {
            themeButtonId = themeToggleGroup.getChildAt(0).getId();
        } else if (LanguageManager.THEME_DARK.equals(currentTheme)) {
            themeButtonId = themeToggleGroup.getChildAt(1).getId();
        } else {
            themeButtonId = themeToggleGroup.getChildAt(2).getId();
        }
        
        themeToggleGroup.check(themeButtonId);
        
        themeToggleGroup.addOnButtonCheckedListener((group, checkedId1, isChecked) -> {
            if (isChecked) {
                String theme = "";
                if (checkedId1 == themeToggleGroup.getChildAt(0).getId()) {
                    theme = LanguageManager.THEME_LIGHT;
                } else if (checkedId1 == themeToggleGroup.getChildAt(1).getId()) {
                    theme = LanguageManager.THEME_DARK;
                } else {
                    theme = LanguageManager.THEME_AUTO;
                }
                languageManager.setTheme(theme);
                Toast.makeText(MainActivity.this, 
                        getString(R.string.success), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupTranslateSettings() {
        // Initialize translate switch
        translateSwitch.setChecked(languageManager.isTranslateEnabled());
        
        translateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            languageManager.setTranslateEnabled(isChecked);
            translateLanguageInput.setEnabled(isChecked);
            if (isChecked) {
                setupTranslateLanguageDropdown();
            }
        });
        
        if (languageManager.isTranslateEnabled()) {
            setupTranslateLanguageDropdown();
        } else {
            translateLanguageInput.setEnabled(false);
        }
    }
    
    private void setupTranslateLanguageDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                LanguageManager.getSupportedLanguages()
        );
        
        String currentTranslateLang = languageManager.getTranslateLanguage();
        String currentLanguageName = getLanguageNameFromCode(currentTranslateLang);
        
        translateLanguageInput.setText(currentLanguageName);
        
        translateLanguageInput.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.translate_language))
                    .setAdapter(adapter, (dialog, which) -> {
                        String selectedLanguage = adapter.getItem(which);
                        String languageCode = LanguageManager.languageNameToCode(selectedLanguage);
                        languageManager.setTranslateLanguage(languageCode);
                        translateLanguageInput.setText(selectedLanguage);
                    })
                    .show();
        });
    }
    
    private int getLanguageButtonId(String language) {
        switch(language.toLowerCase()) {
            case "id":
                return languageToggleGroup.getChildAt(0).getId();
            case "en":
                return languageToggleGroup.getChildAt(1).getId();
            case "es":
                return languageToggleGroup.getChildAt(2).getId();
            case "fr":
                return languageToggleGroup.getChildAt(3).getId();
            default:
                return -1;
        }
    }
    
    private String getLanguageFromButtonId(int buttonId) {
        if (buttonId == languageToggleGroup.getChildAt(0).getId()) return "id";
        if (buttonId == languageToggleGroup.getChildAt(1).getId()) return "en";
        if (buttonId == languageToggleGroup.getChildAt(2).getId()) return "es";
        if (buttonId == languageToggleGroup.getChildAt(3).getId()) return "fr";
        return "id";
    }
    
    private String getLanguageNameFromCode(String code) {
        switch(code.toLowerCase()) {
            case "id": return "Indonesian (Bahasa Indonesia)";
            case "en": return "English";
            case "es": return "Spanish (Español)";
            case "fr": return "French (Français)";
            case "de": return "German (Deutsch)";
            case "zh-cn": return "Chinese (中文)";
            case "ja": return "Japanese (日本語)";
            default: return "English";
        }
    }
}

