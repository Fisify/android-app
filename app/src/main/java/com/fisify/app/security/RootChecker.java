package com.fisify.app.security;

import java.io.File;
import java.io.IOException;

public class RootChecker {
    public static boolean isDeviceRooted() {
        String[] paths = {
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
        };

        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }

        String buildTags = android.os.Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        try {
            Process process = Runtime.getRuntime().exec("su");
            process.destroy();
            return true;
        } catch (IOException e) {
            // No se pudo ejecutar 'su'
        }

        return false;
    }
}
