package com.edward.galleryflow;
import android.os.CancellationSignal;import android.os.OperationCanceledException;
/** A cancellation flag remains observable even if a provider ignores interruption. */
final class BrowseRequest {
 final CancellationSignal signal=new CancellationSignal();volatile boolean cancelled;
 void check(){if(cancelled||Thread.currentThread().isInterrupted())throw new OperationCanceledException();}
 void markCancelled(){cancelled=true;}
}
