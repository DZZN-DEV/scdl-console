package com.chaquo.python.console;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.chaquo.python.utils.PythonConsoleActivity;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends PythonConsoleActivity {

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
        errorMessage += "Stack trace: " + Log.getStackTraceString(e);

        String filename = "error_log.txt";
        String filepath = getFilesDir().getPath() + "/" + filename;
        try {
            FileWriter writer = new FileWriter(filepath);
            writer.write(errorMessage);
            writer.close();
        } catch (IOException ex) {
            Log.e("Error", "Failed to write error log to file", ex);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        System.exit(0);
    }
}
