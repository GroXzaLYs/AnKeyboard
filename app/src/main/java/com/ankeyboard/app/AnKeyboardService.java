package com.ankeyboard.app;

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
    private LinearLayout candidateLayout;
    
    private LearningDictionary brain;
    
    private boolean isCaps = false;
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
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(false); 
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

            for (final String suggestion : suggestions) {
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

    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    @Override public void onText(CharSequence text) {}
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeDown() {}
    @Override public void swipeUp() {}
}
