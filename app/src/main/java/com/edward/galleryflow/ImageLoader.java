package com.edward.galleryflow;
import android.content.Context;import android.graphics.*;import android.net.Uri;import android.os.*;import android.util.LruCache;import java.util.*;import java.util.concurrent.*;import java.util.function.Consumer;
/** Main-thread request bookkeeping; bounded decode concurrency and demand-first queue. */
final class ImageLoader {
 final Context context;final Handler main=new Handler(Looper.getMainLooper());
 final ThreadPoolExecutor pool=new ThreadPoolExecutor(2,2,0,TimeUnit.MILLISECONDS,new PriorityBlockingQueue<Runnable>());
 final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(CacheBudget.images(Runtime.getRuntime().maxMemory())){protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
 final Map<String,Task> pending=new HashMap<>();int epoch;long sequence;
 ImageLoader(Context c){context=c.getApplicationContext();}
 void load(Uri uri,Consumer<Bitmap> done){request(uri,done,false);}
 void prefetch(Uri uri,Consumer<Bitmap> done){request(uri,done,true);}
 void request(Uri uri,Consumer<Bitmap> done,boolean preload){String key=uri.toString();if(!preload){for(Runnable queued:pool.getQueue().toArray(new Runnable[0])){Task task=(Task)queued;if(!task.key.equals(key)&&pool.remove(task)){pending.remove(task.key,task);task.listeners.clear();}}}Bitmap hit=cache.get(key);if(hit!=null){done.accept(hit);return;}Task task=pending.get(key);if(task!=null){task.listeners.add(done);if(!preload&&task.priority!=0&&pool.remove(task)){task.priority=0;pool.execute(task);}return;}if(preload&&pool.getQueue().size()>=2)return;task=new Task(uri,preload?1:0);task.listeners.add(done);pending.put(key,task);pool.execute(task);}
 final class Task implements Runnable,Comparable<Task>{final Uri uri;final String key;final int token=epoch;final long order=sequence++;int priority;final List<Consumer<Bitmap>> listeners=new ArrayList<>();Task(Uri u,int p){uri=u;key=u.toString();priority=p;}public int compareTo(Task other){int p=Integer.compare(priority,other.priority);return p!=0?p:Long.compare(order,other.order);}public void run(){Bitmap result=null;try{result=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),uri),(d,info,s)->{int w=info.getSize().getWidth(),h=info.getSize().getHeight();float scale=Math.min(1f,2048f/Math.max(w,h));d.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);d.setTargetSize(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));});}catch(Exception|OutOfMemoryError ignored){}Bitmap loaded=result;main.post(()->{if(token!=epoch)return;pending.remove(key,this);if(loaded!=null)cache.put(key,loaded);List<Consumer<Bitmap>> callbacks=new ArrayList<>(listeners);listeners.clear();for(Consumer<Bitmap> callback:callbacks)callback.accept(loaded);});}}
 void clear(){epoch++;pool.getQueue().clear();for(Task task:pending.values())task.listeners.clear();pending.clear();cache.evictAll();}
 void close(){clear();pool.shutdownNow();}
}
