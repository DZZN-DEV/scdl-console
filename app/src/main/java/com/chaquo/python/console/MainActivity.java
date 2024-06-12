package com.chaquo.python.console;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.utils.PythonConsoleActivity;
import com.chaquo.python.utils.Utils;

import java.io.File;

public class MainActivity extends PythonConsoleActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private EditText urlInput;
    private TextView tvOutput;
    private ScrollView svOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Berechtigungen überprüfen und anfordern
        checkAndRequestPermissions();

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

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void executeDownload() {
        String url = urlInput.getText().toString();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Utils.isValidURL(url)) {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                Python py = Python.getInstance();
                PyObject pyObject = py.getModule("scdl_downloader");  // Das Python-Skript importieren
                String downloadPath = Utils.getInternalStoragePath(this);
                PyObject result = pyObject.callAttr("download", url, null, false, null, downloadPath); // Die Methode mit Argumenten aufrufen

                runOnUiThread(() -> {
                    tvOutput.setText(result.toString());
                    svOutput.fullScroll(ScrollView.FOCUS_DOWN);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvOutput.setText("Error: " + e.getMessage());
                    svOutput.fullScroll(ScrollView.FOCUS_DOWN);
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                createDownloadFolder();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createDownloadFolder() {
        File downloadFolder = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "scdl");
        if (!downloadFolder.exists()) {
            boolean success = downloadFolder.mkdirs();
            if (!success) {
                Toast.makeText(this, "Failed to create download folder", Toast.LENGTH_SHORT).show();
            }
        }
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
            String downloadPath = Utils.getInternalStoragePath(getApplication());
            py.getModule("scdl_downloader").callAttr("download", url, null, false, null, downloadPath);
        }
    }
}
