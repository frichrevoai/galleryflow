package com.edward.galleryflow;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.widget.Switch;

/** A compact native Switch with a mathematically circular thumb and soft shadow. */
final class CircleSwitch extends Switch {
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    CircleSwitch(Context context, boolean compact) {
        super(context);
        setShowText(false);
        setSplitTrack(false);
        setSwitchPadding(dp(compact ? 6 : 12));
        setSwitchMinWidth(dp(compact ? 38 : 48));
        setMinimumHeight(dp(compact ? 40 : 50));
        setThumbDrawable(new RoundThumb());
        setThumbTintList(null);
        GradientDrawable track = new GradientDrawable();
        track.setShape(GradientDrawable.RECTANGLE);
        track.setCornerRadius(dp(compact ? 13 : 15));
        track.setColor(UiTheme.line);
        track.setSize(dp(compact ? 37 : 46), dp(compact ? 22 : 28));
        setTrackDrawable(track);
        setTrackTintList(new ColorStateList(
            new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
            new int[] { UiTheme.accent, UiTheme.line }));
    }
    private final class RoundThumb extends Drawable {
        final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint face = new Paint(Paint.ANTI_ALIAS_FLAG);
        RoundThumb(){ shadow.setColor(0x44000000);face.setColor(0xffffffff); }
        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            float radius = Math.max(1, Math.min(b.width(), b.height()) * .5f - dp(3));
            float cx = b.exactCenterX(), cy = b.exactCenterY() - dp(1);
            canvas.drawCircle(cx, cy + dp(2), radius + dp(1), shadow);
            canvas.drawCircle(cx, cy, radius, face);
        }
        @Override public void setAlpha(int a){face.setAlpha(a);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter f){face.setColorFilter(f);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        @Override public int getIntrinsicWidth(){return dp(24);}
        @Override public int getIntrinsicHeight(){return dp(24);}
    }
}
