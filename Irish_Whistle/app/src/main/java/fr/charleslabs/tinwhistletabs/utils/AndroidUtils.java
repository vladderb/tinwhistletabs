package fr.charleslabs.tinwhistletabs.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AndroidUtils {
    public static void clearSpans(@NonNull final Spannable editable) {
        final Object[] spans = editable.getSpans(0, editable.length(), Object.class);

        for (final Object span : spans) {
            if (span instanceof ForegroundColorSpan) {
                editable.removeSpan(span);
            }
        }
    }

    public static int getCharacterOffset(TextView textView, int x, int y) {
        x += textView.getScrollX() - textView.getTotalPaddingLeft();
        y += textView.getScrollY() - textView.getTotalPaddingTop();

        final Layout layout = textView.getLayout();

        final int lineCount = layout.getLineCount();
        if (lineCount == 0 || y < layout.getLineTop(0) || y >= layout.getLineBottom(lineCount - 1))
            return -1;

        final int line = layout.getLineForVertical(y);
        if (x < layout.getLineLeft(line) || x >= layout.getLineRight(line))
            return -1;

        int start = layout.getLineStart(line);
        int end = layout.getLineEnd(line);

        while (end > start + 1) {
            int middle = start + (end - start) / 2;

            if (x >= layout.getPrimaryHorizontal(middle)) {
                start = middle;
            }
            else {
                end = middle;
            }
        }

        return start;
    }

    public static Drawable createTextDrawable(Context context, String text, int color) {
        return new Drawable() {
            private final Paint paint = new Paint();
            
            {
                paint.setColor(color);
                paint.setTextSize(48f);
                paint.setAntiAlias(true);
                paint.setFakeBoldText(true);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setStyle(Paint.Style.FILL);
            }

            @Override
            public void draw(@NonNull Canvas canvas) {
                Paint.FontMetrics fontMetrics = paint.getFontMetrics();
                float textHeight = fontMetrics.descent - fontMetrics.ascent;
                float textOffset = (textHeight / 2) - fontMetrics.descent;
                
                canvas.drawText(text, 
                    getBounds().width() / 2f, 
                    (getBounds().height() / 2f) + textOffset, 
                    paint);
            }

            @Override
            public void setAlpha(int alpha) {
                paint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
                paint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
    }

}
