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

import android.app.Dialog;
import android.widget.GridView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.content.res.Configuration;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.Typeface;
import java.util.List;

/**
 * Main keyboard service with word learning, emoji, and translation features
 */
public class AnKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private Keyboard selectionKeyboard;
    private LinearLayout candidateLayout;
    
    private LearningDictionary brain;
    private LanguageManager languageManager;
    
    private boolean isCaps = false;
    private boolean isSelectionMode = false;
    private StringBuilder composing = new StringBuilder();
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            brain = new LearningDictionary(this);
            languageManager = new LanguageManager(this);
            handler = new Handler(Looper.getMainLooper());
            seedInitialData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize with common words
     */
    private void seedInitialData() {
        try {
            if (brain.getPredictions("a").size() < 2) {
                brain.learnWord("AnKeyboard");
                brain.learnWord("Hello");
                brain.learnWord("What");
                brain.learnWord("How");
                brain.learnWord("Good");
                brain.learnWord("Thank");
                brain.learnWord("Please");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateInputView() {
        try {
            keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
            keyboard = new Keyboard(this, R.xml.qwerty);
            selectionKeyboard = new Keyboard(this, R.xml.selection);
            
            if (keyboardView != null) {
                keyboardView.setKeyboard(keyboard);
                keyboardView.setOnKeyboardActionListener(this);
                keyboardView.setPreviewEnabled(false);
                updateKeyboardTheme();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return keyboardView;
    }

    @Override
    public View onCreateCandidatesView() {
        try {
            View v = getLayoutInflater().inflate(R.layout.candidate_view, null);
            if (v != null) {
                candidateLayout = v.findViewById(R.id.candidate_layout);
            }
            return v;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;

            switch (primaryCode) {
                case Keyboard.KEYCODE_DELETE:
                    handleBackspace(ic);
                    break;
                    
                case Keyboard.KEYCODE_SHIFT:
                    isCaps = !isCaps;
                    keyboard.setShifted(isCaps);
                    if (keyboardView != null) {
                        keyboardView.invalidateAllKeys();
                    }
                    break;
                    
                case Keyboard.KEYCODE_DONE: 
                case 10:
                    commitAndLearn(ic, "\n");
                    break;
                    
                case 32: // Space
                    commitAndLearn(ic, " ");
                    break;
                    
                case -100: // Emoji
                    showEmojiPicker();
                    break;
                    
                case -200: // Select / Done
                    toggleSelectionMode();
                    break;
                    
                case -201: // Expand left
                    expandSelectionLeft();
                    break;
                    
                case -202: // Expand right
                    expandSelectionRight();
                    break;
                    
                case -203: // Select all
                    selectAll();
                    break;
                    
                case -204: // Cut
                    cutText();
                    break;
                    
                case -205: // Copy
                    copyText();
                    break;
                    
                case -206: // Paste
                    pasteText();
                    break;
                    
                default:
                    char code = (char) primaryCode;
                    if (Character.isLetter(code) && isCaps) {
                        code = Character.toUpperCase(code);
                    }
                    
                    composing.append(code);
                    ic.setComposingText(composing, 1);
                    updateCandidates();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Commit word and learn it
     */
    private void commitAndLearn(InputConnection ic, String separator) {
        try {
            if (composing.length() > 0) {
                String wordTyped = composing.toString();
                
                ic.commitText(wordTyped, 1);
                brain.learnWord(wordTyped);
                
                // Translate if enabled
                if (languageManager.isTranslateEnabled()) {
                    translateWord(wordTyped, ic);
                }
                
                composing.setLength(0);
            }
            
            ic.commitText(separator, 1);
            updateCandidates();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Translate word asynchronously
     */
    private void translateWord(String word, InputConnection ic) {
        new Thread(() -> {
            try {
                String targetLang = languageManager.getTranslateLanguage();
                String translated = TranslateManager.translate(word, targetLang);
                
                if (translated != null && !translated.equals(word)) {
                    // Optionally show translated word in candidates
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Handle backspace/delete
     */
    private void handleBackspace(InputConnection ic) {
        try {
            final int length = composing.length();
            if (length > 1) {
                composing.delete(length - 1, length);
                ic.setComposingText(composing, 1);
                updateCandidates();
            } else if (length > 0) {
                composing.setLength(0);
                ic.commitText("", 0);
                updateCandidates();
            } else {
                ic.deleteSurroundingText(1, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Update word suggestions
     */
    private void updateCandidates() {
        try {
            if (candidateLayout == null) return;
            candidateLayout.removeAllViews();

            if (composing.length() > 0) {
                setCandidatesViewShown(true);
                
                List<String> suggestions = brain.getPredictions(composing.toString());

                // First suggestion as autocorrect
                if (!suggestions.isEmpty()) {
                    String autocorrect = suggestions.get(0);
                    if (!autocorrect.equalsIgnoreCase(composing.toString())) {
                        Button btn = createSuggestionButton(autocorrect + " (Auto)", true);
                        final String finalAutocorrect = autocorrect;
                        btn.setOnClickListener(v -> pickAutoCorrect(finalAutocorrect));
                        candidateLayout.addView(btn);
                    }
                }

                // Other suggestions
                for (int i = 1; i < Math.min(6, suggestions.size()); i++) {
                    final String suggestion = suggestions.get(i);
                    if (!suggestion.equalsIgnoreCase(composing.toString())) {
                        Button btn = createSuggestionButton(suggestion, false);
                        btn.setOnClickListener(v -> pickSuggestion(suggestion));
                        candidateLayout.addView(btn);
                    }
                }
            } else {
                setCandidatesViewShown(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create suggestion button
     */
    private Button createSuggestionButton(String text, boolean isAutocorrect) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        btn.setTypeface(null, isAutocorrect ? Typeface.BOLD : Typeface.NORMAL);
        btn.setBackgroundColor(isAutocorrect ? Color.parseColor("#E8F5E8") : Color.TRANSPARENT);
        btn.setTextColor(Color.parseColor("#1F1F1F"));
        btn.setPadding(30, 0, 30, 0);
        btn.setAllCaps(false);
        return btn;
    }

    /**
     * Pick a suggestion
     */
    private void pickSuggestion(String suggestion) {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(suggestion + " ", 1);
                brain.learnWord(suggestion);
                composing.setLength(0);
                updateCandidates();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Pick autocorrected word
     */
    private void pickAutoCorrect(String autocorrect) {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.setComposingText(autocorrect, 1);
                composing.setLength(0);
                composing.append(autocorrect);
                updateCandidates();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show emoji picker dialog
     */
    private void showEmojiPicker() {
        try {
            Dialog dialog = new Dialog(this);
            dialog.setTitle(R.string.emoji);
            
            GridView gridView = new GridView(this);
            String[] emojis = {"😀", "😂", "😊", "😍", "🥰", "😘", "😉", "😎", "🤔", "😢", 
                              "😭", "😤", "😡", "🥺", "😴", "🤤", "🤗", "🤭", "🤫", "🤥", 
                              "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "👍",
                              "👎", "👏", "🙌", "🤝", "💪", "🤗", "🎉", "🎊", "🎈", "🎁",
                              "⭐", "✨", "💫", "🌟", "💥", "🔥", "👀", "💯", "🤷", "🤨"};
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, emojis);
            gridView.setAdapter(adapter);
            gridView.setNumColumns(8);
            gridView.setOnItemClickListener((parent, view, position, id) -> {
                try {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.commitText(emojis[position], 1);
                    }
                    dialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            
            dialog.setContentView(gridView);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Toggle selection mode
     */
    private void toggleSelectionMode() {
        try {
            isSelectionMode = !isSelectionMode;
            if (isSelectionMode) {
                if (selectionKeyboard != null && keyboardView != null) {
                    keyboardView.setKeyboard(selectionKeyboard);
                }
                startSelection();
            } else {
                if (keyboard != null && keyboardView != null) {
                    keyboardView.setKeyboard(keyboard);
                }
            }
            if (keyboardView != null) {
                keyboardView.invalidateAllKeys();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Start text selection
     */
    private void startSelection() {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                CharSequence selected = ic.getSelectedText(0);
                if (selected == null || selected.length() == 0) {
                    CharSequence before = ic.getTextBeforeCursor(1000, 0);
                    if (before != null && before.length() > 0) {
                        ic.setSelection(before.length() - 1, before.length());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Expand selection left
     */
    private void expandSelectionLeft() {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                CharSequence before = ic.getTextBeforeCursor(1000, 0);
                int beforeLen = before != null ? before.length() : 0;
                if (beforeLen > 0) {
                    ic.setSelection(beforeLen - 1, beforeLen);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Expand selection right
     */
    private void expandSelectionRight() {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                CharSequence before = ic.getTextBeforeCursor(1000, 0);
                int beforeLen = before != null ? before.length() : 0;
                if (beforeLen < 1000) {
                    ic.setSelection(beforeLen, beforeLen + 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Select all text
     */
    private void selectAll() {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                CharSequence before = ic.getTextBeforeCursor(10000, 0);
                CharSequence after = ic.getTextAfterCursor(10000, 0);
                int total = (before != null ? before.length() : 0) + (after != null ? after.length() : 0);
                ic.setSelection(0, total);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cut selected text
     */
    private void cutText() {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                CharSequence selected = ic.getSelectedText(0);
                if (selected != null && selected.length() > 0) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("text", selected);
                        clipboard.setPrimaryClip(clip);
                        ic.commitText("", 1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Copy selected text
     */
    private void copyText() {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                CharSequence selected = ic.getSelectedText(0);
                if (selected != null && selected.length() > 0) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("text", selected);
                        clipboard.setPrimaryClip(clip);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Paste text from clipboard
     */
    private void pasteText() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            CharSequence text = item.getText();
            if (text != null) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(text, 1);
                }
            }
        }
    }

    /**
     * Update keyboard theme based on system settings
     */
    private void updateKeyboardTheme() {
        try {
            if (keyboardView != null) {
                boolean isDarkMode = languageManager.isDarkMode(this);
                int bgColor = isDarkMode ? 
                        getResources().getColor(R.color.keyboardBackgroundDark) :
                        getResources().getColor(R.color.keyboardBackground);
                keyboardView.setBackgroundColor(bgColor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Gesture and event handlers
    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    @Override public void onText(CharSequence text) {}
    
    @Override 
    public void swipeLeft() { 
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.deleteSurroundingText(50, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override 
    public void swipeRight() { 
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(" ", 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override 
    public void swipeDown() { 
        requestHideSelf(0);
    }
    
    @Override 
    public void swipeUp() { 
        showEmojiPicker();
    }
}
