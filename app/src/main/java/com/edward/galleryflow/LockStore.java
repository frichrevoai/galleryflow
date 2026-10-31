package com.edward.galleryflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class LockStore {
    private final SharedPreferences prefs;
    LockStore(Context c) { prefs=c.getSharedPreferences("folder-security",Context.MODE_PRIVATE); }
    boolean configured() {return prefs.contains("hash");}
    static String canonical(Uri uri) {
        String id;
        try { id=DocumentsContract.getDocumentId(uri); } catch(Exception e) { id=DocumentsContract.getTreeDocumentId(uri); }
        if("com.android.externalstorage.documents".equals(uri.getAuthority())) {
            int colon=id.indexOf(':');
            if(colon>=0) return pathKey(id.substring(0,colon),id.substring(colon+1));
        }
        return "doc:"+uri.getAuthority()+":"+id;
    }
    static String pathKey(String volume,String path) {
        String v=volume.equals("external_primary")||volume.equals("external")?"primary":volume;
        String p=path.replace('\\','/');
        while(p.startsWith("/")) p=p.substring(1);
        while(p.endsWith("/")) p=p.substring(0,p.length()-1);
        return "fs:"+v.toLowerCase(Locale.ROOT)+":"+p.toLowerCase(Locale.ROOT);
    }
    Set<String> keys() {return new HashSet<>(prefs.getStringSet("locks",Collections.emptySet()));}
    static String itemKey(MediaRepository.Item i){return i.accessKey!=null?i.accessKey:i.folder?i.key:i.key+"/"+i.name.toLowerCase(java.util.Locale.ROOT);}
    boolean locked(String key) {
        for(String p:keys()) if(key.equals(p)||key.startsWith(p+"/")||(p.endsWith(":")&&key.startsWith(p))) return true;
        return false;
    }
    String label(String key) {return prefs.getString("label:"+key,key);}
    void setLocked(String key,String label,boolean locked) {
        Set<String> s=keys();if(locked)s.add(key);else s.remove(key);
        prefs.edit().putStringSet("locks",s).putString("label:"+key,label).commit();
    }
    private byte[] derive(char[] password,byte[] salt) throws Exception {
        PBEKeySpec spec=new PBEKeySpec(password,salt,210000,256);
        try {return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        finally {spec.clearPassword();}
    }
    synchronized void setup(char[] password) throws Exception {
        if(password.length<4)throw new IllegalArgumentException("Use at least 4 characters");
        byte[] salt=new byte[32];new SecureRandom().nextBytes(salt);
        byte[] hash=derive(password,salt);
        if(!prefs.edit().putString("salt",Base64.encodeToString(salt,Base64.NO_WRAP)).putString("hash",Base64.encodeToString(hash,Base64.NO_WRAP)).putInt("failures",0).putLong("retry",0).commit()) throw new Exception("Could not save password");
    }
    long waitSeconds() {return Math.max(0,(prefs.getLong("retry",0)-System.currentTimeMillis()+999)/1000);}
    synchronized boolean verify(char[] password) throws Exception {
        if(waitSeconds()>0)return false;
        boolean ok=MessageDigest.isEqual(derive(password,Base64.decode(prefs.getString("salt",""),Base64.DEFAULT)),Base64.decode(prefs.getString("hash",""),Base64.DEFAULT));
        int failures=ok?0:prefs.getInt("failures",0)+1;
        long delay=failures<5?0:Math.min(300,30L*(1L<<Math.min(4,failures-5)))*1000;
        prefs.edit().putInt("failures",failures).putLong("retry",ok?0:System.currentTimeMillis()+delay).commit();return ok;
    }
}
