package com.fisify.app.security.checkers;

import java.io.File;

public class XposedChecker {

    public boolean isXposedActive() {
        return checkXposedFiles() || checkXposedStack();
    }

    private boolean checkXposedFiles() {
        String[] xposedPaths = {
                "/system/framework/XposedBridge.jar",
                "/system/lib/libxposed_art.so",
                "/system/lib64/libxposed_art.so",
                "/system/xposed.prop"
        };

        for (String path : xposedPaths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }

    private boolean checkXposedStack() {
        try {
            throw new Exception("StackCheck");
        } catch (Exception e) {
            for (StackTraceElement element : e.getStackTrace()) {
                if (element.getClassName().contains("de.robv.android.xposed") ||
                        element.getClassName().contains("xposed")) {
                    return true;
                }
            }
        }
        return false;
    }
}