package com.edward.galleryflow;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;
import java.util.*;

/** Only visible thumbnails are drawn; density layouts are cached outside the animation loop. */
final class GalleryView extends View {
    interface Listener {void open(int index);void hold(int index);default void scrolled(String month){}default void refresh(){}default void range(int start,int end,boolean add,boolean begin){}}
    private final ThumbnailLoader thumbnails;
    private final LockStore locks; private final AppSettings settings; private Set<String> selected=Collections.emptySet(),favorites=Collections.emptySet(); private final Map<String,Bitmap> visibleBitmaps=new HashMap<>(); private float maxHeight=0; private final Map<Integer,Float> heights=new HashMap<>();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF dest=new RectF();private final Rect source=new Rect();
    private final GestureDetector gestures;private final OverScroller scroller;
    private boolean scaling=false;private float startSpan,startDensity;
    private List<MediaRepository.Item> items=Collections.emptyList();
    private boolean[] hidden=new boolean[0];private final Runnable redraw=this::postInvalidateOnAnimation;
    private final Map<Integer,TileLayout.Box[]> layouts=new HashMap<>();
    private float[] preparedRatios=new float[0];private boolean[] preparedHeadings=new boolean[0];
    private int monthJumpAnchor=-1;private float density=3,offset=0,anchorFraction=.5f,anchorScreen;private int anchor=0;
    private boolean pullEligible,pulling;private float pullDistance;
    private boolean folderGrid=false;
    void setFolderGrid(boolean value){folderGrid=value;}
    private boolean square=false,admin=false,multiTouch=false;private Listener listener;
    private ValueAnimator animator;private boolean selectionMode,dragging,dragAdd;private int dragAnchor=-1;private float touchX,touchY,downX,downY;private final Runnable edgeScroll=()->autoScrollSelection();
    GalleryView(Context c,ThumbnailLoader t,LockStore l) {
        super(c);thumbnails=t;locks=l;settings=new AppSettings(c);scroller=new OverScroller(c);
        setContentDescription("Photo gallery. Pinch to resize tiles. Swipe to scroll. Gallery modes includes a one-column layout.");
        setFocusable(true);
        gestures=new GestureDetector(c,new GestureDetector.SimpleOnGestureListener(){
            public boolean onDown(android.view.MotionEvent e){scroller.forceFinished(true);return true;}
            public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){if(!multiTouch&&!dragging){monthJumpAnchor=-1;offset=clamp(offset+dy);notifyScroll();invalidate();}return true;}
            public boolean onFling(MotionEvent a,MotionEvent b,float vx,float vy){if(!multiTouch&&!dragging){scroller.fling(0,(int)offset,0,(int)-vy,0,0,0,(int)maxScroll());postInvalidateOnAnimation();}return true;}
            public boolean onSingleTapUp(MotionEvent e){if(!multiTouch&&listener!=null){int i=hit(e.getX(),e.getY());if(i>=0){performClick();listener.open(i);}}return true;}
            public void onLongPress(MotionEvent e){if(!multiTouch&&listener!=null){int i=hit(e.getX(),e.getY());if(i>=0){performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);listener.hold(i);if(selectionMode&&!items.get(i).folder){dragAnchor=i;dragAdd=true;beginDrag();}}}}
        });
    }
    void setItems(List<MediaRepository.Item> list,boolean admin,Listener listener){if(animator!=null)animator.cancel();scroller.forceFinished(true);items=list;monthJumpAnchor=-1;favorites=settings.set("favorites");visibleBitmaps.clear();this.admin=admin;this.listener=listener;hidden=new boolean[list.size()];for(int i=0;i<hidden.length;i++)hidden[i]=!admin&&locks.locked(LockStore.itemKey(list.get(i)));offset=0;layouts.clear();prepare();invalidate();}
    void setSquare(boolean value){square=value;layouts.clear();prepare();offset=clamp(offset);invalidate();}
    private static final int[] COLUMN_MODES={1,2,3,4,5,8,12};
    private int nearestColumn(float value){int best=COLUMN_MODES[0];for(int c:COLUMN_MODES)if(Math.abs(value-c)<Math.abs(value-best))best=c;return best;}
    void resizeBy(int delta){setAnchor(getWidth()/2f,getHeight()/2f);int target=nearestColumn(density),index=0;for(int j=0;j<COLUMN_MODES.length;j++)if(COLUMN_MODES[j]==target)index=j;settle(COLUMN_MODES[Math.max(0,Math.min(COLUMN_MODES.length-1,index+delta))]);}
    private int viewportItem(){if(layouts.isEmpty())return -1;for(int n=firstVisible();n<items.size();n++)if(!items.get(n).section&&!items.get(n).folder&&box(n).y+box(n).h>offset+dp(12))return n;return -1;}
    String monthLabel(){int n=viewportItem();return n<0?"":GalleryFormatting.month(items.get(n).date);}
    float scrollFraction(){return maxScroll()<=0?0:offset/maxScroll();}
    void scrubToFraction(float value){scroller.forceFinished(true);if(animator!=null)animator.cancel();monthJumpAnchor=-1;offset=clamp(Math.max(0,Math.min(1,value))*maxScroll());notifyScroll();invalidate();}

    private void notifyScroll(){if(listener!=null)listener.scrolled(monthLabel());}
    void jumpMonth(int direction){int current=monthJumpAnchor>=0?monthJumpAnchor:viewportItem();if(current<0)return;java.text.SimpleDateFormat format=new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT);String month=items.get(current).date<=0?"":format.format(new Date(items.get(current).date));int target=-1;for(int n=current+direction;n>=0&&n<items.size();n+=direction){MediaRepository.Item item=items.get(n);if(item.section||item.folder)continue;String candidate=item.date<=0?"":format.format(new Date(item.date));if(!candidate.equals(month)){target=n;if(direction<0){for(int m=n-1;m>=0;m--){MediaRepository.Item prior=items.get(m);if(prior.section)continue;if(prior.folder||!format.format(new Date(prior.date)).equals(candidate))break;target=m;}}break;}}if(target<0)return;monthJumpAnchor=target;if(target>0&&items.get(target-1).section)target--;scroller.forceFinished(true);if(animator!=null)animator.cancel();animator=ValueAnimator.ofFloat(offset,clamp(box(target).y));animator.setDuration(280);animator.setInterpolator(new DecelerateInterpolator(2));animator.addUpdateListener(a->{offset=(float)a.getAnimatedValue();notifyScroll();invalidate();});animator.start();}
    void setSelectionMode(boolean value){selectionMode=value;if(!value){dragging=false;removeCallbacks(edgeScroll);}}
    void setColumns(int n){setAnchor(getWidth()/2f,getHeight()/2f);settle(nearestColumn(n));}
    android.graphics.RectF tileRect(int index){TileLayout.Box b=box(index);return new RectF(b.x,b.y-offset,b.x+b.w,b.y-offset+b.h);}
    void setSelection(Set<String> selection){selected=new HashSet<>(selection);invalidate();}
    float savedDensity(){return density;} float savedOffset(){return offset;}
    void restore(float d,float scroll){density=nearestColumn(d);offset=clamp(scroll);invalidate();}
    /** Rebuild only the current two densities; 14 complete layouts would cost O(14N) memory. */
    private void prepare(){
        layouts.clear();heights.clear();maxHeight=0;
        if(getWidth()==0)return;
        preparedRatios=new float[items.size()];preparedHeadings=new boolean[items.size()];
        for(int i=0;i<preparedRatios.length;i++){
            MediaRepository.Item item=items.get(i);
            preparedHeadings[i]=item.section;
            preparedRatios[i]=(item.folder?settings.bool("squareFolders",true):square)?1:item.ratio;
        }
        int lo=Math.max(1,Math.min(12,(int)Math.floor(density)));
        ensureLayout(lo);ensureLayout(Math.min(12,lo+1));
    }
    private void ensureLayout(int n){
        if(getWidth()==0||layouts.containsKey(n))return;
        if(folderGrid&&(n==2||n>5)){
            int reuse=n==2?1:5;ensureLayout(reuse);
            layouts.put(n,layouts.get(reuse));heights.put(n,heights.get(reuse));return;
        }
        TileLayout.Box[] layout;
        if(folderGrid){
            layout=FolderGridLayout.create(getWidth(),dp(12),Math.max(2,Math.min(5,n)),items.size(),dp(60));
        }else{
            float[] effective=preparedRatios;
            if(n==1){effective=preparedRatios.clone();for(int j=0;j<effective.length;j++)if(!items.get(j).folder)effective[j]=items.get(j).ratio;}
            layout=TileLayout.masonry(getWidth(),dp(2),n,effective,preparedHeadings,dp(36));
        }
        float bottom=0;for(TileLayout.Box b:layout){bottom=Math.max(bottom,b.y+b.h);maxHeight=Math.max(maxHeight,b.h);}
        layouts.put(n,layout);heights.put(n,bottom+(folderGrid?dp(12):0));
    }
    protected void onSizeChanged(int w,int h,int ow,int oh){layouts.clear();prepare();offset=clamp(offset);}
    private TileLayout.Box box(int i){int lo=Math.max(1,(int)Math.floor(density)),hi=Math.min(12,lo+1);ensureLayout(lo);ensureLayout(hi);if(density==lo||layouts.get(lo)==layouts.get(hi))return layouts.get(lo)[i];return TileLayout.interpolate(layouts.get(lo)[i],layouts.get(hi)[i],density-lo);}
    private float maxScroll(){if(items.isEmpty()||layouts.isEmpty())return 0;int lo=Math.max(1,(int)density),hi=Math.min(12,lo+1);ensureLayout(lo);ensureLayout(hi);return Math.max(0,heights.get(lo)+(heights.get(hi)-heights.get(lo))*(density-lo)-getHeight());}
    private float clamp(float v){return Math.max(0,Math.min(v,maxScroll()));}
    private int hit(float x,float y){if(layouts.isEmpty())return -1;for(int i=firstVisible();i<items.size();i++){TileLayout.Box b=box(i);if(b.y-offset>getHeight())break;if(!items.get(i).section&&x>=b.x&&x<b.x+b.w&&y+offset>=b.y&&y+offset<b.y+b.h)return i;}return -1;}
    private int firstVisible(){int lo=0,hi=items.size();while(lo<hi){int m=(lo+hi)/2;TileLayout.Box b=box(m);if(b.y<offset-maxHeight)lo=m+1;else hi=m;}return lo;}
    private void setAnchor(float x,float y){anchor=hit(x,y);if(anchor<0)anchor=Math.min(items.size()-1,firstVisible());anchorScreen=y;if(anchor>=0){TileLayout.Box b=box(anchor);anchorFraction=Math.max(0,Math.min(1,(y+offset-b.y)/b.h));}}
    private void reanchor(){if(anchor>=0&&!items.isEmpty()){TileLayout.Box b=box(anchor);offset=clamp(b.y+b.h*anchorFraction-anchorScreen);}}
    private void settle(float target){if(animator!=null)animator.cancel();animator=ValueAnimator.ofFloat(density,target);animator.setDuration(240);animator.setInterpolator(new DecelerateInterpolator(1.6f));animator.addUpdateListener(a->{density=(float)a.getAnimatedValue();reanchor();invalidate();});animator.start();}
    public boolean onTouchEvent(MotionEvent e){
        int action=e.getActionMasked();touchX=e.getX();touchY=e.getY();if(action==MotionEvent.ACTION_DOWN){downX=touchX;downY=touchY;pullEligible=offset<=0&&!selectionMode;scroller.forceFinished(true);pulling=false;pullDistance=0;dragAnchor=hit(touchX,touchY);dragAdd=dragAnchor>=0&&!selected.contains(items.get(dragAnchor).identity());dragging=false;multiTouch=false;scaling=false;if(animator!=null)animator.cancel();}
        if(e.getPointerCount()>1){pullEligible=false;pulling=false;pullDistance=0;multiTouch=true;dragging=false;removeCallbacks(edgeScroll);}
        if(selectionMode&&!multiTouch&&e.getPointerCount()==1&&dragAnchor>=0&&!items.get(dragAnchor).folder){if(action==MotionEvent.ACTION_MOVE&&dragging){if(!dragging)beginDrag();selectAtPointer();removeCallbacks(edgeScroll);postOnAnimation(edgeScroll);return true;}if((action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)&&dragging){dragging=false;removeCallbacks(edgeScroll);MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);gestures.onTouchEvent(cancel);cancel.recycle();return true;}}
        boolean ending=action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL||(action==MotionEvent.ACTION_POINTER_UP&&(e.getPointerCount()==2||e.getActionIndex()<2));
        if(ending&&scaling){scaling=false;settle(nearestColumn(density));}
        else if(e.getPointerCount()>=2&&action!=MotionEvent.ACTION_POINTER_UP){
            float span=(float)Math.hypot(e.getX(1)-e.getX(0),e.getY(1)-e.getY(0));
            float focusX=(e.getX(0)+e.getX(1))/2,focusY=(e.getY(0)+e.getY(1))/2;
            if(!scaling&&span>dp(12)){if(animator!=null)animator.cancel();scroller.forceFinished(true);setAnchor(focusX,focusY);startSpan=span;startDensity=density;scaling=true;}
            else if(scaling&&span>dp(8)){density=Math.max(1,Math.min(12,startDensity*startSpan/span));anchorScreen=focusY;reanchor();invalidate();}
        }
        if(pullEligible&&!multiTouch&&!selectionMode){if(action==MotionEvent.ACTION_MOVE&&!pulling&&(Math.abs(touchX-downX)>dp(16)||touchY-downY< -dp(16)))pullEligible=false;float dy=touchY-downY,dx=touchX-downX;if(pullEligible&&action==MotionEvent.ACTION_MOVE&&(pulling||dy>dp(16)&&dy>Math.abs(dx)*1.5f)){if(!pulling){MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);gestures.onTouchEvent(cancel);cancel.recycle();}pulling=true;pullDistance=Math.max(0,dy);invalidate();return true;}if(pulling&&(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)){boolean refresh=action==MotionEvent.ACTION_UP&&PullRefresh.shouldRefresh(touchY-downY,dp(80),false);pulling=false;pullDistance=0;pullEligible=false;invalidate();if(refresh&&listener!=null){performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);listener.refresh();}return true;}}
        gestures.onTouchEvent(e);return true;
    }
    private void beginDrag(){dragging=true;scroller.forceFinished(true);if(listener!=null)listener.range(dragAnchor,dragAnchor,dragAdd,true);}
    private void selectAtPointer(){int i=hit(Math.max(1,Math.min(getWidth()-1,touchX)),Math.max(1,Math.min(getHeight()-1,touchY)));if(i>=0&&listener!=null)listener.range(dragAnchor,i,dragAdd,false);}
    private void autoScrollSelection(){if(!dragging||!selectionMode)return;float speed=touchY<dp(48)?-dp(12):touchY>getHeight()-dp(48)?dp(12):0;if(speed!=0){offset=clamp(offset+speed);selectAtPointer();invalidate();}postOnAnimation(edgeScroll);}
    public boolean performClick(){super.performClick();return true;}
    public void computeScroll(){if(scroller.computeScrollOffset()){monthJumpAnchor=-1;offset=clamp(scroller.getCurrY());notifyScroll();postInvalidateOnAnimation();}}
    protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();removeCallbacks(edgeScroll);super.onDetachedFromWindow();}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    private void drawLock(Canvas c,float x,float y,boolean open){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setColor(UiTheme.accent);paint.setShadowLayer(dp(2),0,dp(1),0xff000000);c.drawRoundRect(x-dp(6),y,x+dp(7),y+dp(10),dp(2),dp(2),paint);c.drawArc(x-dp(6),y-dp(9),x+dp(6),y+dp(3),180,open?230:180,false,paint);paint.clearShadowLayer();paint.setStyle(Paint.Style.FILL);}
    private String shortened(String value,float width){int count=paint.breakText(value,true,Math.max(1,width),null);if(count>=value.length())return value;int fit=paint.breakText(value,true,Math.max(1,width-paint.measureText("…")),null);return value.substring(0,fit)+"…";}
    private void drawFolder(Canvas c,MediaRepository.Item item,TileLayout.Box b,boolean locked,Set<String> keep){
      float top=b.y-offset;RectF cover=new RectF(b.x,top,b.x+b.w,top+b.w);int save=c.save();Path clip=new Path();clip.addRoundRect(cover,dp(14),dp(14),Path.Direction.CW);c.clipPath(clip);paint.setColor(UiTheme.surface);c.drawRect(cover,paint);Bitmap bitmap=null;
      if(!locked&&item.uri!=null&&!item.document){String key=item.identity();keep.add(key);bitmap=thumbnails.get(item.uri,thumbnailPixels(b.w),redraw);if(bitmap!=null)visibleBitmaps.put(key,bitmap);else bitmap=visibleBitmaps.get(key);}
      if(bitmap!=null){int side=Math.min(bitmap.getWidth(),bitmap.getHeight());source.set((bitmap.getWidth()-side)/2,(bitmap.getHeight()-side)/2,(bitmap.getWidth()+side)/2,(bitmap.getHeight()+side)/2);c.drawBitmap(bitmap,source,cover,paint);}else if(!locked){paint.setColor(0xffbca779);float x=cover.centerX(),y=cover.centerY();c.drawRoundRect(x-dp(16),y-dp(10),x+dp(16),y+dp(12),dp(3),dp(3),paint);c.drawRect(x-dp(16),y-dp(15),x-dp(1),y-dp(7),paint);}
      if(locked||admin&&locks.locked(LockStore.itemKey(item)))drawLock(c,cover.left+dp(16),cover.top+dp(16),!locked);c.restoreToCount(save);
      paint.setColor(UiTheme.text);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextSize(13*getResources().getDisplayMetrics().scaledDensity);c.drawText(shortened(settings.displayName(item.name),b.w-dp(4)),b.x+dp(2),cover.bottom+dp(23),paint);
      paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(11*getResources().getDisplayMetrics().scaledDensity);paint.setColor(UiTheme.muted);String detail=locked?"Locked":item.count+" items";c.drawText(shortened(detail,b.w-dp(4)),b.x+dp(2),cover.bottom+dp(43),paint);
      if(favorites.contains(item.identity())&&!locked){paint.setColor(UiTheme.accent);paint.setTextSize(dp(18));c.drawText("★",cover.right-dp(24),cover.top+dp(24),paint);}
    }
    private int thumbnailPixels(float tileWidth){if(!folderGrid&&density<1.5f)return Math.min(1280,Math.max(768,Math.round(tileWidth)));int target=Math.max(72,Math.round(tileWidth*1.3f));int limit=Math.max(128,settings.num("thumbnailSize",384));return Math.min(limit,target<=96?96:target<=144?144:target<=192?192:target<=256?256:target<=384?384:512);}
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(layouts.isEmpty())return;Set<String> keep=new HashSet<>();
        for(int i=firstVisible();i<items.size();i++){
            TileLayout.Box b=box(i);if(b.y-offset>getHeight())break;if(b.y+b.h<offset)continue;
            MediaRepository.Item item=items.get(i);boolean locked=hidden[i];if(folderGrid&&item.folder){drawFolder(canvas,item,b,locked,keep);continue;}dest.set(b.x,b.y-offset,b.x+b.w,b.y-offset+b.h);
            if(item.section){paint.setColor(UiTheme.muted);paint.setTextSize(dp(15));canvas.drawText(item.name,dp(8),dest.bottom-dp(12),paint);continue;}
            int save=canvas.save();if(item.folder&&settings.bool("roundFolders",true)){android.graphics.Path clip=new android.graphics.Path();clip.addRoundRect(dest,dp(8),dp(8),android.graphics.Path.Direction.CW);canvas.clipPath(clip);}
            paint.setColor(locked?0xff242124:UiTheme.surface);canvas.drawRect(dest,paint);
            Bitmap bitmap=null;if(!locked&&!(item.folder&&item.document)&&item.uri!=null){String key=item.identity();keep.add(key);bitmap=thumbnails.get(item.uri,thumbnailPixels(b.w),redraw);if(bitmap!=null)visibleBitmaps.put(key,bitmap);else bitmap=visibleBitmaps.get(key);}
            if(bitmap!=null){float scale=Math.max(b.w/bitmap.getWidth(),b.h/bitmap.getHeight());int sw=Math.min(bitmap.getWidth(),Math.round(b.w/scale)),sh=Math.min(bitmap.getHeight(),Math.round(b.h/scale));source.set((bitmap.getWidth()-sw)/2,(bitmap.getHeight()-sh)/2,(bitmap.getWidth()+sw)/2,(bitmap.getHeight()+sh)/2);canvas.drawBitmap(bitmap,source,dest,paint);}
            if(item.folder&&bitmap==null&&!locked){paint.setColor(locked?0xffb39b7a:0xffc7a86c);float cx=dest.centerX(),cy=dest.centerY()-dp(9);canvas.drawRoundRect(cx-dp(18),cy-dp(11),cx+dp(18),cy+dp(13),dp(3),dp(3),paint);canvas.drawRect(cx-dp(18),cy-dp(16),cx-dp(1),cy-dp(8),paint);}
            boolean label=item.folder||settings.bool("fileNames",false)&&(!settings.bool("videoNamesOnly",false)||item.video);
            if(label){paint.setColor(item.folder&&UiTheme.light?0xeeffffff:0xb8000000);canvas.drawRect(dest.left,dest.bottom-dp(32),dest.right,dest.bottom,paint);paint.setColor(item.folder&&UiTheme.light?UiTheme.text:0xffe7ecf0);paint.setTypeface(Typeface.create(item.folder?"sans-serif-medium":"sans-serif",Typeface.NORMAL));paint.setTextSize(dp(item.folder?settings.num("folderFont",12):11));String text=settings.displayName(item.name)+(item.folder&&settings.bool("folderCount",true)&&!locked?" ("+item.count+")":"");int count=paint.breakText(text,true,Math.max(1,b.w-dp(12)),null);canvas.drawText(text.substring(0,count),dest.left+dp(6),dest.bottom-dp(10),paint);}
            if(item.video){paint.setColor(0xc0000000);canvas.drawRect(dest.left,dest.top,dest.right,dest.top+dp(22),paint);paint.setColor(0xffffffff);paint.setTextSize(dp(11));String duration=settings.bool("videoDuration",true)?String.format(java.util.Locale.ROOT," %d:%02d",item.duration/60000,item.duration/1000%60):"";canvas.drawText("▶"+duration,dest.left+dp(4),dest.top+dp(15),paint);}
            if(item.folder&&(locked||admin&&locks.locked(LockStore.itemKey(item))))drawLock(canvas,dest.left+dp(16),dest.top+dp(16),!locked);
            if(favorites.contains(item.identity())&&!locked){paint.setColor(UiTheme.accent);paint.setTextSize(dp(17));canvas.drawText("★",dest.right-dp(22),dest.top+dp(21),paint);}
            if(selected.contains(item.identity())){paint.setColor(0x665b98c0);canvas.drawRect(dest,paint);paint.setColor(0xffffffff);paint.setTextSize(dp(26));canvas.drawText("✓",dest.centerX()-dp(12),dest.centerY(),paint);}
            canvas.restoreToCount(save);
        }visibleBitmaps.keySet().retainAll(keep);if(pulling&&pullDistance>0){float progress=Math.min(1,pullDistance/dp(80)),y=dp(8)+Math.min(dp(18),pullDistance*.15f);String label=progress>=1?"Release to refresh":"Pull to refresh";paint.setTextSize(dp(12));float width=paint.measureText(label)+dp(58),left=(getWidth()-width)/2;paint.setColor(UiTheme.surface);canvas.drawRoundRect(left,y,left+width,y+dp(40),dp(20),dp(20),paint);paint.setColor(UiTheme.accent);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));canvas.drawArc(left+dp(14),y+dp(12),left+dp(30),y+dp(28),-90,Math.max(25,progress*330),false,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(UiTheme.text);canvas.drawText(label,left+dp(40),y+dp(25),paint);}

    }
}
