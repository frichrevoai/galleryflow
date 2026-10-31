package com.edward.galleryflow;
import android.app.job.*;
public final class TrashCleanup extends JobService{
 public boolean onStartJob(JobParameters p){new Thread(()->{new FileActions(this).expire();jobFinished(p,false);},"trash-expiry").start();return true;}
 public boolean onStopJob(JobParameters p){return true;}
}
