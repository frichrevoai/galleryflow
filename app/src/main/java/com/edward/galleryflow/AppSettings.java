package com.edward.galleryflow;
import android.content.*;
import java.util.*;
final class AppSettings {
 final SharedPreferences p;
 AppSettings(Context c){p=c.getSharedPreferences("gallery-options",0);}
 boolean bool(String k,boolean d){return p.getBoolean(k,d);} int num(String k,int d){return p.getInt(k,d);} String str(String k,String d){return p.getString(k,d);}
 void put(String k,boolean v){p.edit().putBoolean(k,v).apply();} void put(String k,int v){p.edit().putInt(k,v).apply();} void put(String k,String v){p.edit().putString(k,v).apply();}
 Set<String> set(String k){return new HashSet<>(p.getStringSet(k,Collections.emptySet()));}
 void toggle(String k,String v){Set<String>s=set(k);if(!s.add(v))s.remove(v);p.edit().putStringSet(k,s).apply();}
 boolean favorite(MediaRepository.Item i){return set("favorites").contains(i.identity());}
 boolean included(String key){Set<String> in=set("included"),out=set("excluded");for(String s:out)if(under(key,s))return false;if(in.isEmpty())return true;for(String s:in)if(under(key,s))return true;return false;}
 static boolean under(String k,String p){return k.equals(p)||k.startsWith(p+"/")||(p.endsWith(":")&&k.startsWith(p));}
 static int naturalCompare(String a,String b){int i=0,j=0;while(i<a.length()&&j<b.length()){char x=Character.toLowerCase(a.charAt(i)),y=Character.toLowerCase(b.charAt(j));if(Character.isDigit(x)&&Character.isDigit(y)){int si=i,sj=j;while(i<a.length()&&Character.isDigit(a.charAt(i)))i++;while(j<b.length()&&Character.isDigit(b.charAt(j)))j++;String n=a.substring(si,i).replaceFirst("^0+(?!$)",""),m=b.substring(sj,j).replaceFirst("^0+(?!$)","");int cmp=Integer.compare(n.length(),m.length());if(cmp==0)cmp=n.compareTo(m);if(cmp!=0)return cmp;}else{if(x!=y)return x-y;i++;j++;}}return Integer.compare(a.length()-i,b.length()-j);}
 String displayName(String n){if(!bool("hideExtension",false))return n;int dot=n.lastIndexOf('.');return dot>0?n.substring(0,dot):n;}
 void sort(List<MediaRepository.Item> list){sortItems(list,str("sort","date_desc"),bool("natural",true));}
 static void sortItems(List<MediaRepository.Item> list,String order,boolean natural){list.sort((a,b)->{if(a.folder!=b.folder)return a.folder?-1:1;int result;switch(order){case "name":case "name_desc":result=natural?naturalCompare(a.name,b.name):a.name.compareToIgnoreCase(b.name);if(order.equals("name_desc"))result=-result;break;case "size_asc":result=Long.compare(a.size,b.size);break;case "size":result=Long.compare(b.size,a.size);break;case "date_asc":result=Long.compare(a.date,b.date);break;default:result=Long.compare(b.date,a.date);}return result!=0?result:a.identity().compareTo(b.identity());});}
}
