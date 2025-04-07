package com.fisify.app.security;

import com.fisify.app.BuildConfig;

import java.io.IOException;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SSLPinning {
    private static final String WEBVIEW_HOST = BuildConfig.WEBVIEW_HOST;
    private static final String EXPECTED_WEBVIEW_PIN = BuildConfig.EXPECTED_WEBVIEW_PIN;

    public static void main(String[] args) {
        makeRequestWithPinning();
    }

    public static void makeRequestWithPinning() {
        String hostname = WEBVIEW_HOST;
        String sha256Fingerprint = "sha256/" + EXPECTED_WEBVIEW_PIN;

        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                .add(hostname, sha256Fingerprint)
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .certificatePinner(certificatePinner)
                .build();

        Request request = new Request.Builder()
                .url("https://" + hostname)
                .build();
        try {
            Response response = client.newCall(request).execute();
            System.out.println("Pinning OK");
        } catch (IOException e) {
            System.out.println("Pinning FAILED: " + e.getMessage());
        }
    }
}
