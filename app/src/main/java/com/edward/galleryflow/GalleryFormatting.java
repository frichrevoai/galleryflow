package com.edward.galleryflow;
import java.text.SimpleDateFormat;import java.util.*;
final class GalleryFormatting {
 static String month(long date){return date<=0?"Undated":new SimpleDateFormat("MMMM yyyy",Locale.ENGLISH).format(new Date(date));}
 static String time(int ms){int seconds=Math.max(0,ms/1000),h=seconds/3600;return h>0?String.format(Locale.US,"%d:%02d:%02d",h,seconds/60%60,seconds%60):String.format(Locale.US,"%d:%02d",seconds/60,seconds%60);}
 static long retentionMillis(int days){return Math.max(1,Math.min(30,days))*86400000L;}
}
