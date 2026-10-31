package com.edward.galleryflow;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.animation.PathInterpolator;

/** Google-style navigation pill: animate the selected tab with gentle ease-in/ease-out. */
final class TabHighlight extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float radius, inset;
    private float position;
    private ValueAnimator animator;
    private final int surface, selected;
    TabHighlight(float density, int surface, int selected, int initial) {
        this.radius=18f*density;
        this.inset=2f*density;
        this.surface=surface;
        this.selected=selected;
        this.position=initial;
    }
    void moveTo(int index) {
        if(animator!=null)animator.cancel();
        if(Math.abs(position-index)<.001f){position=index;invalidateSelf();return;}
        animator=ValueAnimator.ofFloat(position,index);
        animator.setDuration(290);
        animator.setInterpolator(new PathInterpolator(.4f,0f,.2f,1f));
        animator.addUpdateListener(a->{position=(Float)a.getAnimatedValue();invalidateSelf();});
        animator.start();
    }
    void stop(){if(animator!=null)animator.cancel();}
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();
        paint.setColor(surface);
        canvas.drawRoundRect(new RectF(b),radius+inset,radius+inset,paint);
        float left=b.left+6*inset/2;
        float usable=b.width()-12*inset/2;
        float each=usable/5f;
        float x=left+each*position+inset;
        float y=b.top+3*inset/2;
        paint.setColor(selected);
        canvas.drawRoundRect(new RectF(x,y,x+each-2*inset,b.bottom-3*inset),radius,radius,paint);
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(android.graphics.ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}