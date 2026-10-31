package com.edward.galleryflow;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.*;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import java.io.InputStream;
import java.util.*;
final class MediaRepository {
 static final class Item {
  final Uri uri;final String name,key;final boolean folder;final float ratio;
  org.json.JSONObject trashRecord;String accessKey;
  boolean video=false,section=false,document=false,archive=false,zip=false,generic=false;String path="",mime="image/*";long date=0,size=0,duration=0;int count=0;
  Item(Uri u,String n,String k,boolean f,float r){uri=u;name=n;key=k;folder=f;ratio=r;}
  String identity(){return folder?key:uri==null?name:uri.toString();}
  static Item heading(String label){Item i=new Item(null,label,"",false,1);i.section=true;return i;}
 }
 private final Context context;private final AppSettings settings;private final BrowseRequest request;
 private void check(){if(request!=null)request.check();}
 private android.os.CancellationSignal signal(){return request==null?null:request.signal;}
 MediaRepository(Context c){this(c,null);}
 MediaRepository(Context c,BrowseRequest request){context=c;settings=new AppSettings(c);this.request=request;}
 List<Item> photos(LockStore locks,boolean admin,String folder){return scan(locks,admin,folder,false,false);}
 List<Item> all(LockStore locks,boolean admin,String folder){return scan(locks,admin,folder,true,true);}
 private List<Item> scan(LockStore locks,boolean admin,String folder,boolean videos,boolean filter){List<Item> out=new ArrayList<>();query(out,locks,admin,folder,false,filter);if(videos)query(out,locks,admin,folder,true,filter);settings.sort(out);return out;}
 private void query(List<Item> out,LockStore locks,boolean admin,String folder,boolean video,boolean filter){
  String[] columns=video?new String[]{"_id","_display_name","relative_path","volume_name","width","height","date_modified","_size","mime_type","duration"}:new String[]{"_id","_display_name","relative_path","volume_name","width","height","date_modified","_size","mime_type"};
  Uri collection=video?MediaStore.Video.Media.EXTERNAL_CONTENT_URI:MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
  try(Cursor c=context.getContentResolver().query(collection,columns,null,null,"date_modified DESC",signal())){
   if(c==null)return;while(c.moveToNext()){check();
    String volume=c.getString(3),relative=Objects.toString(c.getString(2),"");String key=LockStore.pathKey(volume,relative);
    if(folder!=null&&!folder.equals(key)||!admin&&locks.locked(key+"/"+c.getString(1).toLowerCase(Locale.ROOT))||filter&&!settings.included(key))continue;
    Uri uri=ContentUris.withAppendedId(video?MediaStore.Video.Media.getContentUri(volume):MediaStore.Images.Media.getContentUri(volume),c.getLong(0));
    Item item=new Item(uri,c.getString(1),key,false,c.getInt(5)>0?(float)c.getInt(4)/c.getInt(5):1);item.video=video;item.date=c.getLong(6)*1000;item.size=c.getLong(7);item.mime=Objects.toString(c.getString(8),video?"video/*":"image/*");item.duration=video?c.getLong(9):0;item.path=(volume.equals("external_primary")?"Internal storage/":volume+"/")+relative+item.name;out.add(item);
   }
  }catch(SecurityException e){/* Keep whichever collection the user permitted. */}
 }
 List<Item> albums(LockStore locks,boolean admin){return albums(locks,admin,true);}
 List<Item> albums(LockStore locks,boolean admin,boolean filter){return albums(locks,admin,filter,"all");}
 List<Item> albums(LockStore locks,boolean admin,boolean filter,String mediaFilter){
  Map<String,Item> out=new LinkedHashMap<>();
  for(Item p:scan(locks,true,null,true,filter)){if(!matchesFilter(p,mediaFilter))continue;Item f=out.get(p.key);if(f==null){String parent=p.path.substring(0,p.path.lastIndexOf('/'));String name=parent.substring(parent.lastIndexOf('/')+1);f=new Item(admin||!locks.locked(LockStore.itemKey(p))?p.uri:null,name,p.key,true,p.ratio);f.path=parent;f.date=p.date;out.put(p.key,f);}f.count++;f.size+=p.size;}
  List<Item> result=new ArrayList<>(out.values());settings.sort(result);return result;
 }
 List<Item> children(Uri folder,LockStore locks,boolean admin){
  return children(folder,locks,admin,false);
 }
 interface Progress {void update(long done,long total);}
 static boolean matchesFilter(Item i,String filter){return i.folder||i.section||filter.equals("all")||filter.equals("photos")&&!i.video&&!i.generic||filter.equals("videos")&&i.video;}
 List<Item> children(Uri folder,LockStore locks,boolean admin,boolean allFiles){return children(folder,locks,admin,allFiles,null);}
 List<Item> children(Uri folder,LockStore locks,boolean admin,boolean allFiles,Progress progress){check();
  if(ArchiveStore.virtual(folder)){try{return new ArchiveStore(context).list(folder,progress);}catch(java.io.IOException e){throw new IllegalStateException(e.getMessage(),e);}}
  String parent=LockStore.canonical(folder);if(!admin&&locks.locked(parent))return new ArrayList<>();
  List<Item> out=new ArrayList<>();Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(folder,DocumentsContract.getDocumentId(folder));
  try(Cursor c=context.getContentResolver().query(children,new String[]{"document_id","_display_name","mime_type","last_modified","_size"},null,null,null,signal())){
   if(c==null)throw new IllegalStateException("Folder unavailable");int total=c.getCount();while(c.moveToNext()){check();if(progress!=null&&(c.getPosition()%8==0||c.getPosition()==total-1))progress.update(c.getPosition(),total);
    String mime=c.getString(2);boolean dir=DocumentsContract.Document.MIME_TYPE_DIR.equals(mime),video=mime!=null&&mime.startsWith("video/");boolean zip=c.getString(1).toLowerCase(Locale.ROOT).endsWith(".zip");boolean generic=!dir&&!zip&&!video&&(mime==null||!mime.startsWith("image/"));if(generic&&!allFiles)continue;
    Uri uri=DocumentsContract.buildDocumentUriUsingTree(folder,c.getString(0));String key=dir?LockStore.canonical(uri):zip?parent+"/"+c.getString(1).toLowerCase(Locale.ROOT):parent;if(!dir&&!admin&&locks.locked(zip?key:key+"/"+c.getString(1).toLowerCase(Locale.ROOT))&&!allFiles)continue;
    boolean hidden=!admin&&locks.locked(dir||zip?key:key+"/"+c.getString(1).toLowerCase(Locale.ROOT));float ratio=1;if(!hidden&&!dir&&!zip&&!video&&!generic){try(InputStream stream=context.getContentResolver().openInputStream(uri)){BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeStream(stream,null,options);if(options.outHeight>0)ratio=(float)options.outWidth/options.outHeight;}catch(Exception ignored){}}
    Item item=new Item(uri,c.getString(1),key,dir||zip,ratio);item.generic=generic;item.zip=zip;item.video=video;item.document=true;item.date=c.getLong(3);item.size=c.getLong(4);item.mime=Objects.toString(mime,"image/*");item.path=DocumentsContract.getDocumentId(uri).replace("primary:","Internal storage/");
    if(video&&!hidden){try(MediaMetadataRetriever m=new MediaMetadataRetriever()){m.setDataSource(context,uri);item.duration=Long.parseLong(Objects.toString(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION),"0"));}catch(Exception ignored){}}
    out.add(item);
   }
  }settings.sort(out);return out;
 }
 List<Item> roots(){List<Item> out=new ArrayList<>();for(UriPermission p:context.getContentResolver().getPersistedUriPermissions()){check();if(!p.isReadPermission())continue;try{Uri uri=DocumentsContract.buildDocumentUriUsingTree(p.getUri(),DocumentsContract.getTreeDocumentId(p.getUri()));String name=DocumentsContract.getTreeDocumentId(p.getUri());try(Cursor c=context.getContentResolver().query(uri,new String[]{"_display_name"},null,null,null,signal())){if(c!=null&&c.moveToFirst())name=c.getString(0);}catch(Exception ignored){}Item i=new Item(uri,name,LockStore.canonical(uri),true,1);i.document=true;i.path=DocumentsContract.getDocumentId(uri);out.add(i);}catch(Exception ignored){}}out.addAll(new ArchiveStore(context).roots());settings.sort(out);return out;}
}
