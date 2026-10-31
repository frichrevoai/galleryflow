package com.edward.galleryflow;
import android.content.*;import android.content.res.*;import android.graphics.drawable.*;
final class UiTheme {
 static boolean light; static int bg,surface,text,muted,accent,line,selected;
 static void configure(Context c){String mode=new AppSettings(c).str("themeMode","System default");light=mode.equals("Light")||(mode.equals("System default")&&(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)!=Configuration.UI_MODE_NIGHT_YES);bg=light?0xfff6f7f9:0xff10171d;surface=light?0xffffffff:0xff202830;text=light?0xff20262d:0xffe7ecf0;muted=light?0xff596673:0xff9aa8b4;accent=light?0xff795500:0xfff5c66b;line=light?0xffdce1e7:0xff36434e;selected=light?0xffffe9b8:0xff463b27;}
 static GradientDrawable shape(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius*c.getResources().getDisplayMetrics().density);return d;}
 static RippleDrawable ripple(Context c,int color,int radius){return new RippleDrawable(ColorStateList.valueOf(light?0x18000000:0x22ffffff),shape(c,color,radius),shape(c,0xffffffff,radius));}
}
