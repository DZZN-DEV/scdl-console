package com.chaquo.python.console;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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
import com.chaquo.python.utils.PermissionsUtils;
import com.chaquo.python.utils.PythonConsoleActivity;
import com.chaquo.python.utils.Utils;
import com.chaquo.python.utils.Permissions;

import java.io.File;

public class MainActivity extends PythonConsoleActivity {

    private static final String PREFS_NAME = "DownloadPrefs";
    private static final String PREF_DOWNLOAD_PATH = "download_path";

    private EditText urlInput;
    private TextView tvOutput;
    private TextView tvDownloadPath;
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
                        displayDownloadPath(downloadPathUri);
                    }
                } else {
                    Toast.makeText(this, "Failed to select directory. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Permissions.checkAndRequestPermissions(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        urlInput = new EditText(this);
        urlInput.setHint("Enter SoundCloud URL");
        layout.addView(urlInput);

        Button executeButton = new Button(this);
        executeButton.setText("Download");
        layout.addView(executeButton);

        Button selectPathButton = new Button(this);
        selectPathButton.setText("Select Download Path");
        layout.addView(selectPathButton);

        tvDownloadPath = new TextView(this);
        layout.addView(tvDownloadPath);

        svOutput = new ScrollView(this);
        tvOutput = new TextView(this);
        svOutput.addView(tvOutput);
        layout.addView(svOutput);

        setContentView(layout);

        executeButton.setOnClickListener(v -> executeDownload());
        selectPathButton.setOnClickListener(v -> openDirectoryPicker());

        downloadPathUri = loadDownloadPath();
        if (downloadPathUri != null) {
            displayDownloadPath(downloadPathUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsUtils.handlePermissionsResult(requestCode, grantResults, this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PermissionsUtils.handleActivityResult(requestCode, this);
    }

    private void executeDownload() {
        String url = urlInput.getText().toString();
        if (url.isEmpty()) {
            url = "https://soundcloud.com/r2rmoe/angelic";  // Default SoundCloud URL
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
                String downloadPath = getPathFromUri(downloadPathUri);
                if (downloadPath == null) {
                    throw new IllegalArgumentException("Invalid download path");
                }
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
        try {
            directoryPickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening directory picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

    private String getPathFromUri(Uri uri) {
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            String[] split = docId.split(":");
            String type = split[0];
            String path = split.length > 1 ? split[1] : "";

            if ("primary".equalsIgnoreCase(type)) {
                return Environment.getExternalStorageDirectory() + "/" + path;
            } else {
                return "/storage/" + type + "/" + path;
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(this, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private void displayDownloadPath(Uri uri) {
        String path = getPathFromUri(uri);
        tvDownloadPath.setText("Download Path: " + (path != null ? path : "Invalid Path"));
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
