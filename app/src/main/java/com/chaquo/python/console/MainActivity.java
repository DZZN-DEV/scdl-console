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

    private EditText urlInput;
    private TextView tvOutput;
    private ScrollView svOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Permissions.checkAndRequestPermissions(this);

        // Create LinearLayout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Create URL input
        urlInput = new EditText(this);
        urlInput.setHint("Enter SoundCloud URL");
        layout.addView(urlInput);

        // Create download button
        Button executeButton = new Button(this);
        executeButton.setText("Download");
        layout.addView(executeButton);

        // Create ScrollView and TextView for output
        svOutput = new ScrollView(this);
        tvOutput = new TextView(this);
        svOutput.addView(tvOutput);
        layout.addView(svOutput);

        // Set the layout
        setContentView(layout);

        // Handle button click events
        executeButton.setOnClickListener(v -> executeDownload());

        // Set download path
        String downloadPath = getFilesDir().getAbsolutePath() + "/SCDL";
        File downloadDirectory = new File(downloadPath);
        if (!downloadDirectory.exists()) {
            downloadDirectory.mkdirs();
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

        String downloadPath = getFilesDir().getAbsolutePath() + "/SCDL";
        new Thread(() -> {
            try {
                Python py = Python.getInstance();
                PyObject pyObject = py.getModule("main");
                PyObject result = pyObject.callAttr("download", url, null, false, null, downloadPath);

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
    public void onBackPressed() {
        finish();
    }

    @Override
    protected Class<? extends Task> getTaskClass() {
        return Task.class;
    }

    public static class Task extends PythonConsoleActivity.Task {
        private String url;
        private String downloadPath;

        public Task(Application app) {
            super(app);
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setDownloadPath(String downloadPath) {
            this.downloadPath = downloadPath;
        }

        @Override
        public void run() {
            Python py = Python.getInstance();
            py.getModule("main").callAttr("download", url, null, false, null, downloadPath);
        }
    }
}




    }

    @Override
    protected Class<? extends Task> getTaskClass() {
        return Task.class;
    }
    
    public static class Task extends PythonConsoleActivity.Task {
        private String url;
        private String downloadPath;

        public Task(Application app) {
            super(app);
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setDownloadPath(String downloadPath) {
            this.downloadPath = downloadPath;
        }

        @Override
        public void run() {
            Python py = Python.getInstance();
            py.getModule("main").callAttr("download", url, null, false, null, downloadPath);
        }
    }
}

