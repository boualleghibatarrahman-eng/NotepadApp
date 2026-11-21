package com.example.notepadapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private EditText fileNameTxt, noteTxt, searchTxt;
    private TextView wordCountTxt, searchResultTxt;
    private MaterialButton saveBtn, openBtn, searchBtn, textSizeBtn, clearHighlightBtn;
    private float currentTextSize = 18f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileNameTxt = findViewById(R.id.fileNameTxt);
        noteTxt = findViewById(R.id.noteTxt);
        searchTxt = findViewById(R.id.searchTxt);
        wordCountTxt = findViewById(R.id.wordCountTxt);
        searchResultTxt = findViewById(R.id.searchResultTxt);
        saveBtn = findViewById(R.id.saveBtn);
        openBtn = findViewById(R.id.openBtn);
        searchBtn = findViewById(R.id.searchBtn);
        textSizeBtn = findViewById(R.id.textSizeBtn);
        clearHighlightBtn = findViewById(R.id.clearHighlightBtn);

        noteTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateWordCount(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        saveBtn.setOnClickListener(v -> {
            String fileName = fileNameTxt.getText().toString().trim();
            String content = noteTxt.getText().toString();

            if (fileName.isEmpty() || content.isEmpty()) {
                Snackbar.make(v, R.string.msg_error_empty, Snackbar.LENGTH_SHORT).show();
                return;
            }

            try {
                FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE);
                fos.write(content.getBytes());
                fos.close();
                Snackbar.make(v, R.string.msg_saved, Snackbar.LENGTH_SHORT).setBackgroundTint(Color.parseColor("#4CAF50")).show();
            } catch (Exception e) {
                Snackbar.make(v, "Error saving file", Snackbar.LENGTH_SHORT).show();
            }
        });

        openBtn.setOnClickListener(v -> {
            String fileName = fileNameTxt.getText().toString().trim();
            if (fileName.isEmpty()) {
                Snackbar.make(v, R.string.msg_error_empty, Snackbar.LENGTH_SHORT).show();
                return;
            }

            try {
                FileInputStream fis = openFileInput(fileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                noteTxt.setText(sb.toString());
                Snackbar.make(v, "File opened!", Snackbar.LENGTH_SHORT).show();
            } catch (Exception e) {
                Snackbar.make(v, R.string.msg_file_not_found, Snackbar.LENGTH_SHORT).show();
            }
        });

        searchBtn.setOnClickListener(v -> {
            String searchWord = searchTxt.getText().toString().trim();
            String fullText = noteTxt.getText().toString();

            if (searchWord.isEmpty()) {
                Snackbar.make(v, R.string.msg_search_empty, Snackbar.LENGTH_SHORT).show();
                return;
            }
            highlightAndCount(searchWord, fullText);
        });

        clearHighlightBtn.setOnClickListener(v -> {
            String plainText = noteTxt.getText().toString();
            noteTxt.setText(plainText);
            searchResultTxt.setText(getString(R.string.search_count_prefix) + "0");
            Snackbar.make(v, R.string.msg_highlights_cleared, Snackbar.LENGTH_SHORT).show();
        });

        SharedPreferences prefs = getSharedPreferences("NotepadPrefs", MODE_PRIVATE);
        fileNameTxt.setText(prefs.getString("last_file_name", ""));
        noteTxt.setText(prefs.getString("last_text", ""));
        currentTextSize = prefs.getFloat("text_size", 18f);
        noteTxt.setTextSize(currentTextSize);

        textSizeBtn.setOnClickListener(v -> {
            currentTextSize += 2f;
            if (currentTextSize > 26f) {
                currentTextSize = 14f;
            }
            noteTxt.setTextSize(currentTextSize);
            Snackbar.make(v, "Size: " + currentTextSize + "sp", Snackbar.LENGTH_SHORT).show();
        });
    }

    private void updateWordCount(String text) {
        text = text.trim();
        if (text.isEmpty()) {
            wordCountTxt.setText(getString(R.string.word_count_prefix) + "0");
            return;
        }
        String[] words = text.split("\\s+");
        wordCountTxt.setText(getString(R.string.word_count_prefix) + words.length);
    }

    private void highlightAndCount(String word, String fullText) {
        SpannableString spannable = new SpannableString(fullText);
        int count = 0;

        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fullText);

        while (matcher.find()) {
            count++;
            spannable.setSpan(
                    new BackgroundColorSpan(Color.parseColor("#FFF3B0")),
                    matcher.start(),
                    matcher.end(),
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        noteTxt.setText(spannable);
        noteTxt.setSelection(noteTxt.getText().length());
        searchResultTxt.setText(getString(R.string.search_count_prefix) + count);

        if (count == 0) {
            Snackbar.make(noteTxt, "No occurrences found", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getSharedPreferences("NotepadPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_file_name", fileNameTxt.getText().toString().trim());
        editor.putString("last_text", noteTxt.getText().toString());
        editor.putFloat("text_size", currentTextSize);
        editor.apply();
    }
}