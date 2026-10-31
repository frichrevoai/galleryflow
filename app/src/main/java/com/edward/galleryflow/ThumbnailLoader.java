package com.edward.galleryflow;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.Size;
import java.util.*;
import java.util.concurrent.*;

final class ThumbnailLoader {
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService workers=Executors.newFixedThreadPool(3);
    private final Set<String> pending=new HashSet<>();private final Map<String,Long> failedUntil=new HashMap<>();
    private int generation=0; private final Map<String,Set<Runnable>> listeners=new HashMap<>();
    private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(CacheBudget.thumbnails(Runtime.getRuntime().maxMemory())) {
        protected int sizeOf(String key,Bitmap b){return b.getAllocationByteCount();}
    };
    ThumbnailLoader(Context c){context=c.getApplicationContext();}
    Bitmap get(MediaRepository.Item item,int size,Runnable ready){if(item.uri==null||!ArchiveStore.admin&&new LockStore(context).locked(LockStore.itemKey(item)))return null;boolean cover=item.zip||item.archive&&item.folder;if(!cover&&(item.generic||item.document&&item.folder))return null;return getInternal(item.uri,size,ready,cover?item:null);}
    Bitmap get(Uri uri,int size,Runnable ready) {return getInternal(uri,size,ready,null);}
    private Bitmap getInternal(Uri uri,int size,Runnable ready,MediaRepository.Item cover) {
        if(uri==null)return null;
        String key=uri.toString()+"@"+size+(cover==null?"":"@cover:"+cover.date+":"+cover.size);
        Bitmap bitmap=cache.get(key);if(bitmap!=null)return bitmap;
        Long retryTime=failedUntil.get(key);if(retryTime!=null){if(android.os.SystemClock.uptimeMillis()<retryTime)return null;failedUntil.remove(key);} if(pending.contains(key)){listeners.computeIfAbsent(key,k->new HashSet<>()).add(ready);return null;} if(pending.size()>=48)return null;pending.add(key);listeners.computeIfAbsent(key,k->new HashSet<>()).add(ready);
        int token=generation;
        workers.execute(()->{
            Bitmap loaded=null;Uri sourceUri=uri;
            if(cover!=null){try{sourceUri=new ArchiveStore(context).cover(cover);}catch(Exception ignored){sourceUri=null;}}
            final Uri resolved=sourceUri;
            try {if(resolved==null)throw new IllegalArgumentException("No cover image");if(size>=768&&Objects.toString(context.getContentResolver().getType(resolved),"").startsWith("image/")){loaded=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),resolved),(decoder,info,source)->{int w=info.getSize().getWidth(),h=info.getSize().getHeight();float scale=Math.min(1f,(float)size/Math.max(w,h));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);decoder.setTargetSize(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));});}else loaded=context.getContentResolver().loadThumbnail(resolved,new Size(size,size),null);}
            catch(Exception|OutOfMemoryError e){
                try { if(Objects.toString(context.getContentResolver().getType(resolved),"").startsWith("video/")){try(android.media.MediaMetadataRetriever retriever=new android.media.MediaMetadataRetriever()){retriever.setDataSource(context,resolved);loaded=retriever.getScaledFrameAtTime(0,android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC,size,size);}}else loaded=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),resolved),(decoder,info,source)->{
                    int w=info.getSize().getWidth(),h=info.getSize().getHeight();
                    float scale=Math.min(1f,(float)size/Math.max(w,h));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);decoder.setTargetSize(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));
                }); }catch(Exception|OutOfMemoryError ignored){}
            }
            Bitmap result=loaded;
            main.post(()->{if(token!=generation)return;pending.remove(key);if(result!=null){cache.put(key,result);failedUntil.remove(key);}else{failedUntil.put(key,android.os.SystemClock.uptimeMillis()+7000L);if(listeners.containsKey(key))main.postDelayed(ready,7100L);}Set<Runnable> callbacks=listeners.remove(key);if(callbacks!=null)for(Runnable callback:callbacks)callback.run();});
        });return null;
    }
    void clear(){generation++;cache.evictAll();pending.clear();failedUntil.clear();listeners.clear();}
    void close(){clear();workers.shutdownNow();}
}
