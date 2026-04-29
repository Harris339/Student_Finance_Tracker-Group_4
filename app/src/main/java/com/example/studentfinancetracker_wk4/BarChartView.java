package com.example.studentfinancetracker_wk4;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class BarChartView extends View {

    private float[] income   = {};
    private float[] expenses = {};

    private final Paint incPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint expPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        incPaint.setColor(Color.parseColor("#1D9E75"));
        expPaint.setColor(Color.parseColor("#D85A30"));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        gridPaint.setColor(Color.parseColor("#44FFFFFF"));
        gridPaint.setStrokeWidth(1f);
    }

    public void setData(float[] income, float[] expenses) {
        this.income   = income;
        this.expenses = expenses;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (income.length == 0) return;

        int w = getWidth();
        int h = getHeight();
        float paddingLeft   = 80f;
        float paddingRight  = 20f;
        float paddingTop    = 20f;
        float paddingBottom = 50f;
        float chartH = h - paddingTop - paddingBottom;
        float chartW = w - paddingLeft - paddingRight;

        // Find max value for scaling
        float max = 1f;
        for (int i = 0; i < income.length; i++) {
            max = Math.max(max, Math.max(income[i], expenses[i]));
        }
        max = (float)(Math.ceil(max / 50.0) * 50); // round up to nearest 50

        // Draw grid lines + Y labels
        int gridLines = 4;
        for (int i = 0; i <= gridLines; i++) {
            float y = paddingTop + chartH * (1f - (float) i / gridLines);
            canvas.drawLine(paddingLeft, y, w - paddingRight, y, gridPaint);
            String label = "€" + (int)(max * i / gridLines);
            canvas.drawText(label, paddingLeft - 8f, y + 9f, textPaint);
        }

        // Draw bars
        float groupW  = chartW / income.length;
        float barW    = groupW * 0.28f;
        float gap     = groupW * 0.06f;

        for (int i = 0; i < income.length; i++) {
            float groupX = paddingLeft + i * groupW + groupW * 0.1f;

            // Income bar
            float incH = (income[i] / max) * chartH;
            RectF incRect = new RectF(groupX, paddingTop + chartH - incH,
                    groupX + barW, paddingTop + chartH);
            canvas.drawRoundRect(incRect, 6f, 6f, incPaint);

            // Expense bar
            float expH = (expenses[i] / max) * chartH;
            RectF expRect = new RectF(groupX + barW + gap, paddingTop + chartH - expH,
                    groupX + barW * 2 + gap, paddingTop + chartH);
            canvas.drawRoundRect(expRect, 6f, 6f, expPaint);

            // Week label
            canvas.drawText("Wk " + (i + 1),
                    groupX + barW + gap / 2f,
                    h - 10f, textPaint);
        }

        // Legend
        textPaint.setTextAlign(Paint.Align.LEFT);
        incPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(paddingLeft, h - 44f, paddingLeft + 20f, h - 28f, incPaint);
        canvas.drawText("Income", paddingLeft + 26f, h - 30f, textPaint);
        canvas.drawRect(paddingLeft + 110f, h - 44f, paddingLeft + 130f, h - 28f, expPaint);
        canvas.drawText("Expenses", paddingLeft + 136f, h - 30f, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }
}