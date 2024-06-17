package com.chaquo.python.console;

import android.content.ContentResolver;
import android.os.Environment;
import android.content.ContentUris;
import android.app.Application;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import androidx.core.content.ContextCompat;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.utils.PythonConsoleActivity;
import com.chaquo.python.utils.Utils;

public class MainActivity extends PythonConsoleActivity {

    private static final String PREFS_NAME = "DownloadPrefs";
    private static final String PREF_DOWNLOAD_PATH = "download_path";

    private EditText urlInput;
    private TextView tvOutput;
    private ScrollView svOutput;
    private String downloadPath;

    private final ActivityResultLauncher<Intent> directoryPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri downloadPathUri = null;

                    if (result.getData() != null) {
                        // Check for single or multiple URIs
                        if (result.getData().hasClipData()) {
                            ClipData clipData = result.getData().getClipData();
                            if (clipData.getItemCount() == 1) {
                                downloadPathUri = clipData.getItemAt(0).getUri();
                            }
                        } else {
                            downloadPathUri = result.getData().getData();
                        }
                    }

                    // Handle SAF URI or regular path
                    if (downloadPathUri != null) {
                        if (isUriRequiresTakePersistableUriPermission(downloadPathUri)) {
                            takePersistableUriPermission(downloadPathUri, Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        }
                        downloadPath = getRealPathFromURI(downloadPathUri);
                        if (downloadPath != null) {
                            saveDownloadPath(downloadPath);
                        } else {
                            Toast.makeText(this, "Failed to get real path from URI", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Invalid download path", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        // Create select path button
        Button selectPathButton = new Button(this);
        selectPathButton.setText("Select Download Path");
        layout.addView(selectPathButton);

        // Create ScrollView and TextView
        svOutput = new ScrollView(this);
        tvOutput = new TextView(this);
        svOutput.addView(tvOutput);
        layout.addView(svOutput);

        // Set the layout
        setContentView(layout);

        // Handle button click events
        executeButton.setOnClickListener(v -> executeDownload());
        selectPathButton.setOnClickListener(v -> openDirectoryPicker());

        // Load saved download path
        downloadPath = loadDownloadPath();      
    }

    private void openDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        directoryPickerLauncher.launch(intent);
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
    
        if (downloadPath == null) {
            Toast.makeText(this, "Please select a download path", Toast.LENGTH_SHORT).show();
            return;
        }
    
        // Check if downloadPath is null
        if (downloadPath == null) {
            Toast.makeText(this, "Invalid download path", Toast.LENGTH_SHORT).show();
            return;
        }
    
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
    

    private void saveDownloadPath(String path) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_DOWNLOAD_PATH, path);
        editor.apply();
    }

    private String loadDownloadPath() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getString(PREF_DOWNLOAD_PATH, null);
    }

    private String getRealPathFromURI(Uri uri) {
        String realPath = null;

        if (DocumentsContract.isDocumentUri(this, uri)) {
            String documentId = DocumentsContract.getDocumentId(uri);
            if (uri.getAuthority().equals("com.android.externalstorage.documents")) {
                String[] split = documentId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    realPath = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (uri.getAuthority().equals("com.android.providers.downloads.documents")) {
                Uri contentUri = Uri.parse("content://downloads/public_downloads");
                Uri finalUri = ContentUris.withAppendedId(contentUri, Long.parseLong(documentId));
                realPath = getDataColumn(this, finalUri, null, null);
            } else if (uri.getAuthority().equals("com.android.providers.media.documents")) {
                String[] split = documentId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = "_id=?";
                String[] selectionArgs = new String[]{split[1]};
                realPath = getDataColumn(this, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            realPath = getDataColumn(this, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            realPath = uri.getPath();
        }

        return realPath;
    }

    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        String[] projection = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
    
    private boolean isUriRequiresTakePersistableUriPermission(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                Context context = getApplicationContext();
                PackageManager packageManager = context.getPackageManager();
                ResolveInfo resolveInfo = packageManager.resolveActivity(new Intent(Intent.ACTION_VIEW, uri), PackageManager.MATCH_ALL);
                if (resolveInfo != null && resolveInfo.activityInfo.packageName != null) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}


