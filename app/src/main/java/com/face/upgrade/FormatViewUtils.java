package com.face.upgrade;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class FormatViewUtils {
    /**
     * A movement method that interprets movement keys by scrolling the text buffer.
     *
     * @param textView
     */
    public static void setMovementMethod(TextView textView) {
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    /**
     * scroll back to top
     *
     * @param textView
     */
    public static void scrollBackToTop(TextView textView) {
        textView.scrollTo(0, 0);
        textView.setText("");
    }

    /**
     * show data to Activity
     *
     * @param htmlStr Support html tags
     */
    public static void formatData(TextView textView, String htmlStr, String appendPattern) {
        if (textView != null && !ObjectUtils.isEmpty(htmlStr)) {
            boolean isNull = ObjectUtils.isEmpty(appendPattern);
            textView.append(isNull ? "" : TimeUtils.getTodayDateHms(appendPattern) + "：");
            textView.append(Html.fromHtml(htmlStr));
            textView.append("\n");
            Layout layout = textView.getLayout();
            if (layout != null) {
                int scrollAmount = layout.getLineTop(textView.getLineCount()) - textView.getHeight();
                textView.scrollTo(0, scrollAmount > 0 ? scrollAmount : 0);
            }
        }
    }

    /**
     * show data to Activity
     *
     * @param htmlStr Support html tags
     */
    public static void formatData(TextView textView, String htmlStr) {
        formatData(textView, htmlStr, "");
    }

    public static String formatUnderlin(Context context,int color, String content) {
        return "<u><font color='" + getHexColor(context,color) + "'>" + content + "</font></u>";
    }


    public static String formatColor(Context context,String content, int color) {
        if (ObjectUtils.isEmpty(content)) {
            return content;
        } else {
            return "<font color=\"" + getHexColor(context,color) + "\">" + content + "</font>";
        }
    }

    public static String getHexColor(Context context,int color) {
        // 获取 Resources 对象
        Resources res = context.getResources();
        // 通过 Resources 对象获取颜色值
        int colorAccentValue = res.getColor(color);
        // 将颜色值转换为十六进制表示的字符串
        return String.format("#%06X", (0xFFFFFF & colorAccentValue));
    }
}
