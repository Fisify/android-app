package com.fisify.app.security;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import com.fisify.app.security.checkers.FridaChecker;
import com.fisify.app.security.checkers.XposedChecker;

public class InstrumentationDetector {
    private static final int TIMEOUT_MS = 1500;

    public static void performSecurityChecks(Context context) {
        new Thread(() -> {
            long startTime = System.currentTimeMillis();

            if (quickChecks() || deepChecks()) {
                ((Activity)context).runOnUiThread(() -> showSecurityDialog(context));
            }

            Log.d("Security", "Scan completed in: " +
                    (System.currentTimeMillis() - startTime) + "ms");
        }).start();
    }

    // Chequeos rápidos (archivos, debugger)
    private static boolean quickChecks() {
        return new XposedChecker().isXposedActive() ||
                android.os.Debug.isDebuggerConnected();
    }

    // Chequeos costosos (puertos)
    private static boolean deepChecks() {
        FridaChecker fridaChecker = new FridaChecker();
        return fridaChecker.checkFridaFiles() ||
                fridaChecker.checkFridaPort();
    }

    private static void showSecurityDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Security Alert")
                .setMessage("This app cannot run in modified environments")
                .setCancelable(false)
                .setPositiveButton("Exit", (dialog, which) -> {
                    System.exit(0);
                })
                .show();
    }
}
