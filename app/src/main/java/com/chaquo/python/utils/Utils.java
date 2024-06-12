package com.chaquo.python.utils;

import android.content.*;
import android.content.res.*;

public class Utils {
    /** Make this package easy to copy to other apps by avoiding direct "R" references. (It
     * would be better to do this by distributing it along with its resources in an AAR, but
     * Chaquopy doesn't support getting Python code from an AAR yet.)*/
    public static int resId(Context context, String type, String name) {
        Resources resources = context.getResources();
        return resources.getIdentifier(name, type, context.getApplicationInfo().packageName);
    }
    // Method to validate the SoundCloud URL
    public static boolean isValidURL(String url) {
        String regex = "^(https?:\\/\\/)?(www\\.)?(soundcloud\\.com)\\/([a-zA-Z0-9_-]+)\\/([a-zA-Z0-9_-]+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    // Method to get the internal storage path for scdl
    public static String getInternalStoragePath(Context context) {
        File file = new File(context.getFilesDir(), "scdl");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }
}
