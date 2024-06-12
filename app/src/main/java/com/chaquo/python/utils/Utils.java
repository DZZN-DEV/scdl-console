package com.chaquo.python.utils;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static int resId(Context context, String type, String name) {
        Resources resources = context.getResources();
        return resources.getIdentifier(name, type, context.getApplicationInfo().packageName);
    }

    // Methode zur Validierung der SoundCloud-URL
    public static boolean isValidURL(String url) {
        String regex = "^(https?:\\/\\/)?(www\\.)?(soundcloud\\.com)\\/([a-zA-Z0-9_-]+)\\/([a-zA-Z0-9_-]+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    // Methode, um den Pfad aus einer URI zu erhalten
    public static String getPathFromUri(Context context, Uri uri) {
        String path = null;
        String[] projection = { MediaStore.MediaColumns.DATA };

        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            path = cursor.getString(column_index);
            cursor.close();
        }

        return path;
    }
}
