package com.chtj.base_framework;

import android.content.Context;
import android.net.Uri;
import android.provider.Settings;

public class FSettingsTools {
    public static class Secure {
        public static void setIntValue(Context context, String key, int value) {
            Settings.Secure.putInt(context.getContentResolver(), key, value);
        }

        public static void setLongValue(Context context, String key, long value) {
            Settings.Secure.putLong(context.getContentResolver(), key, value);
        }

        public static void setFloatValue(Context context, String key, float value) {
            Settings.Secure.putFloat(context.getContentResolver(), key, value);
        }

        public static void setStringValue(Context context, String key, String value) {
            Settings.Secure.putString(context.getContentResolver(), key, value);
        }

        public int getIntValue(Context context, String key, int defaultValue) {
            return Settings.Secure.getInt(context.getContentResolver(), key, defaultValue);
        }

        public long getLongValue(Context context, String key, long defaultValue) {
            return Settings.Secure.getLong(context.getContentResolver(), key, defaultValue);
        }

        public float getFloatValue(Context context, String key, float defaultValue) {
            return Settings.Secure.getFloat(context.getContentResolver(), key, defaultValue);
        }

        public String getStringValue(Context context, String defaultValue) {
            return Settings.Secure.getString(context.getContentResolver(), defaultValue);
        }

        public Uri getUriValue(Uri uri, String name) {
            return Settings.Secure.getUriFor(uri, name);
        }

        public Uri getUriValue(String name) {
            return Settings.Secure.getUriFor(name);
        }
    }

    public static class Global {
        public static void setIntValue(Context context, String key, int value) {
            Settings.Global.putInt(context.getContentResolver(), key, value);
        }

        public static void setLongValue(Context context, String key, long value) {
            Settings.Global.putLong(context.getContentResolver(), key, value);
        }

        public static void setFloatValue(Context context, String key, float value) {
            Settings.Global.putFloat(context.getContentResolver(), key, value);
        }

        public static void setStringValue(Context context, String key, String value) {
            Settings.Global.putString(context.getContentResolver(), key, value);
        }

        public int getIntValue(Context context, String key, int defaultValue) {
            return Settings.Global.getInt(context.getContentResolver(), key, defaultValue);
        }

        public long getLongValue(Context context, String key, long defaultValue) {
            return Settings.Global.getLong(context.getContentResolver(), key, defaultValue);
        }

        public float getFloatValue(Context context, String key, float defaultValue) {
            return Settings.Global.getFloat(context.getContentResolver(), key, defaultValue);
        }

        public String getStringValue(Context context, String defaultValue) {
            return Settings.Global.getString(context.getContentResolver(), defaultValue);
        }

        public Uri getUriValue(Uri uri, String name) {
            return Settings.Global.getUriFor(uri, name);
        }

        public Uri getUriValue(String name) {
            return Settings.Global.getUriFor(name);
        }
    }

    public static class System {
        public static void setIntValue(Context context, String key, int value) {
            Settings.System.putInt(context.getContentResolver(), key, value);
        }

        public static void setLongValue(Context context, String key, long value) {
            Settings.System.putLong(context.getContentResolver(), key, value);
        }

        public static void setFloatValue(Context context, String key, float value) {
            Settings.System.putFloat(context.getContentResolver(), key, value);
        }

        public static void setStringValue(Context context, String key, String value) {
            Settings.System.putString(context.getContentResolver(), key, value);
        }

        public int getIntValue(Context context, String key, int defaultValue) {
            return Settings.System.getInt(context.getContentResolver(), key, defaultValue);
        }

        public long getLongValue(Context context, String key, long defaultValue) {
            return Settings.System.getLong(context.getContentResolver(), key, defaultValue);
        }

        public float getFloatValue(Context context, String key, float defaultValue) {
            return Settings.System.getFloat(context.getContentResolver(), key, defaultValue);
        }

        public String getStringValue(Context context, String defaultValue) {
            return Settings.System.getString(context.getContentResolver(), defaultValue);
        }

        public Uri getUriValue(Uri uri, String name) {
            return Settings.System.getUriFor(uri, name);
        }

        public Uri getUriValue(String name) {
            return Settings.System.getUriFor(name);
        }
    }

}
