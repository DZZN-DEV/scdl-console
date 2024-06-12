package com.chaquo.python.console;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.utils.PythonConsoleActivity;
import com.chaquo.python.utils.Utils;

import java.io.File;

public class MainActivity extends PythonConsoleActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "DownloadPrefs";
    private static final String PREF_DOWNLOAD_PATH = "download_path";

    private EditText urlInput;
    private TextView tvOutput;
    private ScrollView svOutput;
    private Uri downloadPathUri;

    private final ActivityResultLauncher<Intent> directoryPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    downloadPathUri = result.getData().getData();
                    if (downloadPathUri != null) {
                        getContentResolver().takePersistableUriPermission(downloadPathUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        saveDownloadPath(downloadPathUri);
                    }
                }
            });

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

        // Button zum Herunterladen erstellen
        Button executeButton = new Button(this);
        executeButton.setText("Download");
        layout.addView(executeButton);

        // Button zum Auswählen des Download-Pfads erstellen
        Button selectPathButton = new Button(this);
        selectPathButton.setText("Select Download Path");
        layout.addView(selectPathButton);

        // ScrollView und TextView erstellen
        svOutput = new ScrollView(this);
        tvOutput = new TextView(this);
        svOutput.addView(tvOutput);
        layout.addView(svOutput);

        // Layout setzen
        setContentView(layout);

        // Button-Klick-Ereignisse behandeln
        executeButton.setOnClickListener(v -> executeDownload());
        selectPathButton.setOnClickListener(v -> openDirectoryPicker());

        // Gespeicherten Download-Pfad laden
        downloadPathUri = loadDownloadPath();

        // Berechtigungen überprüfen und anfordern
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "All files access granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "All files access denied", Toast.LENGTH_SHORT).show();
                }
            }
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

        if (downloadPathUri == null) {
            Toast.makeText(this, "Please select a download path", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                Python py = Python.getInstance();
                PyObject pyObject = py.getModule("main");
                String downloadPath = Utils.getPathFromUri(this, downloadPathUri);
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

    private void openDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        directoryPickerLauncher.launch(intent);
    }

    private void saveDownloadPath(Uri uri) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_DOWNLOAD_PATH, uri.toString());
        editor.apply();
    }

    private Uri loadDownloadPath() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = preferences.getString(PREF_DOWNLOAD_PATH, null);
        return uriString != null ? Uri.parse(uriString) : null;
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
