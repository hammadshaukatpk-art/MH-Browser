package com.mh.browser;

import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import java.util.Locale;

/** Opens a completed download with an external Android media player.
 * The chooser is outside MH Browser and grants read access to content:// URIs. */
public final class MediaOpenHelper {
 private MediaOpenHelper(){}
 public static String mimeFor(Context context,Uri uri,String storedMime,String name){
  if(storedMime!=null&&!storedMime.trim().isEmpty()&&!"*/*".equals(storedMime))return storedMime;
  String mime=null;
  try{mime=context.getContentResolver().getType(uri);}catch(Exception ignored){}
  if(mime==null&&name!=null){
   int dot=name.lastIndexOf('.');
   if(dot>=0&&dot+1<name.length()){
    mime=MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(dot+1).toLowerCase(Locale.ROOT));
   }
  }
  return mime==null?"video/*":mime;
 }
 public static Intent chooser(Context context,Uri uri,String mime){
  Intent view=new Intent(Intent.ACTION_VIEW);
  view.setDataAndType(uri,mime);
  view.addCategory(Intent.CATEGORY_DEFAULT);
  view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  view.setClipData(ClipData.newRawUri("MH Browser download",uri));
  // Exclude MH Browser's own embedded playback activity: this is an external file player.
  Intent chooser=Intent.createChooser(view,"Open with video or audio player");
  chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
  chooser.setClipData(ClipData.newRawUri("MH Browser download",uri));
  return chooser;
 }
 public static void open(Context context,Uri uri,String mime,String name){
  if(uri==null){Toast.makeText(context,"Downloaded file is unavailable",Toast.LENGTH_LONG).show();return;}
  try{context.startActivity(chooser(context,uri,mimeFor(context,uri,mime,name)));}
  catch(Exception e){Toast.makeText(context,"No external app can play this file",Toast.LENGTH_LONG).show();}
 }
 public static PendingIntent notificationClick(Context context,int requestCode,String uri,String mime,String name){
  Uri file=Uri.parse(uri);
  Intent chooser=chooser(context,file,mimeFor(context,file,mime,name));
  chooser.setAction("com.mh.browser.OPEN_EXTERNAL_DOWNLOAD."+requestCode);
  return PendingIntent.getActivity(context,requestCode,chooser,
    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
}
