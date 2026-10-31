package com.edward.galleryflow;
import android.content.Context;import android.view.Gravity;import android.widget.*;import java.util.Locale;
/** Controls are overlays; polling ends when detached or explicitly closed. */
final class VideoProgress extends LinearLayout {
 interface Listener {void scrubbing(boolean value);void speedMenu();}
 final SeekBar seek;final TextView elapsed,remaining,speed;private final VideoView video;private final Listener listener;private boolean dragging,closed;
 private final Runnable tick=new Runnable(){public void run(){if(closed)return;refresh();postDelayed(this,250);}};
 VideoProgress(Context c,VideoView video,float rate,Listener listener){super(c);this.video=video;this.listener=listener;setOrientation(VERTICAL);int pad=dp(4);setPadding(dp(12),dp(6),dp(12),dp(6));setBackground(UiTheme.shape(c,UiTheme.surface,20));seek=new SeekBar(c);seek.setMax(10000);seek.setContentDescription("Video playback position");addView(seek,new LayoutParams(-1,dp(28)));LinearLayout labels=new LinearLayout(c);labels.setGravity(Gravity.CENTER_VERTICAL);elapsed=label("0:00");remaining=label("−0:00");speed=label(String.format(Locale.US,"%s×",rate));elapsed.setGravity(Gravity.LEFT);remaining.setGravity(Gravity.RIGHT);speed.setGravity(Gravity.CENTER);speed.setOnClickListener(v->listener.speedMenu());labels.addView(elapsed,new LayoutParams(0,dp(28),1));labels.addView(speed,new LayoutParams(dp(72),dp(28)));labels.addView(remaining,new LayoutParams(0,dp(28),1));addView(labels);
 seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){dragging=true;listener.scrubbing(true);}public void onProgressChanged(SeekBar s,int p,boolean user){if(user&&video.getDuration()>0)elapsed.setText(time((int)(video.getDuration()*(p/10000d))));}public void onStopTrackingTouch(SeekBar s){if(video.getDuration()>0)video.seekTo((int)(video.getDuration()*(s.getProgress()/10000d)));dragging=false;listener.scrubbing(false);refresh();}});post(tick);
 }
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}TextView label(String text){TextView v=new TextView(getContext());v.setText(text);v.setTextColor(UiTheme.text);v.setTextSize(12);return v;}
 static String time(int ms){return GalleryFormatting.time(ms);}

 void refresh(){if(dragging)return;int duration=video.getDuration(),position=video.getCurrentPosition();seek.setEnabled(duration>0);if(duration>0){seek.setProgress((int)(position*10000L/duration));elapsed.setText(time(position));remaining.setText("−"+time(duration-position));}}
 void close(){closed=true;removeCallbacks(tick);}protected void onDetachedFromWindow(){close();super.onDetachedFromWindow();}
}
