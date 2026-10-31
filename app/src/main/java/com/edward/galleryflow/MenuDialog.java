package com.edward.galleryflow;
import android.app.AlertDialog;import android.content.Context;import android.graphics.Typeface;import android.widget.*;
/** Shared typography, rounded surfaces and compact actions for every app dialog. */
final class MenuDialog extends AlertDialog.Builder {
 private final Context context;
 MenuDialog(Context context){super(context);this.context=context;}
 @Override public AlertDialog.Builder setTitle(CharSequence title){TextView heading=new TextView(context);float d=context.getResources().getDisplayMetrics().density;heading.setText(title);heading.setTextSize(17);heading.setTextColor(UiTheme.text);heading.setTypeface(null,Typeface.BOLD);heading.setPadding((int)(20*d),(int)(16*d),(int)(20*d),(int)(10*d));return setCustomTitle(heading);}
 @Override public AlertDialog create(){AlertDialog dialog=super.create();dialog.setOnShowListener(v->{dialog.getWindow().setBackgroundDrawable(UiTheme.shape(context,UiTheme.surface,24));for(int id:new int[]{-1,-2,-3}){Button b=dialog.getButton(id);if(b!=null){b.setTextSize(12);b.setAllCaps(false);b.setTextColor(UiTheme.accent);b.setBackground(UiTheme.ripple(context,UiTheme.surface,20));}}TextView message=dialog.findViewById(android.R.id.message);if(message!=null){message.setTextSize(13);message.setTextColor(UiTheme.text);}ListView list=dialog.getListView();if(list!=null){list.setDivider(new android.graphics.drawable.ColorDrawable(UiTheme.line));list.setDividerHeight(1);}});return dialog;}
}
