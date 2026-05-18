package com.chtj.base_framework;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Screen related tools.
 */
public class FScreentTools {
    private static final String TAG = "ScreentShotUtil";

    private static final String CLASS_SURFACE_CONTROL = "android.view.SurfaceControl";
    private static final String CLASS_SURFACE = "android.view.Surface";
    private static final String METHOD_SCREENSHOT = "screenshot";
    private static final String SCREEN_CAP_BIN = "/system/bin/screencap";

    /**
     * Take a screenshot and save it under /sdcard.
     *
     * @return screenshot path, or empty string if failed
     */
    public static String takeScreenshot() {
        return takeScreenshot("");
    }

    /**
     * Take a screenshot and save it to the specified path.
     *
     * @param fileFullPath full output path, for example /sdcard/local/20201515.png
     * @return screenshot path, or empty string if failed
     */
    public static String takeScreenshot(String fileFullPath) {
        if (fileFullPath == null || fileFullPath.length() == 0) {
            fileFullPath = buildDefaultScreenshotPath();
        }

        File outFile = new File(fileFullPath);
        ensureParentDir(outFile);

        Bitmap bitmap = takeScreenshotBitmap();
        if (bitmap != null && saveBitmap2file(bitmap, outFile.getAbsolutePath())) {
            return outFile.getAbsolutePath();
        }

        // Final fallback for builds where hidden screenshot APIs are changed or blocked.
        if (takeScreenshotByCommand(outFile, false) || takeScreenshotByCommand(outFile, true)) {
            return outFile.exists() && outFile.length() > 0 ? outFile.getAbsolutePath() : "";
        }
        return "";
    }

    /**
     * Take a screenshot and return a Bitmap.
     *
     * <p>Android 4.4.2+ does not provide a public no-prompt full-screen capture API
     * for normal third-party apps. This method is intended for system/privileged apps
     * or rooted devices.</p>
     */
    public static Bitmap takeScreenshotBitmap() {
        WindowManager wm = (WindowManager) FBaseTools.getContext().getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return null;
        }

        Display display = wm.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);

        Matrix displayMatrix = new Matrix();
        float[] dims = {displayMetrics.widthPixels, displayMetrics.heightPixels};
        float degrees = getDegreesForRotation(display.getRotation());
        boolean requiresRotation = degrees > 0;
        if (requiresRotation) {
            displayMatrix.reset();
            displayMatrix.preRotate(-degrees);
            displayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
        }

        Bitmap screenBitmap = screenShot((int) dims[0], (int) dims[1]);
        if (screenBitmap == null) {
            return null;
        }

        if (requiresRotation) {
            Bitmap rotatedBitmap = Bitmap.createBitmap(displayMetrics.widthPixels,
                    displayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(rotatedBitmap);
            canvas.translate(rotatedBitmap.getWidth() / 2f, rotatedBitmap.getHeight() / 2f);
            canvas.rotate(degrees);
            canvas.translate(-dims[0] / 2f, -dims[1] / 2f);
            canvas.drawBitmap(screenBitmap, 0, 0, null);
            canvas.setBitmap(null);
            screenBitmap.recycle();
            screenBitmap = rotatedBitmap;
        }

        screenBitmap.setHasAlpha(false);
        screenBitmap.prepareToDraw();
        return screenBitmap;
    }

    /**
     * Save bitmap as PNG.
     *
     * @return true if saved successfully
     */
    public static boolean saveBitmap2file(Bitmap bmp, String fileName) {
        if (bmp == null || bmp.isRecycled() || fileName == null || fileName.length() == 0) {
            return false;
        }

        File file = new File(fileName);
        ensureParentDir(file);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;
        FileOutputStream stream = null;
        try {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
            is = new ByteArrayInputStream(baos.toByteArray());
            stream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                stream.write(buffer, 0, len);
            }
            stream.flush();
            return file.exists() && file.length() > 0;
        } catch (FileNotFoundException e) {
            Log.i(TAG, e.toString());
        } catch (IOException e) {
            Log.i(TAG, e.toString());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.i(TAG, e.toString());
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.i(TAG, e.toString());
                }
            }
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        return false;
    }

    private static float getDegreesForRotation(int value) {
        switch (value) {
            case Surface.ROTATION_90:
                return 360f - 90f;
            case Surface.ROTATION_180:
                return 360f - 180f;
            case Surface.ROTATION_270:
                return 360f - 270f;
            default:
                return 0f;
        }
    }

    private static Bitmap screenShot(int width, int height) {
        Log.i(TAG, "android.os.Build.VERSION.SDK : " + android.os.Build.VERSION.SDK_INT);
        try {
            Log.i(TAG, "width : " + width);
            Log.i(TAG, "height : " + height);
            Class<?> surfaceClass;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                surfaceClass = Class.forName(CLASS_SURFACE_CONTROL);
            } else {
                surfaceClass = Class.forName(CLASS_SURFACE);
            }

            try {
                Method method = surfaceClass.getDeclaredMethod(METHOD_SCREENSHOT, int.class, int.class);
                method.setAccessible(true);
                return (Bitmap) method.invoke(null, width, height);
            } catch (NoSuchMethodException ignored) {
                Method method = surfaceClass.getDeclaredMethod(METHOD_SCREENSHOT,
                        Rect.class, int.class, int.class, int.class);
                method.setAccessible(true);
                return (Bitmap) method.invoke(null, new Rect(0, 0, width, height),
                        width, height, Surface.ROTATION_0);
            }
        } catch (NoSuchMethodException e) {
            Log.e(TAG, e.toString());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.toString());
        } catch (IllegalAccessException e) {
            Log.e(TAG, e.toString());
        } catch (InvocationTargetException e) {
            Log.e(TAG, e.toString());
        } catch (ClassNotFoundException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    private static String buildDefaultScreenshotPath() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        String fileName = format.format(new Date(System.currentTimeMillis())) + ".png";
        return "/sdcard/" + fileName;
    }

    private static boolean takeScreenshotByCommand(File outFile, boolean root) {
        String cmd = SCREEN_CAP_BIN + " -p " + shellQuote(outFile.getAbsolutePath());
        FCmdTools.CommandResult result = FCmdTools.execCommand(cmd, root);
        if (result.result != 0) {
            Log.w(TAG, "screencap failed, root=" + root + ", err=" + result.errorMsg);
            return false;
        }
        return outFile.exists() && outFile.length() > 0;
    }

    private static void ensureParentDir(File file) {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            Log.w(TAG, "create parent dir failed: " + parentFile.getAbsolutePath());
        }
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
