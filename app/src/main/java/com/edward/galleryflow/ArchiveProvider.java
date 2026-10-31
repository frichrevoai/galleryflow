package com.edward.galleryflow;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.ParcelFileDescriptor;import android.provider.OpenableColumns;import java.io.*;import java.util.*;
public final class ArchiveProvider extends ContentProvider{
 private ArchiveStore store;public boolean onCreate(){store=new ArchiveStore(getContext());return true;}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!mode.equals("r"))throw new FileNotFoundException("ZIP entries are read-only");try{return ParcelFileDescriptor.open(store.materialize(uri),ParcelFileDescriptor.MODE_READ_ONLY);}catch(IOException e){throw new FileNotFoundException(e.getMessage());}}
 public String getType(Uri uri){store.check(uri);return ArchiveStore.mime(Objects.toString(uri.getQueryParameter("entry"),""));}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){store.check(uri);String entry=Objects.toString(uri.getQueryParameter("entry"),"");String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor c=new MatrixCursor(cols);Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(cols[i])?entry.substring(entry.lastIndexOf('/')+1):null;c.addRow(row);return c;}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException("Read-only ZIP");}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException("Read-only ZIP");}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException("Read-only ZIP");}
}
