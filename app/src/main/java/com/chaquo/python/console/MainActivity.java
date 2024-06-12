package com.chaquo.python.console;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.utils.PythonConsoleActivity;
import com.chaquo.python.utils.Utils;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends PythonConsoleActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private EditText urlInput;
    private TextView tvOutput;
    private ScrollView svOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);

        urlInput = findViewById(R.id.url_input);
        tvOutput = findViewById(R.id.tvOutput);
        svOutput = findViewById(R.id.svOutput);
        Button executeButton = findViewById(R.id.execute_button);

        executeButton.setOnClickListener(v -> executeDownload());

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void executeDownload() {
        String url = urlInput.getText().toString().trim();
        if (!Utils.isValidURL(url)) {
            Toast.makeText(this, "Please enter a valid SoundCloud URL", Toast.LENGTH_SHORT).show();
            return;
        }

        String maxTracks = "5"; // For example purposes, this could be dynamic
        ll
        String downloadFavorites = "true"; // For example purposes, this could be dynamic
        // String authToken = "your_auth_token"; Replace with actual token if needed
        String downloadPath = Utils.getInternalStoragePath(this); // Use internal storage path

        new Thread(() -> {
            try {
                Python py = Python.getInstance();
                PyObject mainModule = py.getModule("main");
                PyObject result = mainModule.callAttr("main", url, maxTracks, downloadFavorites, authToken, downloadPath);
                runOnUiThread(() -> handleResult(result));
            } catch (Exception e) {
                Log.e("MainActivity", "Error calling Python script", e);
                runOnUiThread(() -> Toast.makeText(this, "Error calling Python script: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void handleResult(PyObject result) {
        try {
            JSONObject jsonResult = new JSONObject(result.toString());
            boolean success = jsonResult.getBoolean("success");
            if (success) {
                Toast.makeText(this, "Download successful", Toast.LENGTH_LONG).show();
                tvOutput.setText("Download successful");
            } else {
                String error = jsonResult.getString("error");
                Toast.makeText(this, "Download failed: " + error, Toast.LENGTH_LONG).show();
                tvOutput.setText("Download failed: " + error);
            }
        } catch (JSONException e) {
            Log.e("MainActivity", "Error parsing JSON result", e);
            Toast.makeText(this, "Error parsing result: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        svOutput.fullScroll(ScrollView.FOCUS_DOWN);
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
        public Task(Application app) {
            super(app);
        }

        @Override
        public void run() {
            py.getModule("main").callAttr("main");
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> handleUncaughtException(t, e));
    }

    private void handleUncaughtException(Thread t, Throwable e) {
        String errorMessage = "Error: " + e.getMessage() + "\n";
        errorMessage += "Stack trace: " + Log.getStackTraceString(e);

        String filename = "error_log.txt";
        String filepath = getFilesDir().getPath() + "/" + filename;
        try (FileWriter writer = new FileWriter(filepath)) {
            writer.write(errorMessage);
        } catch (IOException ex) {
            Log.e("Error", "Failed to write error log to file", ex);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        System.exit(0);
    }
}
