package com.quickspend.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Display language only: canonical transaction and category values never change. */
public final class QsLanguage {
    private static volatile JSONObject hindi;
    private QsLanguage() { }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("qs_float", Context.MODE_PRIVATE); }
    public static String get(Context c) { return prefs(c).getString("language", "en"); }
    public static boolean chosen(Context c) { return prefs(c).contains("language"); }
    public static void set(Context c, String language) {
        if (!"hi".equals(language) && !"en".equals(language)) throw new IllegalArgumentException("Unsupported language");
        if (!prefs(c).edit().putString("language", language).commit()) throw new IllegalStateException("Could not save language");
    }
    public static String colorMode(Context c) {
        String mode=prefs(c).getString("colorMode","dark");
        return "light".equals(mode)||"matte".equals(mode)?mode:"dark";
    }
    public static boolean categoryPicker(Context c){return prefs(c).getBoolean("categoryPicker",false);}
    public static void setCategoryPicker(Context c,boolean enabled){
        if(!prefs(c).edit().putBoolean("categoryPicker",enabled).commit())throw new IllegalStateException("Could not save category picker");
    }
    public static void setColorMode(Context c,String mode) {
        if(!"dark".equals(mode)&&!"light".equals(mode)&&!"matte".equals(mode)) throw new IllegalArgumentException("Unsupported color mode");
        if(!prefs(c).edit().putString("colorMode",mode).commit()) throw new IllegalStateException("Could not save color mode");
    }
    public static void applySystemBars(android.app.Activity activity) {
        boolean light="light".equals(colorMode(activity));
        androidx.core.view.WindowInsetsControllerCompat bars=new androidx.core.view.WindowInsetsControllerCompat(activity.getWindow(),activity.getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(light);bars.setAppearanceLightNavigationBars(light);
    }
    public static String tr(Context c, String text) {
        if (text == null || !"hi".equals(get(c))) return text;
        try {
            if (hindi == null) synchronized (QsLanguage.class) {
                if (hindi == null) try (InputStream input=c.getAssets().open("public/locales/hi.json"); ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
                    byte[] buffer=new byte[4096]; int n;
                    while ((n=input.read(buffer)) != -1) bytes.write(buffer,0,n);
                    hindi=new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                }
            }
            return hindi.optString(text,text);
        } catch (Exception e) { return text; }
    }
}
