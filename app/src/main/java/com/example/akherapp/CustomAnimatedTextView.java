package com.example.akherapp;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatTextView;

public class CustomAnimatedTextView extends AppCompatTextView {
    private String fullText = "";
    private int currentIndex = 0;
    private long animationDelay = 50; // Délai entre chaque caractère (ms)
    private Handler handler;
    private boolean isAnimating = false;

    public CustomAnimatedTextView(Context context) {
        super(context);
        init();
    }

    public CustomAnimatedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomAnimatedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        handler = new Handler();
    }

    public void animateText(String text) {
        if (isAnimating) {
            stopAnimation();
        }
        
        fullText = text;
        currentIndex = 0;
        setText("");
        isAnimating = true;
        
        // Démarrer l'animation
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentIndex <= fullText.length()) {
                    setText(fullText.substring(0, currentIndex));
                    currentIndex++;
                    
                    if (currentIndex <= fullText.length()) {
                        handler.postDelayed(this, animationDelay);
                    } else {
                        isAnimating = false;
                    }
                }
            }
        });
    }

    public void animateTextWordByWord(String text) {
        if (isAnimating) {
            stopAnimation();
        }

        fullText = text;
        String[] words = text.split("\\s+");  // Better word splitting
        currentIndex = 0;
        setText("");
        isAnimating = true;

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentIndex < words.length) {
                    StringBuilder displayText = new StringBuilder();
                    for (int i = 0; i <= currentIndex; i++) {
                        displayText.append(words[i]).append(" ");
                    }
                    setText(displayText.toString().trim());
                    currentIndex++;

                    if (currentIndex < words.length) {
                        handler.postDelayed(this, animationDelay * 3); // Slightly faster animation
                    } else {
                        isAnimating = false;
                    }
                }
            }
        });
    }

    public void stopAnimation() {
        handler.removeCallbacksAndMessages(null);
        isAnimating = false;
        setText(fullText);
    }

    public void setAnimationDelay(long delay) {
        this.animationDelay = delay;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}