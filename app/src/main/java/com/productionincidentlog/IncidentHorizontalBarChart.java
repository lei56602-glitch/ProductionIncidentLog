package com.productionincidentlog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Offline horizontal bar chart for compact category and equipment rankings. */
public class IncidentHorizontalBarChart extends View {
    private final String[] labels;
    private final int[] values;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int blue = 0xFF1677EB;
    private final int green = 0xFF16B887;
    private final int ink = 0xFF17324D;
    private final int muted = 0xFF71869A;

    public IncidentHorizontalBarChart(Context context, String[] labels, int[] values) {
        super(context);
        this.labels = labels == null ? new String[0] : labels.clone();
        this.values = values == null ? new int[0] : values.clone();
        setBackgroundColor(0xFFFFFFFF);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desired = Math.max(72, labels.length * 43 + 18);
        int height = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
            ? MeasureSpec.getSize(heightMeasureSpec) : desired;
        setMeasuredDimension(width, Math.max(desired, height));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (labels.length == 0) return;
        float labelWidth = Math.min(104, Math.max(72, getWidth() * 0.27f));
        float left = labelWidth;
        float right = getWidth() - 38;
        float slot = (getHeight() - 12) / (float) labels.length;
        int max = 1;
        for (int value : values) if (value > max) max = value;
        for (int i = 0; i < labels.length; i++) {
            float centerY = 8 + slot * (i + 0.5f);
            paint.setColor(muted);
            paint.setTextSize(12);
            paint.setTextAlign(Paint.Align.LEFT);
            String label = labels[i] == null ? "" : labels[i];
            float labelLimit = labelWidth - 9;
            while (!label.isEmpty() && paint.measureText(label) > labelLimit) {
                label = label.substring(0, label.length() - 1);
            }
            if (!label.equals(labels[i]) && labelLimit > paint.measureText("…")) {
                while (!label.isEmpty() && paint.measureText(label + "…") > labelLimit) {
                    label = label.substring(0, label.length() - 1);
                }
                label += "…";
            }
            canvas.drawText(label, 3, centerY + 4, paint);
            paint.setColor(0xFFE9EEF5);
            canvas.drawRoundRect(left, centerY - 7, right, centerY + 7, 8, 8, paint);
            int value = i < values.length ? Math.max(0, values[i]) : 0;
            float end = left + (right - left) * value / (float) max;
            paint.setColor(i < 2 && labels.length == 2 ? green : blue);
            canvas.drawRoundRect(left, centerY - 7, Math.max(left + 2, end), centerY + 7, 8, 8, paint);
            paint.setColor(ink);
            paint.setTextSize(12);
            paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf(value), getWidth() - 4, centerY + 4, paint);
        }
    }
}
