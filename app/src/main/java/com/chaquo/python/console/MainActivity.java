package com.chaquo.python.console;

import android.app.*;
import com.chaquo.python.utils.*;
import android.os.Environment;
import java.io.*;

public class MainActivity extends PythonConsoleActivity {

    // On API level 31 and higher, pressing Back in a launcher activity sends it to the back by
    // default, but that would make it difficult to restart the activity.
    @Override public void onBackPressed() {
        finish();
    }

    @Override protected Class<? extends Task> getTaskClass() {
        return Task.class;
    }

    public static class Task extends PythonConsoleActivity.Task {
        public Task(Application app) {
            super(app);
        }

        @Override public void run() {
            py.getModule("main").callAttr("main");
        }
    }

    // Perfect crash handler
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                handleUncaughtException(t, e);
            }
        });
    }

    private void handleUncaughtException(Thread t, Throwable e) {
        String errorMessage = "Error: " + e.getMessage() + "\n";
        errorMessage += "Stack trace: " + android.util.Log.getStackTraceString(e);

        // Save error message to internal storage as a .txt file
        String filename = "error_log.txt";
        String filepath = getFilesDir().getPath() + "/" + filename;
        try {
            FileWriter writer = new FileWriter(filepath);
            writer.write(errorMessage);
            writer.close();
        } catch (IOException ex) {
            // Handle file writing error
            Log.e("Error", "Failed to write error log to file", ex);
        }

        // Restart the app
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        System.exit(0);
    }
}
