package com.mh.browser;

import android.app.*;
import android.content.*;
import android.os.*;
import android.graphics.Color;
import org.json.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;

public class MediaDownloadService extends Service {
 public static final String CHANNEL="mh_media_downloads_v1";
 private static final int FOREGROUND_ID=101;
 private final AtomicInteger active=new AtomicInteger();
 private NotificationManager manager;
 private long lastWrite=0;
 @Override public void onCreate(){
  super.onCreate();
  manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26){
   NotificationChannel c=new NotificationChannel(CHANNEL,"Media downloads",NotificationManager.IMPORTANCE_LOW);
   c.setDescription("Download progress and completion for MH Browser");
   manager.createNotificationChannel(c);
  }
 }
 public static void enqueue(Context context,String url,MediaDownloadEngine.Choice choice){
  Intent intent=new Intent(context,MediaDownloadService.class);
  intent.putExtra("url",url);intent.putExtra("label",choice.label);
  intent.putExtra("height",choice.height);intent.putExtra("audio",choice.audioFormat);
  intent.putExtra("task",UUID.randomUUID().toString());
  if(Build.VERSION.SDK_INT>=26)context.startForegroundService(intent);
  else context.startService(intent);
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent==null)return START_NOT_STICKY;
  String url=intent.getStringExtra("url"),task=intent.getStringExtra("task");
  if(url==null||task==null)return START_NOT_STICKY;
  String label=intent.getStringExtra("label");
  MediaDownloadEngine.Choice choice=new MediaDownloadEngine.Choice(label==null?"Media":label,
   intent.getIntExtra("height",0),intent.getStringExtra("audio"));
  int id=notificationId(task);
  active.incrementAndGet();
  // Android requires foreground service promotion promptly after startForegroundService.
  startForeground(FOREGROUND_ID,notification("Downloading media",choice.label,0,true,false));
  save(task,choice.label,"Downloading",0,"Starting…",null,null);
  manager.notify(id,notification("MH Browser • Downloading",choice.label,0,true,false));
  MediaDownloadEngine.download(getApplicationContext(),url,choice,new MediaDownloadEngine.Callback(){
   @Override public void ready(String title,java.util.List<MediaDownloadEngine.Choice> choices){}
   @Override public void progress(int percent,String line){
    long now=SystemClock.elapsedRealtime();
    if(now-lastWrite<650&&percent<100)return;
    lastWrite=now;
    String speed=extractSpeed(line);
    String info=percent+"%"+(speed.isEmpty()?"":" • "+speed);
    save(task,choice.label,"Downloading",percent,info,null,null);
    manager.notify(id,notification("MH Browser • Downloading",info,percent,true,false));
    manager.notify(FOREGROUND_ID,notification("Downloading media",info,percent,true,false));
   }
   @Override public void completed(String name,String uri,String mime){
    save(task,name,"Completed",100,"Download complete",uri,mime);
    manager.notify(id,notification("Download complete",name,100,false,false));
    finish(startId);
   }
   @Override public void failed(String error){
    save(task,choice.label,"Failed",0,error,null,null);
    manager.notify(id,notification("Download failed",shortText(error),0,false,true));
    finish(startId);
   }
  });
  return START_NOT_STICKY;
 }
 private synchronized void finish(int startId){
  if(active.decrementAndGet()<=0){
   stopForeground(STOP_FOREGROUND_REMOVE);
   stopSelfResult(startId);
  }
 }
 private static int notificationId(String task){return 2000+(task.hashCode()&0x3fffffff);}
 private static String extractSpeed(String line){
  if(line==null)return "";
  Matcher m=Pattern.compile("([0-9]+(?:\\.[0-9]+)?\\s*(?:KiB|MiB|GiB|KB|MB|GB)/s)",Pattern.CASE_INSENSITIVE).matcher(line);
  return m.find()?m.group(1):"";
 }
 private static String shortText(String s){return s==null?"Unknown error":s.length()>100?s.substring(0,100):s;}
 private Notification notification(String title,String message,int percent,boolean ongoing,boolean failed){
  Intent open=new Intent(this,BrowserActivity.class);
  open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  open.putExtra("openDownloads",true);
  PendingIntent click=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
  b.setSmallIcon(android.R.drawable.stat_sys_download)
   .setContentTitle(title).setContentText(shortText(message))
   .setContentIntent(click).setAutoCancel(!ongoing).setOngoing(ongoing)
   .setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE);
  if(ongoing)b.setProgress(100,percent,percent<=0);
  else b.setSmallIcon(failed?android.R.drawable.stat_notify_error:android.R.drawable.stat_sys_download_done);
  return b.build();
 }
 private void save(String task,String name,String state,int percent,String detail,String uri,String mime){
  synchronized(MediaDownloadService.class){
   android.content.SharedPreferences p=getSharedPreferences("mh",0);
   JSONArray a;try{a=new JSONArray(p.getString("downloads","[]"));}catch(Exception ex){a=new JSONArray();}
   JSONObject record=null;
   for(int i=0;i<a.length();i++){
    JSONObject item=a.optJSONObject(i);
    if(item!=null&&task.equals(item.optString("task"))){record=item;break;}
   }
   if(record==null){record=new JSONObject();a.put(record);}
   try{
    record.put("id",-2);record.put("task",task);record.put("name",name);
    if(!record.has("time"))record.put("time",System.currentTimeMillis());
    record.put("state",state);record.put("progress",percent);record.put("detail",detail);
    if(uri!=null)record.put("uri",uri);
    if(mime!=null)record.put("mime",mime);
    p.edit().putString("downloads",a.toString()).commit();
   }catch(Exception ignored){}
  }
 }
 @Override public android.os.IBinder onBind(Intent i){return null;}
}
