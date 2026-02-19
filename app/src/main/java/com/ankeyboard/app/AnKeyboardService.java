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
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import java.util.List;

public class AnKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private Keyboard selectionKeyboard;
    private LinearLayout candidateLayout;
    
    private LearningDictionary brain;
    
    private boolean isCaps = false;
    private boolean isSelectionMode = false;
    private StringBuilder composing = new StringBuilder();

    @Override
    public void onCreate() {
        super.onCreate();
        brain = new LearningDictionary(this);
        seedInitialData();
    }
    
    private void seedInitialData() {
        if (brain.getPredictions("a").size() < 2) {
            brain.learnWord("AnKeyboard");
            brain.learnWord("Halo");
            brain.learnWord("Apa");
            brain.learnWord("Kabar");
            brain.learnWord("Selamat");
            brain.learnWord("Malam");
        }
    }

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        keyboard = new Keyboard(this, R.xml.qwerty);
        selectionKeyboard = new Keyboard(this, R.xml.selection);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(false);
        updateKeyboardTheme();
        return keyboardView;
    }

    @Override
    public View onCreateCandidatesView() {
        View v = getLayoutInflater().inflate(R.layout.candidate_view, null);
        candidateLayout = v.findViewById(R.id.candidate_layout);
        return v;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                handleBackspace(ic);
                break;
                
            case Keyboard.KEYCODE_SHIFT:
                isCaps = !isCaps;
                keyboard.setShifted(isCaps);
                keyboardView.invalidateAllKeys();
                break;
                
            case Keyboard.KEYCODE_DONE: 
            case 10:
                commitAndLearn(ic, "\n");
                break;
                
            case 32: // Spasi
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
    }

    private void commitAndLearn(InputConnection ic, String separator) {
        if (composing.length() > 0) {
            String wordTyped = composing.toString();
            
            ic.commitText(wordTyped, 1);
            
            brain.learnWord(wordTyped);
            
            composing.setLength(0);
        }
        
        ic.commitText(separator, 1);
        updateCandidates();
    }

    private void handleBackspace(InputConnection ic) {
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
    }

    private void updateCandidates() {
        if (candidateLayout == null) return;
        candidateLayout.removeAllViews();

        if (composing.length() > 0) {
            setCandidatesViewShown(true);
            
            List<String> suggestions = brain.getPredictions(composing.toString());

            // First suggestion as autocorrect
            if (!suggestions.isEmpty()) {
                String autocorrect = suggestions.get(0);
                if (!autocorrect.equalsIgnoreCase(composing.toString())) {
                    Button btn = new Button(this);
                    btn.setText(autocorrect + " (Auto)");
                    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    btn.setTypeface(null, Typeface.BOLD);
                    btn.setBackgroundColor(Color.parseColor("#E8F5E8"));
                    btn.setPadding(30, 0, 30, 0);
                    btn.setAllCaps(false);
                    
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pickAutoCorrect(autocorrect);
                        }
                    });
                    candidateLayout.addView(btn);
                }
            }

            // Other suggestions
            for (int i = 1; i < Math.min(6, suggestions.size()); i++) {
                final String suggestion = suggestions.get(i);
                if (!suggestion.equalsIgnoreCase(composing.toString())) {
                    Button btn = new Button(this);
                    btn.setText(suggestion);
                    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    btn.setTypeface(null, Typeface.NORMAL);
                    btn.setBackgroundColor(Color.TRANSPARENT);
                    btn.setPadding(30, 0, 30, 0);
                    btn.setAllCaps(false);
                    
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pickSuggestion(suggestion);
                        }
                    });
                    candidateLayout.addView(btn);
                }
            }
        } else {
            setCandidatesViewShown(false);
        }
    }

    private void pickSuggestion(String suggestion) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(suggestion + " ", 1);
            
            brain.learnWord(suggestion);
            
            composing.setLength(0);
            updateCandidates();
        }
    }

    private void pickAutoCorrect(String autocorrect) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.setComposingText(autocorrect, 1);
            composing.setLength(0);
            composing.append(autocorrect);
            updateCandidates();
        }
    }

    private void showEmojiPicker() {
        Dialog dialog = new Dialog(this);
        dialog.setTitle("Pilih Emoji");
        
        GridView gridView = new GridView(this);
        String[] emojis = {"😀", "😂", "😊", "😍", "🥰", "😘", "😉", "😎", "🤔", "😢", "😭", "😤", "😡", "🥺", "😴", "🤤", "🤗", "🤭", "🤫", "🤥", "😐", "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "🤐", "😯", "😪", "😫", "😴", "😌", "😛", "😜", "😝", "🤤", "🤗", "🤭", "🤫", "🤥", "😐", "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "🤐", "😯", "😪", "😫", "😴", "😌", "😛", "😜", "😝", "🤤", "🤗", "🤭", "🤫", "🤥", "😐", "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "🤐", "😯", "😪", "😫", "😴", "😌", "😛", "😜", "😝", "🤤", "🤗", "🤭", "🤫", "🤥", "😐", "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "🤐", "😯", "😪", "😫", "😴", "😌"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, emojis);
        gridView.setAdapter(adapter);
        gridView.setNumColumns(10);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(emojis[position], 1);
                }
                dialog.dismiss();
            }
        });
        
        dialog.setContentView(gridView);
        dialog.show();
    }

    private void toggleSelectionMode() {
        isSelectionMode = !isSelectionMode;
        if (isSelectionMode) {
            keyboardView.setKeyboard(selectionKeyboard);
            startSelection();
        } else {
            keyboardView.setKeyboard(keyboard);
        }
        keyboardView.invalidateAllKeys();
    }

    private void startSelection() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            // Get current selection
            CharSequence selected = ic.getSelectedText(0);
            if (selected == null || selected.length() == 0) {
                // No selection, start at cursor position
                CharSequence before = ic.getTextBeforeCursor(1000, 0);
                int cursorPos = before.length();
                if (cursorPos > 0) {
                    ic.setSelection(cursorPos - 1, cursorPos);
                }
            }
        }
    }

    private void expandSelectionLeft() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence before = ic.getTextBeforeCursor(1000, 0);
            CharSequence selected = ic.getSelectedText(0);
            CharSequence after = ic.getTextAfterCursor(1000, 0);
            
            int start = before.length() - (selected != null ? selected.length() : 0);
            int end = before.length() + (after != null ? after.length() : 0);
            
            if (start > 0) {
                ic.setSelection(start - 1, end);
            }
        }
    }

    private void expandSelectionRight() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence before = ic.getTextBeforeCursor(1000, 0);
            CharSequence selected = ic.getSelectedText(0);
            CharSequence after = ic.getTextAfterCursor(1000, 0);
            
            int start = before.length() - (selected != null ? selected.length() : 0);
            int end = before.length() + (after != null ? after.length() : 0);
            
            if (end < start + (selected != null ? selected.length() : 0) + (after != null ? after.length() : 0)) {
                ic.setSelection(start, end + 1);
            }
        }
    }

    private void selectAll() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence before = ic.getTextBeforeCursor(1000, 0);
            CharSequence after = ic.getTextAfterCursor(1000, 0);
            ic.setSelection(0, before.length() + after.length());
        }
    }

    private void cutText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence selected = ic.getSelectedText(0);
            if (selected != null && selected.length() > 0) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text", selected);
                clipboard.setPrimaryClip(clip);
                ic.commitText("", 1);
            }
        }
    }

    private void copyText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence selected = ic.getSelectedText(0);
            if (selected != null && selected.length() > 0) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text", selected);
                clipboard.setPrimaryClip(clip);
            }
        }
    }

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

    private void updateKeyboardTheme() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        if (isDarkMode) {
            keyboardView.setBackgroundColor(getResources().getColor(R.color.keyboardBackgroundDark));
            // For simplicity, we'll keep the same key background, but in a real app you'd switch drawable
        } else {
            keyboardView.setBackgroundColor(getResources().getColor(R.color.keyboardBackground));
        }
    }

    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    @Override public void onText(CharSequence text) {}
    @Override public void swipeLeft() { 
        // Delete word
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.deleteSurroundingText(50, 0); // Rough delete word
        }
    }
    @Override public void swipeRight() { 
        // Space
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(" ", 1);
        }
    }
    @Override public void swipeDown() { 
        // Hide keyboard
        requestHideSelf(0);
    }
    @Override public void swipeUp() { 
        // Show emoji
        showEmojiPicker();
    }
}
