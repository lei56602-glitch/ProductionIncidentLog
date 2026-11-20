package com.productionincidentlog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Lightweight native bar chart for offline incident statistics. */
public class IncidentBarChart extends View {
    private final String[] labels;
    private final int[] values;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int blue = 0xFF1769AA;
    private final int ink = 0xFF17324D;
    private final int muted = 0xFF71869A;

    public IncidentBarChart(Context context, String[] labels, int[] values) {
        super(context);
        this.labels = labels == null ? new String[0] : labels.clone();
        this.values = values == null ? new int[0] : values.clone();
        setBackgroundColor(0xFFFFFFFF);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, Math.max(190, MeasureSpec.getSize(heightMeasureSpec)));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (labels.length == 0) return;
        float left=42, right=getWidth()-16, top=22, bottom=getHeight()-48;
        int max=1; for(int v:values) if(v>max) max=v;
        paint.setColor(0xFFE5EDF5); paint.setStrokeWidth(1);
        canvas.drawLine(left,bottom,right,bottom,paint);
        float slot=(right-left)/labels.length;
        for(int i=0;i<labels.length;i++) {
            float barWidth=Math.min(42,slot*0.58f);
            float center=left+slot*(i+0.5f);
            float barHeight=(bottom-top)*values[i]/(float)max;
            paint.setColor(blue);
            canvas.drawRoundRect(center-barWidth/2,bottom-barHeight,center+barWidth/2,bottom,8,8,paint);
            paint.setColor(ink); paint.setTextSize(12); paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(String.valueOf(values[i]),center,Math.max(top-2,bottom-barHeight-7),paint);
            paint.setColor(muted); paint.setTextSize(10);
            String label=labels[i];
            if(label.length()>7) label=label.substring(0,7);
            canvas.drawText(label,center,bottom+20,paint);
        }
    }
}
