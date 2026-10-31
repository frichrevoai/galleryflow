package com.edward.galleryflow;
import android.app.*;import android.content.*;import android.view.*;import android.widget.*;import java.util.*;
final class SettingsPanel{
 interface Actions{void changed();void folders(boolean included);void permissions();}
 final Activity activity;final AppSettings prefs;final Actions actions;
 static final String[] TRANSITIONS={"Slide","Accordion","Transparency and scale","Cube","Depth","Fade","Fan","Flip","Foreground to background","Gate","Parallax","Rotate down","Rotate up","Scale in out","Tablet","Stack"};
 SettingsPanel(Activity a,AppSettings p,Actions act){activity=a;prefs=p;actions=act;}
 void show(){new MenuDialog(activity).setTitle("Settings").setItems(new String[]{"Main","Interface customization","Media viewer","Slideshow"},(d,n)->page(n)).setNegativeButton("Close",null).show();}
 int dp(int v){return Math.round(v*activity.getResources().getDisplayMetrics().density);}
 void page(int page){
  LinearLayout body=new LinearLayout(activity);body.setOrientation(1);body.setPadding(dp(16),dp(8),dp(16),dp(16));ScrollView scroll=new ScrollView(activity);scroll.addView(body);
  if(page==0){
   row(body,"Included folders","Choose library folders to scan; empty means all",()->actions.folders(true));row(body,"Excluded folders","Exclude folders and their subfolders",()->actions.folders(false));
   toggle(body,"Sort digits as numbers","natural",true);choice(body,"Startup screen","startup",new String[]{"Last used","Folders","All media","Photos","Videos","Explorer","Favorites","Trash"},"Folders");

  }else if(page==1){
   section(body,"APPEARANCE");
   choice(body,"Appearance","themeMode",new String[]{"System default","Light","Dark"},"System default");
   section(body,"GRID LABELS");
   toggle(body,"Display file names in grid view","fileNames",false);toggle(body,"Display file names only for videos","videoNamesOnly",false);toggle(body,"Don't show extension","hideExtension",false);
   section(body,"FOLDERS AND THUMBNAILS");
   toggle(body,"Display media count on folders","folderCount",true);toggle(body,"Use square thumbnails for images","squareImages",false);toggle(body,"Use square thumbnails for folders","squareFolders",true);toggle(body,"Use rounded square thumbnails for folders","roundFolders",true);
   numbers(body,"Thumbnail resolution","thumbnailSize",new int[]{192,384,768},384," px");numbers(body,"Font size for folders","folderFont",new int[]{10,12,14,16,18},12," sp");
   section(body,"GALLERY OVERLAYS");
   toggle(body,"Show video duration","videoDuration",true);toggle(body,"Show hero image","hero",true);toggle(body,"Date separators","months",true);choice(body,"Date grouping","dateGroup",new String[]{"Month","Week","Day"},"Day");
  }else if(page==2){
   section(body,"TOOLBAR AND INTERACTIONS");
   toggle(body,"Keep screen on","keepScreen",true);toggle(body,"Automatically hide toolbar","autoHide",true);toggle(body,"Keep images rotated","keepRotation",true);toggle(body,"Delete confirmation in image viewer","confirmDelete",true);toggle(body,"Enable swipe down to close","swipeClose",true);toggle(body,"Enable tap-to-advance","tapAdvance",true);
   section(body,"TRANSITIONS");
   choice(body,"Media viewer transition","transition",TRANSITIONS,"Slide");numbers(body,"Transition speed","transitionMs",new int[]{120,180,260,400,600},260," ms");
   section(body,"DISPLAY AND BACKGROUND");
   toggle(body,"Dimmed blur background to fill the screen","blurBackground",false);
   toggle(body,"Maximum brightness","maxBrightness",false);toggle(body,"Use vertical gesture for custom brightness","brightnessGesture",false);toggle(body,"Remember custom brightness","rememberBrightness",false);
   section(body,"VIDEO PLAYBACK");
   toggle(body,"Loop videos in video player","loopPlayer",true);toggle(body,"Loop videos in image viewer","loopViewer",true);toggle(body,"Auto start videos in image viewer","autoVideo",true);toggle(body,"Use internal video player","internalVideo",true);
   section(body,"ORIENTATION");
   choice(body,"Change screen orientation","orientation",new String[]{"Auto","Portrait","Landscape"},"Auto");toggle(body,"Best Fit · rotate to fit without cropping","bestFit",false);
  }else{choice(body,"Slideshow transition","transition",TRANSITIONS,"Slide");numbers(body,"Transition duration","transitionMs",new int[]{120,180,260,400,600},260," ms");numbers(body,"Slideshow interval","slideSeconds",new int[]{2,3,5,8,10,15,30},5," seconds");toggle(body,"Loop slideshow","slideLoop",true);toggle(body,"Shuffle slideshow","slideShuffle",false);}
  new MenuDialog(activity).setTitle(new String[]{"Main","Interface customization","Media viewer","Slideshow"}[page]).setView(scroll).setPositiveButton("Done",(d,n)->actions.changed()).show();
 }
 void section(LinearLayout body,String title){
  if(body.getChildCount()>0){View divider=new View(activity);divider.setBackgroundColor(UiTheme.line);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.topMargin=dp(14);lp.bottomMargin=dp(9);body.addView(divider,lp);}
  TextView label=new TextView(activity);label.setText(title);label.setTextSize(10);label.setTextColor(UiTheme.muted);label.setTypeface(null,android.graphics.Typeface.BOLD);label.setPadding(dp(10),dp(5),0,dp(6));body.addView(label);
 }
 void toggle(LinearLayout body,String title,String key,boolean def){
  CircleSwitch s=new CircleSwitch(activity,false);s.setText(title);s.setTextColor(UiTheme.text);s.setTextSize(13);s.setPadding(dp(10),dp(8),dp(10),dp(8));s.setBackground(UiTheme.ripple(activity,UiTheme.surface,14));s.setChecked(prefs.bool(key,def));s.setOnCheckedChangeListener((v,on)->prefs.put(key,on));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(3);body.addView(s,lp);
 }
 void row(LinearLayout body,String title,String detail,Runnable click){LinearLayout row=new LinearLayout(activity);row.setOrientation(1);row.setPadding(dp(10),dp(10),dp(10),dp(10));row.setMinimumHeight(dp(52));row.setBackground(UiTheme.ripple(activity,UiTheme.surface,14));TextView t=new TextView(activity);t.setText(title);t.setTextColor(UiTheme.text);t.setTextSize(13);row.addView(t);if(!detail.isEmpty()){TextView sub=new TextView(activity);sub.setText(detail);sub.setTextSize(11);sub.setTextColor(UiTheme.muted);sub.setPadding(0,dp(3),0,0);row.addView(sub);}row.setOnClickListener(v->click.run());body.addView(row);}
 void choice(LinearLayout b,String title,String key,String[] labels,String def){row(b,title,prefs.str(key,def),()->new MenuDialog(activity).setTitle(title).setSingleChoiceItems(labels,Arrays.asList(labels).indexOf(prefs.str(key,def)),(d,n)->{prefs.put(key,labels[n]);d.dismiss();pageRefreshLabel(b,title,labels[n]);if(key.equals("themeMode")){actions.changed();}}).setNegativeButton("Cancel",null).show());}
 void pageRefreshLabel(LinearLayout body,String title,String value){for(int n=0;n<body.getChildCount();n++){android.view.View v=body.getChildAt(n);if(v instanceof LinearLayout){LinearLayout row=(LinearLayout)v;if(row.getChildCount()>1&&row.getChildAt(0) instanceof TextView&&((TextView)row.getChildAt(0)).getText().toString().equals(title))((TextView)row.getChildAt(1)).setText(value);}}}
 void numbers(LinearLayout b,String title,String key,int[] values,int def,String suffix){String[] names=new String[values.length];int current=0;for(int n=0;n<names.length;n++){names[n]=values[n]+suffix;if(values[n]==prefs.num(key,def))current=n;}final int selected=current;row(b,title,prefs.num(key,def)+suffix,()->new MenuDialog(activity).setTitle(title).setSingleChoiceItems(names,selected,(d,n)->{prefs.put(key,values[n]);d.dismiss();pageRefreshLabel(b,title,names[n]);}).setNegativeButton("Cancel",null).show());}
}
