package com.mh.browser;

import android.content.Context;
import android.content.SharedPreferences;
import com.yausername.youtubedl_android.YoutubeDL;

/** Best-effort background extractor update checks. Updates depend on network and upstream releases. */
public final class MediaEngineUpdater {
 private MediaEngineUpdater(){}
 private static volatile boolean running=false;
 private static final long CHECK_INTERVAL=24L*60*60*1000;
 public interface Listener{void finished(String status);}
 public static void check(Context context){update(context,false,null);}
 public static synchronized void update(Context context,boolean force,Listener listener){
  Context app=context.getApplicationContext();
  SharedPreferences prefs=app.getSharedPreferences("mh",0);
  long now=System.currentTimeMillis();
  if(running){if(listener!=null)listener.finished("An update check is already running.");return;}
  if(!force&&now-prefs.getLong("mediaEngineLastCheck",0)<CHECK_INTERVAL)return;
  running=true;
  // Record attempt before starting to prevent a failed connection from causing an update storm.
  prefs.edit().putLong("mediaEngineLastCheck",now).apply();
  new Thread(()->{
   String message;
   try{
    YoutubeDL engine=YoutubeDL.getInstance();
    engine.init(app);
    Object result=engine.updateYoutubeDL(app,
     new YoutubeDL.UpdateChannel("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"));
    message="Media engine update check: "+String.valueOf(result);
    prefs.edit().putString("mediaEngineLastResult",message).apply();
   }catch(Exception e){
    message="Media engine update unavailable: "+String.valueOf(e.getMessage());
    prefs.edit().putString("mediaEngineLastResult",message).apply();
   }finally{running=false;}
   if(listener!=null)listener.finished(message);
  },"mh-extractor-auto-update").start();
 }
}
