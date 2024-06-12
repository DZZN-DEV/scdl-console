package com.chaquo.python.utils;

import android.content.Context;
import android.content.res.Resources;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    /** Make this package easy to copy to other apps by avoiding direct "R" references. (It
     * would be better to do this by distributing it along with its resources in an AAR, but
     * Chaquopy doesn't support getting Python code from an AAR yet.) */
    public static int resId(Context context, String type, String name) {
        Resources resources = context.getResources();
        return resources.getIdentifier(name, type, context.getApplicationInfo().packageName);
    }

    // Methode zur Validierung der SoundCloud-URL
    public static boolean isValidURL(String url) {  // Methode umbenannt und angepasst
        String regex = "^(https?:\\/\\/)?(www\\.)?(soundcloud\\.com)\\/([a-zA-Z0-9_-]+)\\/([a-zA-Z0-9_-]+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    // Methode, um den internen Speicherpfad für scdl zu erhalten
    public static String getInternalStoragePath(Context context) {
        File file = new File(context.getFilesDir(), "scdl");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }
}
