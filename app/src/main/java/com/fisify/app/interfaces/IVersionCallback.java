package com.fisify.app.interfaces;

import com.android.volley.VolleyError;

public interface IVersionCallback {
    void onSuccess(String version);
    void onError(VolleyError error);
}
