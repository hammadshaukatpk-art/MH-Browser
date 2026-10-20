package com.mh.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Firefox GeckoView browsing engine. Browser chrome stays native and uses MH Browser colors. */
public final class GeckoBrowserActivity extends Activity {
 private static final int BG=0xff071326,PANEL=0xff10223a,BLUE=0xff269cff,WHITE=Color.WHITE,GRAY=0xffa4b6ce;
 private static GeckoRuntime runtime;
 private final List<GeckoSession> tabs=new ArrayList<>();
 private final Map<GeckoSession,String> urls=new LinkedHashMap<>();
 private final Map<GeckoSession,String> titles=new LinkedHashMap<>();
 private final Map<GeckoSession,List<Media>> detected=new LinkedHashMap<>();
 private GeckoView view;
 private EditText address;
 private LinearLayout root,top,bottom;
 private TextView tabCounter,downloadButton;
 private int selected=-1;
 private boolean canBack,canForward,privateTab,fullScreen;
 private SharedPreferences prefs;
 private long lastPlayerTap;
 private static final class Media {
  final String url,referer; Media(String u,String r){url=u;referer=r;}
 }
 private int dp(int v){return (int)(getResources().getDisplayMetrics().density*v+0.5f);}
 private TextView label(String text,int size,int color){
  TextView t=new TextView(this);t.setText(text);t.setTextColor(color);t.setTextSize(size);
  t.setGravity(Gravity.CENTER_VERTICAL);return t;
 }
 private TextView button(String text,Runnable callback){
  TextView t=label(text,21,WHITE);t.setGravity(Gravity.CENTER);
  t.setOnClickListener(v->callback.run());return t;
 }
 private void action(LinearLayout row,String text,Runnable run){
  row.addView(button(text,run),new LinearLayout.LayoutParams(0,dp(54),1));
 }
 private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_SHORT).show();}
 private GeckoSession current(){return selected>=0&&selected<tabs.size()?tabs.get(selected):null;}
 private String currentUrl(){return current()==null?"":urls.getOrDefault(current(),"");}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  prefs=getSharedPreferences("mh",0);
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
  setContentView(root);
  if(Build.VERSION.SDK_INT>=35)root.setOnApplyWindowInsetsListener((v,insets)->{
   android.graphics.Insets x=insets.getInsets(WindowInsets.Type.systemBars());
   v.setPadding(x.left,x.top,x.right,x.bottom);return insets;
  });
  top=new LinearLayout(this);top.setPadding(dp(8),dp(8),dp(8),dp(8));top.setBackgroundColor(BG);
  root.addView(top,new LinearLayout.LayoutParams(-1,dp(66)));
  top.addView(button("⌂",()->navigate("https://www.google.com")),new LinearLayout.LayoutParams(dp(44),-1));
  address=new EditText(this);address.setSingleLine(true);address.setTextColor(WHITE);
  address.setHintTextColor(GRAY);address.setHint("Search or enter website");
  address.setTextSize(15);address.setPadding(dp(12),0,dp(8),0);
  address.setBackgroundColor(PANEL);address.setImeOptions(EditorInfo.IME_ACTION_GO);
  top.addView(address,new LinearLayout.LayoutParams(0,-1,1));
  address.setOnEditorActionListener((v,a,event)->{navigate(address.getText().toString());return true;});
  top.addView(button("↻",()->{if(current()!=null)current().reload();}),new LinearLayout.LayoutParams(dp(42),-1));
  view=new GeckoView(this);view.setBackgroundColor(WHITE);
  root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
  bottom=new LinearLayout(this);bottom.setBackgroundColor(PANEL);
  root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(60)));
  action(bottom,"‹",()->{if(canBack&&current()!=null)current().goBack();});
  action(bottom,"›",()->{if(canForward&&current()!=null)current().goForward();});
  downloadButton=button("↓",this::downloads);
  bottom.addView(downloadButton,new LinearLayout.LayoutParams(0,dp(54),1));
  tabCounter=button("▣",this::tabPicker);
  bottom.addView(tabCounter,new LinearLayout.LayoutParams(0,dp(54),1));
  action(bottom,"☰",this::menu);
  if(runtime==null){
   ContentBlocking.Settings blocking=new ContentBlocking.Settings.Builder()
    .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
    .strictSocialTrackingProtection(true).queryParameterStrippingEnabled(true).build();
   runtime=GeckoRuntime.create(getApplicationContext(),
    new GeckoRuntimeSettings.Builder().contentBlocking(blocking).build());
  }
  runtime.getWebExtensionController()
   .ensureBuiltIn("resource://android/assets/mh_adblock/","mh-adblock@mh-browser")
   .accept(extension->{
     extension.setMessageDelegate(new WebExtension.MessageDelegate(){
       @Override public GeckoResult<Object> onMessage(String nativeApp,Object message,WebExtension.MessageSender sender){
        onExtensionMessage(message,sender);return GeckoResult.fromValue(null);
       }
      },"mh_browser");
   },error->android.util.Log.e("MHGecko","Ad extension installation failed",error));
  MediaEngineUpdater.check(getApplicationContext());
  newTab("https://www.google.com",false);
 }
 private void onExtensionMessage(Object raw,WebExtension.MessageSender sender){
  try{
   JSONObject m=raw instanceof JSONObject?(JSONObject)raw:new JSONObject(String.valueOf(raw));
   String type=m.optString("type");
   if("playerTap".equals(type)){lastPlayerTap=SystemClock.elapsedRealtime();return;}
   if(!"media".equals(type))return;
   String u=m.optString("url"),ref=m.optString("referer");
   if(!u.startsWith("http")||u.length()>4096)return;
   runOnUiThread(()->{
    GeckoSession target=current();
    for(GeckoSession session:tabs){
     String page=urls.getOrDefault(session,"");
     if(ref.equals(page)||(!ref.isEmpty()&&sameHost(ref,page))){target=session;break;}
    }
    if(target==null)return;
    List<Media> list=detected.get(target);
    if(list==null)return;
    for(Media item:list)if(item.url.equals(u))return;
    if(list.size()>=60)list.remove(0);
    list.add(new Media(u,ref));
    if(target==current())downloadButton.setText("↓●");
   });
  }catch(Exception ignored){}
 }
 private boolean sameHost(String a,String b){
  try{return Uri.parse(a).getHost().equalsIgnoreCase(Uri.parse(b).getHost());}
  catch(Exception e){return false;}
 }
 private void navigate(String input){
  String url=input.trim();if(url.isEmpty()||current()==null)return;
  if(!url.startsWith("https://")&&!url.startsWith("http://"))
   url=url.contains(".")&&!url.contains(" ")?"https://"+url:"https://www.google.com/search?q="+Uri.encode(url);
  current().loadUri(url);address.clearFocus();
 }
 private void newTab(String url,boolean incognito){
  GeckoSession session=new GeckoSession(new GeckoSessionSettings.Builder()
   .usePrivateMode(incognito).useTrackingProtection(true).build());
  session.setContentDelegate(new GeckoSession.ContentDelegate(){
   @Override public void onFullScreen(GeckoSession s,boolean fullscreen){
    if(s==current())runOnUiThread(()->setFullScreen(fullscreen));
   }
  });
  session.setNavigationDelegate(new GeckoSession.NavigationDelegate(){
   @Override public void onLocationChange(GeckoSession s,String location,
     List<GeckoSession.PermissionDelegate.ContentPermission> permissions,Boolean gesture){
    if(location==null)return;
    urls.put(s,location);
    if(s==current()){address.setText(location);downloadButton.setText("↓");}
    if(!incognito)saveHistory(location,titles.getOrDefault(s,location));
   }
   @Override public void onCanGoBack(GeckoSession s,boolean value){
    if(s==current())canBack=value;
   }
   @Override public void onCanGoForward(GeckoSession s,boolean value){
    if(s==current())canForward=value;
   }
   @Override public GeckoResult<GeckoSession> onNewSession(GeckoSession s,String uri){
    // Ad popunders are never allowed to create surprise tabs.
    return null;
   }
  });
  session.setProgressDelegate(new GeckoSession.ProgressDelegate(){
   @Override public void onPageStop(GeckoSession s,boolean success){
    if(s==current())runOnUiThread(()->downloadButton.setText(detected.get(s).isEmpty()?"↓":"↓●"));
   }
  });
  session.open(runtime);
  tabs.add(session);urls.put(session,url);titles.put(session,"New tab");
  detected.put(session,new ArrayList<>());select(tabs.size()-1);session.loadUri(url);
 }
 private void select(int index){
  if(index<0||index>=tabs.size())return;
  if(view.getSession()!=null)view.releaseSession();
  selected=index;view.setSession(current());
  address.setText(currentUrl());tabCounter.setText("▣"+tabs.size());
  downloadButton.setText(detected.get(current()).isEmpty()?"↓":"↓●");
  canBack=false;canForward=false;
 }
 private void closeTab(int index){
  if(index<0||index>=tabs.size())return;
  GeckoSession removed=tabs.remove(index);
  if(view.getSession()==removed)view.releaseSession();
  removed.close();urls.remove(removed);titles.remove(removed);detected.remove(removed);
  if(tabs.isEmpty()){selected=-1;newTab("https://www.google.com",false);}
  else select(Math.max(0,Math.min(index,tabs.size()-1)));
 }
 private void tabPicker(){
  String[] names=new String[tabs.size()+2];
  for(int i=0;i<tabs.size();i++)names[i]=(i==selected?"● ":"○ ")+(i+1)+". "+shortUrl(urls.get(tabs.get(i)));
  names[tabs.size()]="＋ New tab";names[tabs.size()+1]="＋ Private tab";
  new AlertDialog.Builder(this).setTitle("MH Browser • Tabs")
   .setItems(names,(d,index)->{
    if(index==tabs.size())newTab("https://www.google.com",false);
    else if(index==tabs.size()+1)newTab("https://www.google.com",true);
    else select(index);
   }).setNeutralButton("Close current tab",(d,w)->closeTab(selected))
   .setNegativeButton("Cancel",null).show();
 }
 private String shortUrl(String url){
  try{String host=Uri.parse(url).getHost();return host==null?"New tab":host;}
  catch(Exception e){return "New tab";}
 }
 private void setFullScreen(boolean fullscreen){
  fullScreen=fullscreen;top.setVisibility(fullscreen?View.GONE:View.VISIBLE);
  bottom.setVisibility(fullscreen?View.GONE:View.VISIBLE);
  getWindow().getDecorView().setSystemUiVisibility(fullscreen?
   View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY:0);
 }
 private void menu(){
  String[] names={"New tab","New private tab","Bookmarks","History","Downloads","Save bookmark",
   "Share page","Settings","Refresh"};
  new AlertDialog.Builder(this).setTitle("MH Browser").setItems(names,(d,index)->{
   switch(index){
    case 0:newTab("https://www.google.com",false);break;
    case 1:newTab("https://www.google.com",true);break;
    case 2:bookmarks();break;
    case 3:history();break;
    case 4:openDownloads();break;
    case 5:saveBookmark();break;
    case 6:share();break;
    case 7:settings();break;
    case 8:if(current()!=null)current().reload();break;
   }
  }).show();
 }
 private JSONArray read(String key){
  try{return new JSONArray(prefs.getString(key,"[]"));}catch(Exception e){return new JSONArray();}
 }
 private void save(String key,JSONArray data){prefs.edit().putString(key,data.toString()).apply();}
 private void saveHistory(String url,String title){
  if(url==null||!url.startsWith("http"))return;
  JSONArray previous=read("history"),next=new JSONArray();
  JSONObject item=new JSONObject();
  try{item.put("url",url);item.put("title",title);item.put("time",System.currentTimeMillis());}
  catch(Exception ignored){}
  next.put(item);
  for(int i=0;i<previous.length()&&next.length()<500;i++){
   JSONObject old=previous.optJSONObject(i);
   if(old!=null&&!url.equals(old.optString("url")))next.put(old);
  }
  save("history",next);
 }
 private void saveBookmark(){
  String url=currentUrl();
  if(!url.startsWith("http")){toast("Open a website first");return;}
  JSONArray bookmarks=read("bookmarks");
  for(int i=0;i<bookmarks.length();i++)
   if(url.equals(bookmarks.optJSONObject(i).optString("url"))){toast("Already bookmarked");return;}
  JSONObject item=new JSONObject();
  try{item.put("url",url);item.put("title",shortUrl(url));}catch(Exception ignored){}
  bookmarks.put(item);save("bookmarks",bookmarks);toast("Bookmark saved");
 }
 private void showSaved(String key,String title){
  JSONArray entries=read(key);
  if(entries.length()==0){new AlertDialog.Builder(this).setTitle(title).setMessage("No saved items").setPositiveButton("OK",null).show();return;}
  int count=Math.min(entries.length(),120);String[] items=new String[count];
  for(int i=0;i<count;i++){
   JSONObject item=entries.optJSONObject(i);
   items[i]=item==null?"":item.optString("title",shortUrl(item.optString("url")));
  }
  new AlertDialog.Builder(this).setTitle(title).setItems(items,(d,index)->{
   JSONObject item=entries.optJSONObject(index);
   if(item!=null)navigate(item.optString("url"));
  }).setNeutralButton("Clear all",(d,w)->new AlertDialog.Builder(this)
    .setTitle("Clear "+title+"?").setPositiveButton("Clear",(d2,w2)->save(key,new JSONArray()))
    .setNegativeButton("Cancel",null).show())
   .setNegativeButton("Close",null).show();
 }
 private void bookmarks(){showSaved("bookmarks","Bookmarks");}
 private void history(){showSaved("history","Browsing history");}
 private void share(){
  if(currentUrl().isEmpty())return;
  Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");
  send.putExtra(Intent.EXTRA_TEXT,currentUrl());startActivity(Intent.createChooser(send,"Share website"));
 }
 private void openDownloads(){
  try{startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));}
  catch(Exception e){toast("Open your Downloads app");}
 }
 private void downloads(){
  if(current()==null)return;
  List<Media> media=detected.get(current());
  String url=currentUrl();
  LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
  layout.setPadding(dp(16),dp(12),dp(16),dp(12));
  TextView hint=label(media.isEmpty()?
   "Play the video first. Embedded video streams appear here when the player exposes an accessible media URL.":
   "Detected playable media sources",14,GRAY);
  layout.addView(hint);
  ScrollView scroll=new ScrollView(this);scroll.addView(layout);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("↓ Media downloads")
   .setView(scroll).setNegativeButton("Close",null).create();
  if(media!=null)for(int i=media.size()-1;i>=0;i--){
   Media item=media.get(i);
   boolean hls=item.url.toLowerCase(Locale.ROOT).contains(".m3u8");
   TextView option=label("↓  "+(hls?"HLS video":"Video / audio")+"  •  "+shortUrl(item.url),16,WHITE);
   option.setPadding(dp(12),dp(14),dp(12),dp(14));option.setBackgroundColor(PANEL);
   layout.addView(option);
   option.setOnClickListener(v->{dialog.dismiss();downloadMedia(item);});
  }
  TextView page=label("↓  Download from current webpage (supported sites)",15,BLUE);
  page.setPadding(dp(12),dp(18),dp(12),dp(18));layout.addView(page);
  page.setOnClickListener(v->{dialog.dismiss();qualityChooser(url,url);});
  TextView folder=label("View saved downloads",15,WHITE);
  folder.setPadding(dp(12),dp(14),dp(12),dp(14));layout.addView(folder);
  folder.setOnClickListener(v->{dialog.dismiss();openDownloads();});
  dialog.show();
 }
 private void downloadMedia(Media item){
  String lower=item.url.toLowerCase(Locale.ROOT);
  if(lower.contains(".m3u8")){qualityChooser(item.url,item.referer);return;}
  if(lower.matches(".*\\.(mp4|m4v|webm|mov|mp3|m4a|aac|wav|ogg|opus|flac)(\\?.*)?$")){
   new AlertDialog.Builder(this).setTitle("Download detected file")
    .setMessage(shortUrl(item.url)+"\nDownload this media directly?")
    .setPositiveButton("Download",(d,w)->{
      String name=android.webkit.URLUtil.guessFileName(item.url,null,null);
      MediaDownloadService.enqueueDirect(getApplicationContext(),item.url,name,
       lower.matches(".*\\.(mp3|m4a|aac|wav|ogg|opus|flac)(\\?.*)?$")?"audio/*":"video/*",
       "Mozilla/5.0 (Android) Gecko/140.0 Firefox/140.0",null,item.referer);
      toast("Media download queued");
    }).setNegativeButton("Cancel",null).show();
  }else qualityChooser(item.url,item.referer);
 }
 private void qualityChooser(String url,String referer){
  if(url==null||!url.startsWith("http"))return;
  String[] choices={"Best available video","1080p (if available)","720p (if available)",
   "480p (if available)","Audio only"};
  new AlertDialog.Builder(this).setTitle("Choose download quality")
   .setMessage("Only accessible, non-DRM media can be downloaded. Unsupported websites may fail.")
   .setItems(choices,(d,index)->{
    int height=index==1?1080:index==2?720:index==3?480:0;
    String audio=index==4?"mp3":null;
    MediaDownloadService.enqueue(getApplicationContext(),url,
     new MediaDownloadEngine.Choice(choices[index],height,audio),referer,
     "Mozilla/5.0 (Android) Gecko/140.0 Firefox/140.0");
    toast("Download queued. Check Downloads for progress.");
   }).setNegativeButton("Cancel",null).show();
 }
 private void settings(){
  String[] items={"Ad blocker: ON (Gecko protection + extension)",
   "Block popup windows: ON","Private browsing: available in Tabs",
   "Clear browsing history","Clear bookmarks","Open Android Downloads"};
  new AlertDialog.Builder(this).setTitle("Privacy & Security").setItems(items,(d,i)->{
   if(i==3)new AlertDialog.Builder(this).setTitle("Clear history?")
    .setPositiveButton("Clear",(x,w)->save("history",new JSONArray()))
    .setNegativeButton("Cancel",null).show();
   else if(i==4)new AlertDialog.Builder(this).setTitle("Clear bookmarks?")
    .setPositiveButton("Clear",(x,w)->save("bookmarks",new JSONArray()))
    .setNegativeButton("Cancel",null).show();
   else if(i==5)openDownloads();
   else toast("Protection is enabled");
  }).setNegativeButton("Close",null).show();
 }
 @Override public void onBackPressed(){
  if(fullScreen){setFullScreen(false);return;}
  if(current()!=null&&canBack){current().goBack();return;}
  new AlertDialog.Builder(this).setTitle("Exit MH Browser?")
   .setPositiveButton("Exit",(d,w)->finish()).setNegativeButton("Cancel",null).show();
 }
 @Override protected void onDestroy(){
  if(view!=null&&view.getSession()!=null)view.releaseSession();
  for(GeckoSession session:tabs)session.close();
  tabs.clear();super.onDestroy();
 }
}
