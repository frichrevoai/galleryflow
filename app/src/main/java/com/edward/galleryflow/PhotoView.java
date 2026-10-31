package com.edward.galleryflow;
import android.animation.ValueAnimator;import android.content.Context;import android.graphics.*;import android.view.*;import android.view.animation.DecelerateInterpolator;
final class PhotoView extends View {
 interface Listener{void advance(int step,boolean animate);void toggle();void close();void brightness(float delta);void rotated(float angle);default void drag(float offset){}default void videoGestureStart(){}default void videoAdjust(boolean left,float delta){}default void videoSpeed(boolean held){}}
 private long transitionStarted;private int transitionDuration;private boolean transitioning;private final Choreographer.FrameCallback transitionFrame=this::transitionFrame;
 private Bitmap image,previous,before,after,blurredBackground;private float dragOffset,transitionStart,opening=1;private boolean directSlide,videoMode;private RectF origin;private boolean screenOrigin;private float zoom=1,x=0,y=0,angle=0,oldAngle=0,progress=1;private int direction=1;
 private final Paint paint=new Paint(3);private final AppSettings settings;private final GestureDetector gestures;private final ScaleGestureDetector pinch;
 private boolean hasPrevious,hasNext,paging,pendingPage,tracking;private int pageStep;private float releasedVelocity;private VelocityTracker velocityTracker;
 private final SpringMotion spring=new SpringMotion(440,.88);private long lastFrame;private final Choreographer.FrameCallback springFrame=this::springFrame;
 private final SpringMotion orientationSpring=new SpringMotion(300,.85);private boolean orienting;private long orientationFrame;private float oldScale,newScale,oldVisualAngle,newVisualAngle,oldContainerWidth,oldContainerHeight,newContainerWidth,newContainerHeight;private final Choreographer.FrameCallback orientationCallback=this::orientationFrame;
 void boundaries(boolean previous,boolean next){hasPrevious=previous;hasNext=next;}
 private void springFrame(long now){if(!paging)return;double dt=lastFrame==0?0:(now-lastFrame)/1e9;lastFrame=now;boolean more=spring.step(dt);if(pageStep>0&&spring.value<=-getWidth()||pageStep<0&&spring.value>=getWidth()){spring.value=spring.target;spring.velocity=0;more=false;}dragOffset=(float)spring.value;if(pageStep!=0)dragOffset=Math.max(-getWidth(),Math.min(getWidth(),dragOffset));if(listener!=null)listener.drag(dragOffset);invalidate();if(more)Choreographer.getInstance().postFrameCallback(springFrame);else{paging=false;if(!pendingPage)finishPage();}}
 private void settle(int step,float velocity){Choreographer.getInstance().removeFrameCallback(springFrame);pageStep=step;paging=true;lastFrame=0;spring.start(dragOffset,velocity,-step*getWidth());Choreographer.getInstance().postFrameCallback(springFrame);}
 private void finishPage(){dragOffset=0;previous=null;pageStep=0;pendingPage=false;progress=1;if(listener!=null)listener.drag(0);invalidate();}
 private void stopSpring(){Choreographer.getInstance().removeFrameCallback(springFrame);paging=false;lastFrame=0;}
 private void orientationFrame(long now){if(!orienting)return;double dt=orientationFrame==0?0:(now-orientationFrame)/1e9;orientationFrame=now;orienting=orientationSpring.step(dt);invalidate();if(orienting)Choreographer.getInstance().postFrameCallback(orientationCallback);}
 private Listener listener;private boolean speedHeld;private boolean multi=false,brightnessDrag=false;private float downX,downY,lastAngle;private ValueAnimator animator;
 PhotoView(Context c){super(c);settings=new AppSettings(c);setContentDescription("Image viewer. Center tap toggles fullscreen. Left and right edges select previous and next.");
  pinch=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector d){float before=zoom;zoom=Math.max(1,Math.min(8,zoom*d.getScaleFactor()));x=(x-(d.getFocusX()-getWidth()/2f))*zoom/before+d.getFocusX()-getWidth()/2f;y=(y-(d.getFocusY()-getHeight()/2f))*zoom/before+d.getFocusY()-getHeight()/2f;bound();invalidate();return true;}});
  gestures=new GestureDetector(c,new GestureDetector.SimpleOnGestureListener(){
   public boolean onDown(MotionEvent e){return true;}
   public void onLongPress(MotionEvent e){if(videoMode&&!multi&&e.getX()>getWidth()*.33f&&e.getX()<getWidth()*.67f&&listener!=null){speedHeld=true;listener.videoSpeed(true);}}
   public boolean onSingleTapUp(MotionEvent e){if(videoMode){if(listener!=null&&!multi)listener.toggle();return true;}if(!multi&&listener!=null&&settings.bool("tapAdvance",true)&&(e.getX()<getWidth()*.22f||e.getX()>getWidth()*.78f)){listener.advance(e.getX()<getWidth()/2?-1:1,false);return true;}return false;}
   public boolean onSingleTapConfirmed(MotionEvent e){if(videoMode)return true;if(!multi&&listener!=null&&(!settings.bool("tapAdvance",true)||e.getX()>=getWidth()*.22f&&e.getX()<=getWidth()*.78f))listener.toggle();return true;}
   public boolean onDoubleTapEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_UP&&!videoMode&&!multi&&!tracking&&!brightnessDrag&&Math.abs(e.getX()-downX)<dp(8)&&Math.abs(e.getY()-downY)<dp(8)&&listener!=null&&settings.bool("tapAdvance",true)&&(e.getX()<getWidth()*.22f||e.getX()>getWidth()*.78f)){listener.advance(e.getX()<getWidth()/2?-1:1,false);}return true;}
   public boolean onDoubleTap(MotionEvent e){if(videoMode)return true;if(e.getX()>getWidth()*.22f&&e.getX()<getWidth()*.78f){zoom=zoom>1?1:3;x=y=0;bound();invalidate();}return true;}
   public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){if(multi||pinch.isInProgress())return true;if(videoMode){if(a!=null&&Math.abs(b.getY()-downY)>Math.abs(b.getX()-downX)*1.3f){brightnessDrag=true;if(speedHeld){speedHeld=false;if(listener!=null)listener.videoSpeed(false);}if(listener!=null)listener.videoAdjust(downX<getWidth()/2f,dy/Math.max(1,getHeight()));}return true;}if(zoom>1.01f){x-=dx;y-=dy;bound();invalidate();}else if(a!=null&&settings.bool("brightnessGesture",false)&&downX<getWidth()/3f&&Math.abs(b.getY()-downY)>Math.abs(b.getX()-downX)*1.3f){brightnessDrag=true;if(listener!=null)listener.brightness(dy/getHeight());}return true;}
  });
 }
 float[] saveViewState(){return new float[]{zoom,x,y,angle};}
 void restoreViewState(float[] state){if(state!=null&&state.length==4){zoom=Math.max(1,Math.min(8,state[0]));x=state[1];y=state[2];angle=state[3];bound();invalidate();}}
 Bitmap snapshot(){return image;}
 void setListener(Listener l){listener=l;}
 void resetMediaState(){if(speedHeld&&listener!=null)listener.videoSpeed(false);speedHeld=false;stopSpring();transitioning=false;orienting=false;Choreographer.getInstance().removeFrameCallback(transitionFrame);Choreographer.getInstance().removeFrameCallback(orientationCallback);if(animator!=null)animator.cancel();animator=null;origin=null;screenOrigin=false;opening=1;pendingPage=false;pageStep=0;tracking=false;brightnessDrag=false;multi=false;directSlide=false;dragOffset=transitionStart=0;zoom=1;x=y=angle=oldAngle=0;progress=1;image=previous=before=after=blurredBackground=null;setTranslationX(0);setTranslationY(0);if(listener!=null)listener.drag(0);invalidate();}
 void setVideoMode(boolean value){videoMode=value;zoom=1;x=y=0;invalidate();}
 void refreshBackground(){updateBackground(image);invalidate();}
 void neighbor(int step,Bitmap b){if(step<0)before=b;else after=b;invalidate();}
 void clearNeighbors(){before=after=null;}
 void openingFrom(RectF rect){origin=rect;opening=0;screenOrigin=false;}
 void openingFromScreen(RectF rect){origin=rect;opening=0;screenOrigin=true;}
 void animateOpening(){if(origin==null)return;if(animator!=null)animator.cancel();animator=ValueAnimator.ofFloat(0,1);animator.setDuration(260);animator.setInterpolator(new DecelerateInterpolator(2f));animator.addUpdateListener(v->{opening=(float)v.getAnimatedValue();if(opening>=1)origin=null;invalidate();});animator.start();}
 void cancelDrag(){pendingPage=false;previous=null;if(animator!=null)animator.cancel();settle(0,releasedVelocity);}

 void setImage(Bitmap b){transitioning=false;Choreographer.getInstance().removeFrameCallback(transitionFrame);stopSpring();pendingPage=false;pageStep=0;if(animator!=null)animator.cancel();image=b;updateBackground(b);previous=null;dragOffset=0;before=after=null;zoom=1;x=y=0;angle=0;progress=1;invalidate();}
 void display(Bitmap b,int step,boolean animate,float rotation){
  if(animator!=null)animator.cancel();
  if(pendingPage&&pageStep==step){previous=image;oldAngle=angle;image=b;updateBackground(b);angle=rotation;zoom=1;x=y=0;pendingPage=false;if(!paging)finishPage();invalidate();return;}
  stopSpring();pageStep=0;pendingPage=false;previous=image;oldAngle=angle;image=b;updateBackground(b);angle=rotation;zoom=1;x=y=0;direction=step;
  directSlide=animate&&Math.abs(dragOffset)>0;transitionStart=dragOffset;dragOffset=0;progress=animate&&previous!=null?0:1;
  if(progress==0){transitionDuration=Math.max(120,Math.min(2000,settings.num("transitionMs",260)));transitionStarted=0;transitioning=true;Choreographer.getInstance().removeFrameCallback(transitionFrame);Choreographer.getInstance().postFrameCallback(transitionFrame);}else{transitioning=false;Choreographer.getInstance().removeFrameCallback(transitionFrame);previous=null;}

  if(origin!=null)animateOpening();invalidate();
 }
 void displaySlideshow(Bitmap b,int step,float rotation){stopSpring();pendingPage=false;pageStep=0;dragOffset=0;origin=null;opening=1;display(b,step,true,rotation);}
 long transitionRemaining(){return transitioning?transitionDuration:0;}
 private void transitionFrame(long now){if(!transitioning)return;if(transitionStarted==0)transitionStarted=now;progress=Math.min(1,(now-transitionStarted)/(transitionDuration*1000000f));invalidate();if(progress<1)Choreographer.getInstance().postFrameCallback(transitionFrame);else{transitioning=false;previous=null;}}
 float rotation(){return angle;}void rotate(){oldVisualAngle=effectiveAngle(image,angle);angle=(angle+90)%360;newVisualAngle=oldVisualAngle+90;oldContainerWidth=newContainerWidth=getWidth();oldContainerHeight=newContainerHeight=getHeight();orientationSpring.start(0,0,1);orientationFrame=0;orienting=true;Choreographer.getInstance().removeFrameCallback(orientationCallback);Choreographer.getInstance().postFrameCallback(orientationCallback);if(listener!=null)listener.rotated(angle);bound();invalidate();}
 private float effectiveAngle(Bitmap b,float a){if(b==null)return a;if(settings.bool("bestFit",false)){boolean portrait=b.getHeight()>b.getWidth(),screen=getHeight()>getWidth();if(portrait!=screen)a+=90;}return a;}
 private float fit(Bitmap b,float a){double radians=Math.toRadians(effectiveAngle(b,a));float w=(float)(Math.abs(b.getWidth()*Math.cos(radians))+Math.abs(b.getHeight()*Math.sin(radians))),h=(float)(Math.abs(b.getWidth()*Math.sin(radians))+Math.abs(b.getHeight()*Math.cos(radians)));return Math.min(getWidth()/w,getHeight()/h);}
 private void bound(){if(image==null)return;double a=Math.toRadians(effectiveAngle(image,angle));float w=(float)(Math.abs(image.getWidth()*Math.cos(a))+Math.abs(image.getHeight()*Math.sin(a))),h=(float)(Math.abs(image.getWidth()*Math.sin(a))+Math.abs(image.getHeight()*Math.cos(a)));float mx=Math.max(0,(w*fit(image,angle)*zoom-getWidth())/2),my=Math.max(0,(h*fit(image,angle)*zoom-getHeight())/2);x=Math.max(-mx,Math.min(mx,x));y=Math.max(-my,Math.min(my,y));}
 protected void onSizeChanged(int w,int h,int oldw,int oldh){stopSpring();if(!transitioning)finishPage();if(image!=null&&oldw>0&&oldh>0&&w>0&&h>0&&Math.abs(w/(float)h-oldw/(float)oldh)>.3f){float a=angle;if(settings.bool("bestFit",false)&&(image.getHeight()>image.getWidth())!=(oldh>oldw))a+=90;oldVisualAngle=a;newVisualAngle=effectiveAngle(image,angle);double r=Math.toRadians(a);float bw=(float)(Math.abs(image.getWidth()*Math.cos(r))+Math.abs(image.getHeight()*Math.sin(r))),bh=(float)(Math.abs(image.getWidth()*Math.sin(r))+Math.abs(image.getHeight()*Math.cos(r)));oldContainerWidth=oldw;oldContainerHeight=oldh;newContainerWidth=w;newContainerHeight=h;oldScale=Math.min(oldw/bw,oldh/bh);newScale=fit(image,angle);orientationSpring.start(0,0,1);orientationFrame=0;orienting=true;Choreographer.getInstance().removeFrameCallback(orientationCallback);Choreographer.getInstance().postFrameCallback(orientationCallback);}bound();}
 public boolean onTouchEvent(MotionEvent e){
  int action=e.getActionMasked();if((action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)&&speedHeld){speedHeld=false;if(listener!=null)listener.videoSpeed(false);MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);gestures.onTouchEvent(cancel);cancel.recycle();return true;}if(pendingPage)return true;
  if(action==MotionEvent.ACTION_DOWN){if(videoMode&&listener!=null)listener.videoGestureStart();if(transitioning){transitioning=false;Choreographer.getInstance().removeFrameCallback(transitionFrame);progress=1;previous=null;}if(animator!=null){animator.cancel();origin=null;opening=1;progress=1;}if(pageStep!=0){if(previous!=null)dragOffset+=pageStep*getWidth();previous=null;pageStep=0;pendingPage=false;progress=1;}stopSpring();if(velocityTracker!=null)velocityTracker.recycle();velocityTracker=VelocityTracker.obtain();downX=e.getX()-dragOffset;downY=e.getY();multi=false;brightnessDrag=false;tracking=false;}
  if(velocityTracker!=null)velocityTracker.addMovement(e);
  if(e.getPointerCount()>1){multi=true;if(dragOffset!=0){stopSpring();finishPage();}}
  if(!videoMode)pinch.onTouchEvent(e);gestures.onTouchEvent(e);
  if(action==MotionEvent.ACTION_MOVE&&!multi&&!brightnessDrag&&zoom<=1.01f){float dx=e.getX()-downX,dy=e.getY()-downY;if(tracking||Math.abs(dx)>dp(7)&&Math.abs(dx)>Math.abs(dy)*1.2f){tracking=true;boolean edge=dx>0&&!hasPrevious||dx<0&&!hasNext;dragOffset=edge?SpringMotion.rubber(dx,getWidth()*.5f):Math.max(-getWidth(),Math.min(getWidth(),dx));if(listener!=null)listener.drag(dragOffset);postInvalidateOnAnimation();}}
  if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){releasedVelocity=0;if(velocityTracker!=null){velocityTracker.computeCurrentVelocity(1000,dp(8000));releasedVelocity=velocityTracker.getXVelocity();velocityTracker.recycle();velocityTracker=null;}
   if(action==MotionEvent.ACTION_CANCEL){cancelDrag();return true;}
   if(!multi&&!brightnessDrag&&zoom<=1.01f&&listener!=null){float dx=e.getX()-downX,dy=e.getY()-downY;if(tracking){int step=SpringMotion.destination(dragOffset,releasedVelocity,getWidth(),hasPrevious,hasNext);if(step!=0){pendingPage=true;settle(step,releasedVelocity);listener.advance(step,true);}else settle(0,releasedVelocity*.45f);}else if(dy>dp(75)&&Math.abs(dy)>Math.abs(dx)*1.2f&&settings.bool("swipeClose",true))listener.close();}
  }return true;
 }
 private float dp(float n){return n*getResources().getDisplayMetrics().density;}
 protected void onDraw(Canvas c){
  if(videoMode){if(dragOffset!=0){Bitmap adjacent=dragOffset<0?after:before;if(adjacent!=null)drawImage(c,adjacent,0,1,dragOffset+(dragOffset<0?getWidth():-getWidth()),0,255);}return;}
  c.drawColor(0xff080808);if(image==null)return;
  if(settings.bool("blurBackground",false)&&blurredBackground!=null){
   float w=getWidth(),h=getHeight(),bw=blurredBackground.getWidth(),bh=blurredBackground.getHeight();
   float scale=Math.max(w/bw,h/bh),sw=w/scale,sh=h/scale;
   Rect src=new Rect(Math.round((bw-sw)/2f),Math.round((bh-sh)/2f),Math.round((bw+sw)/2f),Math.round((bh+sh)/2f));
   c.drawBitmap(blurredBackground,src,new RectF(0,0,w,h),paint);
   c.drawColor(0xb5000000);
  }
  if(origin!=null&&opening<1){if(screenOrigin){int[] loc=new int[2];getLocationOnScreen(loc);origin.offset(-loc[0],-loc[1]);screenOrigin=false;}float targetWidth=image.getWidth()*fit(image,angle),targetHeight=image.getHeight()*fit(image,angle);RectF target=new RectF((getWidth()-targetWidth)/2,(getHeight()-targetHeight)/2,(getWidth()+targetWidth)/2,(getHeight()+targetHeight)/2);float t=opening;RectF r=new RectF(origin.left+(target.left-origin.left)*t,origin.top+(target.top-origin.top)*t,origin.right+(target.right-origin.right)*t,origin.bottom+(target.bottom-origin.bottom)*t);c.drawBitmap(image,null,r,paint);return;}
  if(pageStep!=0&&previous!=null){drawImage(c,previous,oldAngle,1,dragOffset,0,255);drawImage(c,image,angle,1,dragOffset+pageStep*getWidth(),0,255);}
  else if(dragOffset!=0){drawImage(c,image,angle,1,dragOffset,0,255);Bitmap adjacent=dragOffset<0?after:before;if(adjacent!=null)drawImage(c,adjacent,0,1,dragOffset+(dragOffset<0?getWidth():-getWidth()),0,255);}
  else if(previous!=null&&progress<1){if(directSlide){float end=-direction*getWidth(),offset=transitionStart+(end-transitionStart)*progress;drawImage(c,previous,oldAngle,1,offset,0,255);drawImage(c,image,angle,1,offset+direction*getWidth(),0,255);}else{page(c,previous,oldAngle,false);page(c,image,angle,true);}}
  else if(orienting){float t=(float)orientationSpring.value;c.save();c.translate(getWidth()/2f+x,getHeight()/2f+y);c.rotate(oldVisualAngle+(newVisualAngle-oldVisualAngle)*t);float cw=oldContainerWidth+(newContainerWidth-oldContainerWidth)*t,ch=oldContainerHeight+(newContainerHeight-oldContainerHeight)*t;double ar=Math.toRadians(oldVisualAngle+(newVisualAngle-oldVisualAngle)*t);float bw=(float)(Math.abs(image.getWidth()*Math.cos(ar))+Math.abs(image.getHeight()*Math.sin(ar))),bh=(float)(Math.abs(image.getWidth()*Math.sin(ar))+Math.abs(image.getHeight()*Math.cos(ar)));float scale=Math.min(cw/bw,ch/bh)*zoom;c.scale(scale,scale);c.drawBitmap(image,-image.getWidth()/2f,-image.getHeight()/2f,paint);c.restore();}
  else drawImage(c,image,angle,zoom,x,y,255);
 }

 /** Precompute a blurred, dimmed fill using a 64x64 thumbnail, not the full photo. */
 private void updateBackground(Bitmap b){
  blurredBackground=null;
  if(b==null||!settings.bool("blurBackground",false))return;
  try {final int width=64,height=64;
  Bitmap tiny=Bitmap.createScaledBitmap(b,width,height,true);
  int[] px=new int[width*height],tmp=new int[px.length];tiny.getPixels(px,0,width,0,0,width,height);
  for(int pass=0;pass<3;pass++){
   for(int y=0;y<height;y++)for(int x=0;x<width;x++){
    int red=0,green=0,blue=0,count=0;
    for(int k=-3;k<=3;k++){int xx=Math.max(0,Math.min(width-1,x+k));int color=px[y*width+xx];red+=(color>>>16)&255;green+=(color>>>8)&255;blue+=color&255;count++;}
    tmp[y*width+x]=0xff000000|((red/count)<<16)|((green/count)<<8)|(blue/count);
   }
   for(int y=0;y<height;y++)for(int x=0;x<width;x++){
    int red=0,green=0,blue=0,count=0;
    for(int k=-3;k<=3;k++){int yy=Math.max(0,Math.min(height-1,y+k));int color=tmp[yy*width+x];red+=(color>>>16)&255;green+=(color>>>8)&255;blue+=color&255;count++;}
    px[y*width+x]=0xff000000|((red/count)<<16)|((green/count)<<8)|(blue/count);
   }
  }
  blurredBackground=Bitmap.createBitmap(px,width,height,Bitmap.Config.ARGB_8888);
  if(tiny!=b)tiny.recycle();
  }catch(RuntimeException|OutOfMemoryError failure){
   // Pixel reads may throw for hardware-backed or recycled bitmaps.
   blurredBackground=null;
   android.util.Log.w("GalleryFlow","Blurred background disabled for this image",failure);
  }
 }
 private void drawImage(Canvas c,Bitmap b,float rotation,float z,float tx,float ty,int alpha){c.save();c.translate(getWidth()/2f+tx,getHeight()/2f+ty);c.rotate(effectiveAngle(b,rotation));float scale=fit(b,rotation)*z;c.scale(scale,scale);paint.setAlpha(alpha);c.drawBitmap(b,-b.getWidth()/2f,-b.getHeight()/2f,paint);paint.setAlpha(255);c.restore();}
 private void page(Canvas c,Bitmap b,float rotation,boolean incoming){
  float t=progress,p=incoming?1-t:t,sign=direction*(incoming?1:-1),w=getWidth(),h=getHeight(),sx=1,sy=1,tx=0,ty=0,rot=0,alpha=1;String effect=settings.str("transition","Slide");
  switch(effect){
   case "Accordion":sx=Math.max(.02f,1-p);tx=sign*p*w/2;break;
   case "Transparency and scale":sx=sy=1-.25f*p;alpha=1-p;break;
   case "Cube":sx=Math.max(.02f,(float)Math.cos(p*Math.PI/2));tx=sign*p*w*.6f;break;
   case "Depth":sx=sy=1-.35f*p;alpha=1-p;tx=sign*p*w*.2f;break;
   case "Fade":alpha=1-p;break;
   case "Fan":rot=sign*p*55;ty=p*h*.15f;alpha=1-p;break;
   case "Flip":sx=Math.max(.01f,Math.abs((float)Math.cos(p*Math.PI/2)));alpha=1-p;break;
   case "Foreground to background":sx=sy=1+p*.7f;alpha=1-p;break;
   case "Gate":sx=Math.max(.02f,1-p);alpha=1-p;break;
   case "Parallax":tx=sign*p*w*(incoming?.55f:1);alpha=incoming?1:1-p;break;
   case "Rotate down":rot=sign*p*35;ty=p*h;break;
   case "Rotate up":rot=sign*p*35;ty=-p*h;break;
   case "Scale in out":sx=sy=Math.max(.01f,1-p);alpha=1-p;break;
   case "Tablet":sx=1-.35f*p;rot=sign*p*12;tx=sign*p*w;break;
   case "Stack":tx=sign*p*w;sy=1-.1f*p;break;
   default:tx=sign*p*w;
  }
  c.save();c.translate(w/2+tx,h/2+ty);c.rotate(rot);c.scale(sx,sy);c.translate(-w/2,-h/2);drawImage(c,b,rotation,1,0,0,(int)(255*Math.max(0,alpha)));c.restore();
 }
 protected void onDetachedFromWindow(){transitioning=false;Choreographer.getInstance().removeFrameCallback(transitionFrame);stopSpring();Choreographer.getInstance().removeFrameCallback(orientationCallback);orienting=false;if(velocityTracker!=null){velocityTracker.recycle();velocityTracker=null;}if(animator!=null)animator.cancel();previous=null;super.onDetachedFromWindow();}
}
