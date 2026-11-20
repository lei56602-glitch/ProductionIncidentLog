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
        paint.setStrokeWidth(1);
        paint.setTextSize(9);
        paint.setTextAlign(Paint.Align.RIGHT);
        for(int tick=0;tick<=4;tick++) {
            float y=bottom-(bottom-top)*tick/4f;
            int tickValue=max*tick/4;
            paint.setColor(0xFFE8EEF5);
            canvas.drawLine(left,y,right,y,paint);
            paint.setColor(muted);
            canvas.drawText(String.valueOf(tickValue),left-7,y+3,paint);
        }
        float slot=(right-left)/labels.length;
        for(int i=0;i<labels.length;i++) {
            int value=i<values.length?Math.max(0,values[i]):0;
            float barWidth=Math.min(42,slot*0.58f);
            float center=left+slot*(i+0.5f);
            float barHeight=(bottom-top)*value/(float)max;
            paint.setColor(blue);
            canvas.drawRoundRect(center-barWidth/2,bottom-barHeight,center+barWidth/2,bottom,8,8,paint);
            paint.setColor(ink); paint.setTextSize(12); paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(String.valueOf(value),center,Math.max(top-2,bottom-barHeight-7),paint);
            paint.setColor(muted); paint.setTextSize(10);
            String label=labels[i]==null?"":labels[i];
            if(label.length()>7) label=label.substring(0,6)+"…";
            canvas.drawText(label,center,bottom+20,paint);
        }
    }
}
