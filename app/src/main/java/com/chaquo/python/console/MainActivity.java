package com.chaquo.python.console;

import android.app.Application;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.chaquo.python.Python;
import com.chaquo.python.utils.PythonConsoleActivity;
import com.chaquo.python.utils.Utils;

public class MainActivity extends PythonConsoleActivity {

    private EditText urlInput;
    private TextView tvOutput;
    private ScrollView svOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // LinearLayout erstellen
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // URL-Eingabe erstellen
        urlInput = new EditText(this);
        urlInput.setHint("Enter SoundCloud URL");
        layout.addView(urlInput);

        // Button erstellen
        Button executeButton = new Button(this);
        executeButton.setText("Download");
        layout.addView(executeButton);

        // ScrollView und TextView erstellen
        svOutput = new ScrollView(this);
        tvOutput = new TextView(this);
        svOutput.addView(tvOutput);
        layout.addView(svOutput);

        // Layout setzen
        setContentView(layout);

        // Button-Klick-Ereignis behandeln
        executeButton.setOnClickListener(v -> executeDownload());
    }

    private void executeDownload() {
        String url = urlInput.getText().toString();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Utils.isValidUrl(url)) {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // Task ausführen
        Task task = new Task(getApplication());
        task.setUrl(url);
        task.run();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected Class<? extends Task> getTaskClass() {
        return Task.class;
    }

    public static class Task extends PythonConsoleActivity.Task {
        private String url;

        public Task(Application app) {
            super(app);
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            Python py = Python.getInstance();
            py.getModule("main").callAttr("download", url);
        }
    }
}
