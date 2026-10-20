package com.mh.browser;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import com.yausername.youtubedl_android.mapper.VideoInfo;
import com.yausername.ffmpeg.FFmpeg;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Inspects public, unprotected media URLs and downloads user-selected formats. */
public final class MediaDownloadEngine {
 private MediaDownloadEngine(){}
 public static final class Choice {
  public final String label;
  public final int height;
  public final String audioFormat;
  public Choice(String label,int height,String audioFormat){
   this.label=label;this.height=height;this.audioFormat=audioFormat;
  }
 }
 public interface Callback {
  void ready(String title,List<Choice> choices);
  void progress(int percent,String message);
  void completed(String displayName,String uri,String mime);
  void failed(String message);
 }
 public static void inspect(Context context,String url,Callback callback){
  new Thread(()->{
   boolean acquired=false;
   try{
    MediaEngineUpdater.acquireUse(context);acquired=true;
    YoutubeDLRequest infoRequest=new YoutubeDLRequest(url);
    infoRequest.addOption("--no-playlist");
    VideoInfo info=YoutubeDL.getInstance().getInfo(infoRequest);
    Set<Integer> heights=new HashSet<>();
    List<VideoFormat> formats=info.getFormats();
    if(formats!=null)for(VideoFormat f:formats){
     String codec=f.getVcodec();
     int h=f.getHeight();
     if(h>=144&&h<=4320&&codec!=null&&!codec.equals("none"))heights.add(h);
    }
    List<Integer> sorted=new ArrayList<>(heights);
    Collections.sort(sorted,Collections.reverseOrder());
    List<Choice> choices=new ArrayList<>();
    for(int h:sorted)choices.add(new Choice("Video • up to "+h+"p (MP4 preferred)",h,null));
    if(choices.isEmpty())choices.add(new Choice("Video • best available",0,null));
    choices.add(new Choice("Audio • MP3 (192 kbps target) ",0,"mp3"));
    choices.add(new Choice("Audio • M4A (best available)",0,"m4a"));
    String title=info.getTitle();
    callback.ready(title==null?"Available formats":title,choices);
   }catch(Exception ex){callback.failed("Could not inspect formats: "+safeError(ex));}
   finally{if(acquired)MediaEngineUpdater.releaseUse();}
  },"mh-media-inspect").start();
 }
 public static void download(Context context,String url,Choice choice,Callback callback){
  new Thread(()->{
   File staging=null;
   boolean acquired=false;
   try{
    callback.progress(0,"Preparing latest media extractor…");
    MediaEngineUpdater.acquireUse(context);acquired=true;
    callback.progress(0,"Connecting to media source…");
    FFmpeg.getInstance().init(context.getApplicationContext());
    File root=context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    if(root==null)throw new IllegalStateException("Download storage is unavailable");
    staging=new File(root,"mh-working-"+UUID.randomUUID());
    if(!staging.mkdirs())throw new IllegalStateException("Could not create download directory");
    YoutubeDLRequest req=new YoutubeDLRequest(url);
    req.addOption("--no-playlist");
    req.addOption("--no-mtime");
    // Fragmented streams can be fetched in parallel where supported by the provider.
    req.addOption("--concurrent-fragments","4");
    req.addOption("--retries","5");
    req.addOption("--fragment-retries","5");
    req.addOption("--socket-timeout","25");
    req.addOption("--restrict-filenames");
    req.addOption("-o",new File(staging,"%(title).90B-%(id)s.%(ext)s").getAbsolutePath());
    if(choice.audioFormat!=null){
     req.addOption("-f","bestaudio/best");
     req.addOption("-x");
     req.addOption("--audio-format",choice.audioFormat);
     if("mp3".equals(choice.audioFormat))req.addOption("--audio-quality","192K");
    }else{
     String quality=choice.height>0?"[height<="+choice.height+"]":"";
     // Prefer AVC/H.264 video and AAC/M4A audio: most Android devices decode these reliably.
     // Fall back only when the provider does not offer an Android-friendly format.
     req.addOption("-f",
      "bestvideo"+quality+"[vcodec^=avc1][ext=mp4]+bestaudio[ext=m4a]/"+
      "best"+quality+"[ext=mp4][vcodec^=avc1]/"+
      "bestvideo"+quality+"[ext=mp4]+bestaudio[ext=m4a]/"+
      "best"+quality+"[ext=mp4]/"+
      "bestvideo"+quality+"+bestaudio/best"+quality);
     req.addOption("--merge-output-format","mp4");
    }
    String processId="mh-"+UUID.randomUUID();
    YoutubeDL.getInstance().execute(req,processId,false,(percent,eta,line)->{
     callback.progress(Math.max(0,Math.min(100,Math.round(percent))),line==null?"Downloading media":line);
     return kotlin.Unit.INSTANCE;
    });
    File[] files=staging.listFiles();
    File result=null;int bestScore=-1;
    if(files!=null)for(File f:files){
     if(!f.isFile()||f.length()==0)continue;
     String lower=f.getName().toLowerCase(java.util.Locale.ROOT);
     if(lower.endsWith(".part")||lower.endsWith(".ytdl")||lower.endsWith(".temp")||lower.endsWith(".json"))continue;
     int score;
     if("mp3".equals(choice.audioFormat))score=lower.endsWith(".mp3")?100:0;
     else if("m4a".equals(choice.audioFormat))score=lower.endsWith(".m4a")?100:0;
     else score=lower.endsWith(".mp4")?100:lower.endsWith(".mkv")?85:lower.endsWith(".webm")?80:0;
     if(score==0)continue;
     if(score>bestScore||(score==bestScore&&result!=null&&f.length()>result.length())){
      result=f;bestScore=score;
     }
    }
    if(result==null)throw new IllegalStateException("No playable completed media file was produced");
    // yt-dlp may sometimes report success with an audio-only file on video-only requests.
    // Never label such a file as a successfully downloaded video.
    if(choice.audioFormat==null){
     android.media.MediaMetadataRetriever check=new android.media.MediaMetadataRetriever();
     try{
      check.setDataSource(result.getAbsolutePath());
      String hasVideo=check.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
      if(!"yes".equalsIgnoreCase(hasVideo))
       throw new IllegalStateException("The website returned audio only, not a playable video. Try a different video quality.");
     }finally{try{check.release();}catch(Exception ignored){}}
    }
    String lowerName=result.getName().toLowerCase(java.util.Locale.ROOT);
    String mime=lowerName.endsWith(".mp3")?"audio/mpeg":
      lowerName.endsWith(".m4a")?"audio/mp4":
      lowerName.endsWith(".webm")?"video/webm":
      lowerName.endsWith(".mkv")?"video/x-matroska":"video/mp4";
    Uri saved=publish(context,result,mime);
    callback.completed(result.getName(),saved.toString(),mime);
   }catch(Exception ex){callback.failed("Download failed. "+safeError(ex));}
   finally{if(staging!=null)deleteTree(staging);if(acquired)MediaEngineUpdater.releaseUse();}
  },"mh-media-download").start();
 }
 private static Uri publish(Context context,File source,String mime)throws Exception{
  if(Build.VERSION.SDK_INT<29){
   // Android 8/9: keep the file in the app's private external downloads folder.
   File dest=new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),source.getName());
   copyFile(source,dest);
   return androidx.core.content.FileProvider.getUriForFile(context,context.getPackageName()+".fileprovider",dest);
  }
  ContentValues values=new ContentValues();
  values.put(MediaStore.MediaColumns.DISPLAY_NAME,source.getName());
  values.put(MediaStore.MediaColumns.MIME_TYPE,mime);
  values.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/MH Browser");
  values.put(MediaStore.MediaColumns.IS_PENDING,1);
  Uri uri=context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
  if(uri==null)throw new IllegalStateException("Unable to save to Downloads");
  try{
   try(FileInputStream in=new FileInputStream(source);
       OutputStream out=context.getContentResolver().openOutputStream(uri)){
    if(out==null)throw new IllegalStateException("Unable to open Downloads storage");
    byte[] buffer=new byte[32768];int count;
    while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
   }
   ContentValues done=new ContentValues();
   done.put(MediaStore.MediaColumns.IS_PENDING,0);
   context.getContentResolver().update(uri,done,null,null);
   return uri;
  }catch(Exception ex){context.getContentResolver().delete(uri,null,null);throw ex;}
 }
 private static void copyFile(File src,File dest)throws Exception{
  try(FileInputStream in=new FileInputStream(src);java.io.FileOutputStream out=new java.io.FileOutputStream(dest)){
   byte[] buffer=new byte[32768];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
  }
 }
 private static void deleteTree(File f){
  if(f.isDirectory()){File[] children=f.listFiles();if(children!=null)for(File c:children)deleteTree(c);}
  f.delete();
 }
 private static String safeError(Exception e){
  String message=e.getMessage();
  if(message==null||message.isEmpty())return e.getClass().getSimpleName();
  message=message.replaceAll("(?i)(cookie|authorization|token)=[^\\s&]+","$1=[hidden]");
  // yt-dlp often writes stale-version warnings before the actual fatal error.
  // Surface the last ERROR line instead of misleading users with the warning.
  String[] lines=message.split("\\r?\\n");
  String actual=null;
  for(String line:lines)if(line.trim().startsWith("ERROR:"))actual=line.trim();
  if(actual!=null)message=actual;
  return message.length()>600?message.substring(message.length()-600):message;
 }
}
