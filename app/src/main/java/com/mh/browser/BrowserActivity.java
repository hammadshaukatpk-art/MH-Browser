package com.mh.browser;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import java.util.*;
import java.io.ByteArrayInputStream;
import org.json.*;
import android.app.DownloadManager;
import android.os.Environment;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.content.res.Configuration;
import android.util.Rational;
import android.graphics.drawable.Drawable;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;
public class BrowserActivity extends Activity {
 final int BG=0xff071326,PANEL=0xff10223a,BLUE=0xff269cff,WHITE=Color.WHITE,GRAY=0xffa4b6ce;
 LinearLayout root,head,body,bar; EditText address; boolean showingSettings=false;int screen=0;
 View fullscreenVideo;WebChromeClient.CustomViewCallback fullscreenCallback;FrameLayout videoLayer;
 boolean inPip=false, inlineVideoFocus=false, mediaPlaying=false, privateOnly=false; int historyFilter=0,downloadFilter=0;
 ArrayList<Tab> tabs=new ArrayList<>();int index=-1; boolean privateNext=false;
 android.content.SharedPreferences prefs; String proxyUser="",proxyPassword="";
 static class Tab {WebView web; boolean incognito,desktop; Tab(WebView w,boolean p,boolean d){web=w;incognito=p;desktop=d;}}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
 GradientDrawable bg(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(24);return g;}
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("mh",0);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
 root=column();root.setBackgroundColor(BG);setContentView(root);if(Build.VERSION.SDK_INT>=35){root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets z=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(z.left,z.top,z.right,z.bottom);return insets;});}getWindow().getDecorView().setSystemUiVisibility(0);head=column();root.addView(head);body=column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));bar=row();bar.setBackgroundColor(PANEL);root.addView(bar);
 if(prefs.getBoolean("proxyActive",false)&&WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)){
  String host=prefs.getString("proxyHost",""),port=prefs.getString("proxyPort","");
  if(!host.isEmpty()&&!port.isEmpty()){try{
   ProxyConfig config=new ProxyConfig.Builder().addProxyRule(prefs.getString("proxyScheme","http")+"://"+host+":"+port).build();
   ProxyController.getInstance().setProxyOverride(config,(command)->runOnUiThread(command),()->{});
  }catch(Exception ignored){}}
 }if(privateOnly){CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();createTab(true);}else restoreTabs();showSplash();}

 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 GradientDrawable grad(int a,int b,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});g.setCornerRadius(dp(radius));return g;}
 TextView action(String glyph,int size,Runnable run){TextView t=text(glyph,size,WHITE);t.setGravity(Gravity.CENTER);t.setOnClickListener(v->run.run());return t;}
 void showSplash(){
   LinearLayout splash=column();splash.setBackgroundColor(BG);splash.setGravity(Gravity.CENTER);
   ImageView emblem=new ImageView(this);emblem.setImageResource(R.drawable.app_logo);
   splash.addView(emblem,new LinearLayout.LayoutParams(dp(120),dp(120)));
   TextView name=text("MH Browser",29,WHITE);name.setTypeface(null,Typeface.BOLD);name.setGravity(Gravity.CENTER);
   LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(-1,-2);nlp.topMargin=dp(20);splash.addView(name,nlp);
   TextView subtitle=text("Fast  •  Private  •  Secure",15,GRAY);subtitle.setGravity(Gravity.CENTER);splash.addView(subtitle);
   ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
   progress.setIndeterminate(true);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(130),dp(3));pp.topMargin=dp(48);splash.addView(progress,pp);
   TextView version=text("Version 1.0",12,GRAY);version.setGravity(Gravity.CENTER);
   LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);vp.topMargin=dp(22);splash.addView(version,vp);
   setContentView(splash);
   new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing()&&!isDestroyed()){setContentView(root);root.requestApplyInsets();}},1250);
 }
 void nav(String s,Runnable run){TextView t=action(s,25,run);bar.addView(t,new LinearLayout.LayoutParams(0,dp(58),1));}
 void controls(){bar.removeAllViews();bar.setPadding(dp(8),dp(2),dp(8),dp(2));nav("‹",()->{if(active().canGoBack())active().goBack();else navigate("about:blank");});nav("›",()->{if(active().canGoForward())active().goForward();});nav("⌂",()->navigate("about:blank"));nav("▣",this::tabActions);nav("☰",this::menu);}

 WebView active(){return tabs.get(index).web;}
 String url(String s){s=s.trim();if(s.isEmpty())return "about:blank";if(s.startsWith("https://")||s.startsWith("http://")||s.equals("about:blank"))return s;if(s.contains(".")&&!s.contains(" "))return "https://"+s;return "https://www.google.com/search?q="+Uri.encode(s);}
 void navigate(String target){String dest=url(target);if(!tabs.get(index).incognito&&!dest.equals("about:blank"))prefs.edit().putString("lastUrl",dest).apply();if(dest.equals("about:blank")){active().loadUrl(dest);display();}else{body.removeAllViews();WebView w=active();if(w.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)w.getParent()).removeView(w);body.addView(w,new LinearLayout.LayoutParams(-1,-1));if(prefs.getBoolean("dnt",false)){java.util.Map<String,String> h=new java.util.HashMap<>();h.put("DNT","1");w.loadUrl(dest,h);}else w.loadUrl(dest);display();}}

 void header(){
  head.setBackgroundColor(BG);
  LinearLayout r=row();r.setPadding(dp(14),dp(10),dp(14),dp(9));head.addView(r);
  TextView lock=text("⌕",25,0xff71d6ff);lock.setGravity(Gravity.CENTER);
  LinearLayout search=row();search.setBackground(grad(0xff192e4a,0xff14253f,17));
  r.addView(search,new LinearLayout.LayoutParams(0,dp(48),1));
  search.addView(lock,new LinearLayout.LayoutParams(dp(42),-1));
  address=new EditText(this);address.setSingleLine();address.setHint("Search or type URL");address.setTextColor(WHITE);
  address.setHintTextColor(0xffb1c1d6);address.setTextSize(14);address.setPadding(0,0,dp(8),0);
  address.setBackgroundColor(Color.TRANSPARENT);search.addView(address,new LinearLayout.LayoutParams(0,-1,1));
  address.setImeOptions(2);
  address.setOnEditorActionListener((v,a,e)->{navigate(address.getText().toString());address.clearFocus();
    ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);return true;});
  TextView media=action("↓",24,this::mediaDownloads);media.setContentDescription("Download available media");
  r.addView(media,new LinearLayout.LayoutParams(dp(38),dp(48)));
  TextView reload=action("⟳",25,()->active().reload());r.addView(reload,new LinearLayout.LayoutParams(dp(40),dp(48)));
 }

 void createTab(boolean inc){if(inc&&!privateOnly){startActivity(new Intent(this,PrivateBrowserActivity.class));return;}if(privateOnly)inc=true;WebView w=new WebView(this);w.setBackgroundColor(WHITE);WebSettings s=w.getSettings();s.setJavaScriptEnabled(prefs.getBoolean("javascript",true));s.setLoadsImagesAutomatically(prefs.getBoolean("images",true));s.setDomStorageEnabled(!inc);s.setMediaPlaybackRequiresUserGesture(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setAllowFileAccess(false);s.setSafeBrowsingEnabled(true);s.setSupportZoom(prefs.getBoolean("zoom",true));s.setBuiltInZoomControls(prefs.getBoolean("zoom",true));s.setDisplayZoomControls(false);s.setJavaScriptCanOpenWindowsAutomatically(prefs.getBoolean("popups",false));s.setSupportMultipleWindows(false);if(prefs.getBoolean("desktop",false)){s.setUserAgentString(desktopAgent());s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);}if(inc)s.setCacheMode(WebSettings.LOAD_NO_CACHE);
 CookieManager.getInstance().setAcceptThirdPartyCookies(w,!inc&&!prefs.getBoolean("blockCookies",true));
 w.setDownloadListener((link,userAgent,disposition,mime,length)->{
  String filename=URLUtil.guessFileName(link,disposition,mime);
  themed().setTitle("Download file").setMessage(filename+" • "+(length>0?(length/1024)+" KB":"Size unknown"))
   .setNegativeButton("Cancel",null).setPositiveButton("Download",(d,n)->startDownload(link,filename,mime)).show();
 });
 w.addJavascriptInterface(new Object(){
  @JavascriptInterface public void playing(){runOnUiThread(()->{if(w==active()){mediaPlaying=true;}});}
  @JavascriptInterface public void stopped(){runOnUiThread(()->{if(w==active())mediaPlaying=false;});}
 },"MHMedia");
 w.setWebChromeClient(new WebChromeClient(){
  @Override public void onShowCustomView(View view,CustomViewCallback callback){showVideo(view,callback);}
  @Override public void onHideCustomView(){hideVideo();}
  @Override public boolean onJsAlert(WebView view,String url,String message,JsResult result){
   themed().setTitle("Website message").setMessage(message).setPositiveButton("OK",(d,n)->result.confirm()).setOnCancelListener(d->result.cancel()).show();return true;
  }
  @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback cb){
   if(!prefs.getBoolean("location",false)){cb.invoke(origin,false,false);return;}
   boolean granted=checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED;
   if(!granted){cb.invoke(origin,false,false);runOnUiThread(()->dialog("Location permission","Enable precise location for MH Browser in Android App Settings."));return;}
   runOnUiThread(()->themed().setTitle("Location permission").setMessage("Allow "+origin+" to access your device location?")
    .setNegativeButton("Deny",(d,n)->cb.invoke(origin,false,false))
    .setPositiveButton("Allow",(d,n)->cb.invoke(origin,true,false)).show());
  }
 });
 w.setWebViewClient(new WebViewClient(){
  @Override public void onReceivedHttpAuthRequest(WebView view,HttpAuthHandler handler,String host,String realm){
   if(!proxyUser.isEmpty()&&!proxyPassword.isEmpty()){handler.proceed(proxyUser,proxyPassword);}
   else handler.cancel();
  }
  @Override public void onPageStarted(WebView v,String u,android.graphics.Bitmap icon){if(v==active()&&!showingSettings)display();}@Override public void onPageFinished(WebView v,String u){if(v==active()&&!showingSettings){if(address!=null)address.setText(u.equals("about:blank")?"":u);display();}
 v.evaluateJavascript("(function(){if(window.mhMediaInit)return;window.mhMediaInit=true;function watch(){document.querySelectorAll('video,audio').forEach(function(v){if(v.dataset.mhTracked)return;v.dataset.mhTracked='1';v.addEventListener('play',function(){MHMedia.playing()});v.addEventListener('pause',function(){MHMedia.stopped()})})}watch();new MutationObserver(watch).observe(document.documentElement,{childList:true,subtree:true});})()",null);for(Tab t:tabs)if(t.web==v&&!t.incognito&&u.startsWith("http")){addHistory(u,v.getTitle());break;}}@Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){if(!prefs.getBoolean("trackers",true))return null;String host=req.getUrl().getHost();if(host!=null&&(host.endsWith("doubleclick.net")||host.endsWith("google-analytics.com")||host.endsWith("facebook.net")))return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));return null;}});
 tabs.add(new Tab(w,inc,prefs.getBoolean("desktop",false)));index=tabs.size()-1;display();}
 void display(){showingSettings=false;screen=0;head.removeAllViews();tabStrip();body.removeAllViews();if(active().getUrl()==null||active().getUrl().equals("about:blank")){address=null;home();}else{header();WebView w=active();if(w.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)w.getParent()).removeView(w);body.addView(w,new LinearLayout.LayoutParams(-1,-1));address.setText(w.getUrl());}controls();}
 void tabStrip(){
  HorizontalScrollView sc=new HorizontalScrollView(this);sc.setHorizontalScrollBarEnabled(false);
  sc.setFillViewport(false);sc.setBackgroundColor(0xff0a1b31);head.addView(sc,new LinearLayout.LayoutParams(-1,dp(51)));
  LinearLayout row=row();row.setPadding(dp(10),dp(5),dp(10),dp(5));sc.addView(row);
  for(int i=0;i<tabs.size();i++){
   final int n=i;Tab t=tabs.get(i);LinearLayout pill=row();
   pill.setPadding(dp(9),0,dp(3),0);pill.setBackground(grad(i==index?0xff245e88:0xff182b44,i==index?0xff183e65:0xff12243a,13));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(138),dp(39));lp.rightMargin=dp(7);row.addView(pill,lp);
   String name=t.web.getTitle();if(name==null||name.isEmpty()||"about:blank".equals(t.web.getUrl()))name="New tab";
   TextView title=text((t.incognito?"◉ ":"")+name,12,WHITE);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
   pill.addView(title,new LinearLayout.LayoutParams(0,-1,1));title.setOnClickListener(v->{index=n;display();});
   TextView x=action("×",20,()->closeTab(n));pill.addView(x,new LinearLayout.LayoutParams(dp(27),-1));
  }
  TextView add=action("+",27,()->createTab(false));add.setBackground(bg(PANEL));row.addView(add,new LinearLayout.LayoutParams(dp(39),dp(39)));
 }
 void closeTab(int n){if(n<0||n>=tabs.size())return;Tab old=tabs.remove(n);old.web.stopLoading();old.web.destroy();
  if(tabs.isEmpty()){index=-1;createTab(false);}else{if(n<index)index--;else if(n==index)index=Math.min(n,tabs.size()-1);display();}}
 void tabActions(){String[] options={"New tab","New private tab","Close current tab","Switch tab"};themed().setTitle("Tabs").setItems(options,(d,n)->{if(n==0)createTab(false);else if(n==1)createTab(true);else if(n==2)closeTab(index);else tabPicker();}).show();}
 void tabPicker(){String[] names=new String[tabs.size()];for(int i=0;i<tabs.size();i++){String t=tabs.get(i).web.getTitle();names[i]=(tabs.get(i).incognito?"◉ ":"")+(t==null?"New tab":t);}themed().setTitle("Switch tab").setSingleChoiceItems(names,index,(d,n)->{index=n;d.dismiss();display();}).setNegativeButton("Close",null).show();}


 void home(){
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(false);body.addView(sc);
  LinearLayout c=column();c.setPadding(dp(19),dp(20),dp(19),dp(20));sc.addView(c);
  LinearLayout brand=row();brand.setGravity(Gravity.CENTER_VERTICAL);c.addView(brand);
  ImageView logo=new ImageView(this);logo.setImageResource(com.mh.browser.R.drawable.app_logo);
  brand.addView(logo,new LinearLayout.LayoutParams(dp(70),dp(70)));
  LinearLayout brandWords=column();brandWords.setPadding(dp(10),0,0,0);brand.addView(brandWords,new LinearLayout.LayoutParams(0,-2,1));
  TextView title=text(tabs.get(index).incognito?"Private Browsing":"MH Browser",23,WHITE);title.setTypeface(null,Typeface.BOLD);brandWords.addView(title);
  brandWords.addView(text("Fast  •  Private  •  Secure",12,0xffa8bcd5));
  LinearLayout profile=column();profile.setGravity(Gravity.CENTER);brand.addView(profile,new LinearLayout.LayoutParams(dp(70),dp(76)));
  ImageView google=new ImageView(this);google.setImageResource(R.drawable.google_g);
  google.setBackground(grad(WHITE,0xfff1f6ff,30));google.setPadding(dp(6),dp(6),dp(6),dp(6));
  profile.addView(google,new LinearLayout.LayoutParams(dp(42),dp(42)));
  TextView account=text("Google",10,WHITE);account.setGravity(Gravity.CENTER);profile.addView(account);
  TextView login=text("Login Gmail",9,GRAY);login.setGravity(Gravity.CENTER);profile.addView(login);
  profile.setOnClickListener(v->googleAccount());

  if(tabs.get(index).incognito){
    TextView warning=text("Private session • separate browser storage. Close all private tabs to end this session.",13,0xff9bdbff);
    warning.setPadding(dp(12),dp(12),dp(12),dp(12));warning.setBackground(bg(PANEL));c.addView(warning);
  }
  TextView caption=text("Discover the web",19,WHITE);caption.setTypeface(null,Typeface.BOLD);
  LinearLayout.LayoutParams cap=new LinearLayout.LayoutParams(-1,-2);cap.topMargin=dp(30);c.addView(caption,cap);
  EditText query=new EditText(this);query.setSingleLine();query.setTextColor(WHITE);query.setTextSize(16);
  query.setHintTextColor(0xffb7c8de);query.setHint("⌕   Search the web or enter a URL");
  query.setPadding(dp(17),0,dp(14),0);query.setBackground(grad(0xff1a3355,0xff122b47,16));
  LinearLayout.LayoutParams qlp=new LinearLayout.LayoutParams(-1,dp(56));qlp.topMargin=dp(12);c.addView(query,qlp);
  query.setImeOptions(2);query.setOnEditorActionListener((v,a,e)->{navigate(query.getText().toString());return true;});
  LinearLayout section=row();LinearLayout.LayoutParams sec=new LinearLayout.LayoutParams(-1,-2);sec.topMargin=dp(26);c.addView(section,sec);
  TextView shortcuts=text("Quick access",18,WHITE);shortcuts.setTypeface(null,Typeface.BOLD);section.addView(shortcuts,new LinearLayout.LayoutParams(0,-2,1));
  TextView edit=text("EDIT",12,0xff62caff);section.addView(edit);edit.setOnClickListener(v->editShortcuts());
  JSONArray links=shortcuts();int count=links.length()+1;
  for(int start=0;start<count;start+=4){
    LinearLayout line=row();line.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams llp=new LinearLayout.LayoutParams(-1,-2);llp.topMargin=dp(14);c.addView(line,llp);
    for(int j=0;j<4;j++){
      int n=start+j;if(n>=count){TextView empty=text("",11,WHITE);line.addView(empty,new LinearLayout.LayoutParams(0,dp(84),1));continue;}
      JSONObject obj=n<links.length()?links.optJSONObject(n):null;
      String label=obj==null?"Add":obj.optString("name","Site");
      String target=obj==null?"":obj.optString("url","");
      LinearLayout item=column();item.setGravity(Gravity.CENTER);line.addView(item,new LinearLayout.LayoutParams(0,dp(84),1));
      TextView circle=text(obj==null?"+":label.substring(0,Math.min(1,label.length())).toUpperCase(),22,WHITE);
      circle.setTypeface(null,Typeface.BOLD);circle.setGravity(Gravity.CENTER);
      int color=obj==null?0xff21547d:new int[]{0xff4285f4,0xffd73a4b,0xffe9962e,0xff2777ba,0xffe66d43,0xff386dc8,0xff2c4569}[n%7];
      circle.setBackground(grad(color,color,20));item.addView(circle,new LinearLayout.LayoutParams(dp(47),dp(47)));
      TextView lab=text(label,11,0xffd7e7fb);lab.setGravity(Gravity.CENTER);lab.setSingleLine(true);
      lab.setEllipsize(android.text.TextUtils.TruncateAt.END);item.addView(lab,new LinearLayout.LayoutParams(-1,dp(29)));
      item.setOnClickListener(v->{if(obj==null)shortcutForm(-1);else navigate(target);});
      item.setOnLongClickListener(v->{if(obj!=null)shortcutOptions(n);return true;});
    }
  }
  FrameLayout ad=new FrameLayout(this);ad.setBackground(grad(0xff153f68,0xff0b2039,19));
  LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(177));hp.topMargin=dp(24);c.addView(ad,hp);
  View art=new View(this){
   android.graphics.Paint p=new android.graphics.Paint(3);
   @Override protected void onDraw(android.graphics.Canvas canvas){
    super.onDraw(canvas);float w=getWidth(),h=getHeight();
    p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(dp(2));
    for(int i=0;i<8;i++){p.setColor(i%2==0?0x5570d5ff:0x4478a8f8);
     canvas.drawLine(w*.58f+dp(i*14),-dp(20),w*.12f+dp(i*21),h+dp(20),p);}
    p.setStrokeWidth(dp(3));p.setColor(0x5593d9ff);
    canvas.drawCircle(w*.89f,h*.24f,dp(43),p);canvas.drawCircle(w*.89f,h*.24f,dp(28),p);
    p.setStyle(android.graphics.Paint.Style.FILL);p.setColor(0x2267d9ff);
    canvas.drawCircle(w*.9f,h*.83f,dp(58),p);
   }
  };ad.addView(art,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout hero=column();hero.setPadding(dp(19),dp(15),dp(15),dp(12));ad.addView(hero,new FrameLayout.LayoutParams(-1,-1));
  TextView tag=text("ADVERTISING SPACE",11,0xff8feaff);tag.setTypeface(null,Typeface.BOLD);hero.addView(tag);
  TextView htitle=text("Advertise with MH",23,WHITE);htitle.setTypeface(null,Typeface.BOLD);
  LinearLayout.LayoutParams ht=new LinearLayout.LayoutParams(-1,-2);ht.topMargin=dp(11);hero.addView(htitle,ht);
  hero.addView(text("For Advertising, Contact with Us",13,0xffd5e8fa));
  TextView go=text("Contact with Us  →",14,0xff9beaff);go.setTypeface(null,Typeface.BOLD);
  LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,-2);gp.topMargin=dp(17);hero.addView(go,gp);
  ad.setOnClickListener(v->dialog("Advertise with MH","Advertising contact details will be available soon."));
  LinearLayout footer=row();footer.setGravity(Gravity.CENTER);
  LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.topMargin=dp(28);c.addView(footer,fp);
  TextView priv=text("◉  Privacy controls",13,0xffa5c8f2);footer.addView(priv,new LinearLayout.LayoutParams(0,-2,1));
  priv.setOnClickListener(v->settings());
  TextView newTab=text("+  New tab",13,0xff8bdfff);footer.addView(newTab);newTab.setOnClickListener(v->createTab(false));
 }


 void tabList(){
  head.removeAllViews();body.removeAllViews();
  LinearLayout h=row();h.setPadding(dp(15),dp(10),dp(15),dp(10));head.addView(h);
  TextView back=action("‹",28,this::display);h.addView(back,new LinearLayout.LayoutParams(dp(40),dp(44)));
  TextView title=text("Tabs  ("+tabs.size()+")",22,WHITE);title.setTypeface(null,Typeface.BOLD);h.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  ScrollView sc=new ScrollView(this);body.addView(sc);LinearLayout items=column();items.setPadding(dp(15),dp(10),dp(15),dp(16));sc.addView(items);
  for(int i=0;i<tabs.size();i++){
    final int n=i;Tab t=tabs.get(i);LinearLayout r=row();r.setPadding(dp(15),dp(12),dp(15),dp(12));
    r.setBackground(grad(0xff183654,0xff0f243e,15));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(88));lp.bottomMargin=dp(10);items.addView(r,lp);
    LinearLayout info=column();r.addView(info,new LinearLayout.LayoutParams(0,-2,1));
    info.addView(text(t.incognito?"◉  PRIVATE TAB":"🌐  BROWSER TAB",11,0xff7dd8ff));
    TextView name=text(t.web.getTitle()==null?"New Tab":t.web.getTitle(),16,WHITE);name.setSingleLine(true);info.addView(name);
    info.setOnClickListener(v->{index=n;display();});
    TextView close=action("×",26,()->{Tab old=tabs.remove(n);old.web.destroy();if(tabs.isEmpty()){index=-1;createTab(false);}else{index=Math.min(n,tabs.size()-1);tabList();}});
    r.addView(close,new LinearLayout.LayoutParams(dp(45),dp(50)));
  }
  Button plus=new Button(this);plus.setText("+ New tab");items.addView(plus);plus.setOnClickListener(v->createTab(false));
  Button priv=new Button(this);priv.setText("+ Private tab");items.addView(priv);priv.setOnClickListener(v->createTab(true));
 }


 void menu(){
  ScrollView sc=new ScrollView(this);sc.setFillViewport(false);
  LinearLayout list=column();list.setPadding(dp(16),dp(16),dp(16),dp(14));sc.addView(list);
  LinearLayout brand=column();brand.setGravity(Gravity.CENTER);list.addView(brand);
  ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.app_logo);
  brand.addView(logo,new LinearLayout.LayoutParams(dp(58),dp(58)));
  TextView heading=text("MH Browser",21,WHITE);heading.setTypeface(null,Typeface.BOLD);heading.setGravity(Gravity.CENTER);
  brand.addView(heading);
  TextView sub=text("Fast  •  Private  •  Secure",11,GRAY);sub.setGravity(Gravity.CENTER);brand.addView(sub);
  menuLabel(list,"TABS & NAVIGATION");
  AlertDialog d=themed().setView(sc).create();
  menuItem(list,"＋","New tab","Open another browser tab",()->createTab(false),d);
  menuItem(list,"◉","New private tab","Private browsing preview",()->createTab(true),d);
  menuItem(list,"▣","Switch tabs","Choose an open tab",this::tabPicker,d);
  menuLabel(list,"YOUR BROWSING");
  menuItem(list,"☆","Bookmarks","Save and manage websites",this::bookmarks,d);
  menuItem(list,"◷","History","Today, previous days, weeks and more",this::history,d);
  menuItem(list,"↓","Downloads","Files saved by Android",this::downloads,d);
  menuLabel(list,"TOOLS & SETTINGS");
  menuItem(list,"⚙","Settings","Customize MH Browser",this::settings,d);
  menuItem(list,"▤","Desktop site",tabs.get(index).desktop?"Enabled for this tab":"Request a desktop layout",this::toggleDesktop,d);
  menuItem(list,"◫","Picture in picture","Fullscreen video only",this::pictureInPicture,d);
  menuItem(list,"↗","Share page","Send this website to another app",()->{
   Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,active().getUrl());
   startActivity(Intent.createChooser(share,"Share page"));
  },d);
  d.show();styleDialog(d);
  if(d.getWindow()!=null){android.util.DisplayMetrics m=getResources().getDisplayMetrics();d.getWindow().setLayout((int)(m.widthPixels*0.91f),Math.min(dp(690),(int)(m.heightPixels*0.83f)));}
 }
 void menuLabel(LinearLayout list,String label){
  TextView v=text(label,11,0xff68d5ff);v.setTypeface(null,Typeface.BOLD);
  v.setPadding(dp(8),dp(18),0,dp(8));list.addView(v);
 }
 void menuItem(LinearLayout list,String symbol,String label,String desc,Runnable action,AlertDialog d){
  LinearLayout r=row();r.setPadding(dp(11),dp(9),dp(9),dp(9));r.setBackground(grad(0xff193653,0xff10263e,13));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(6);list.addView(r,lp);
  TextView icon=text(symbol,23,0xff82dfff);icon.setGravity(Gravity.CENTER);r.addView(icon,new LinearLayout.LayoutParams(dp(41),dp(47)));
  LinearLayout words=column();r.addView(words,new LinearLayout.LayoutParams(0,-2,1));
  TextView name=text(label,15,WHITE);name.setTypeface(null,Typeface.BOLD);words.addView(name);
  words.addView(text(desc,11,GRAY));r.addView(text("›",23,GRAY));
  r.setOnClickListener(v->{d.dismiss();action.run();});
 }
 String desktopAgent(){
  String ua=WebSettings.getDefaultUserAgent(this);
  return ua.replaceAll("\\(Linux; Android[^)]*\\)","(X11; Linux x86_64)").replace(" Mobile "," ").replace("; wv","");
 }
 void toggleDesktop(){
  Tab t=tabs.get(index);t.desktop=!t.desktop;
  WebSettings w=t.web.getSettings();
  w.setUserAgentString(t.desktop?desktopAgent():WebSettings.getDefaultUserAgent(this));
  w.setUseWideViewPort(t.desktop);w.setLoadWithOverviewMode(t.desktop);
  if(t.web.getUrl()!=null&&t.web.getUrl().startsWith("http"))t.web.reload();
  Toast.makeText(this,t.desktop?"Desktop site requested":"Mobile site requested",Toast.LENGTH_SHORT).show();
 }
 void pictureInPicture(){
  if(Build.VERSION.SDK_INT<26){dialog("Picture in picture","Android 8 or newer is required.");return;}
  if(fullscreenVideo==null&&!inlineVideoFocus){
   active().evaluateJavascript("(function(){var v=document.querySelector('video');return !!(v&&!v.paused&&!v.ended);})()",result->{
    if(!"true".equals(result)){dialog("Picture in Picture","Play a video first. The website must allow its video to continue in Picture in Picture.");return;}
    focusInlineVideo();enterVideoPip();
   });return;
  }
  enterVideoPip();
 }
 void focusInlineVideo(){
  if(inlineVideoFocus||fullscreenVideo!=null)return;
  inlineVideoFocus=true;head.setVisibility(View.GONE);bar.setVisibility(View.GONE);
  active().evaluateJavascript("(function(){var v=document.querySelector('video');if(!v)return;var st=document.createElement('style');st.id='mh-video-pip-style';st.textContent='html,body{background:#000!important;overflow:hidden!important}video{position:fixed!important;left:0!important;top:0!important;width:100vw!important;height:100vh!important;object-fit:contain!important;z-index:2147483647!important;background:#000!important}';document.documentElement.appendChild(st);for(var p=v.parentElement;p&&p!==document.body;p=p.parentElement){p.setAttribute('data-mh-pip','1');p.style.setProperty('transform','none','important');p.style.setProperty('overflow','visible','important');}})()",null);
 }
 void restoreInlineVideo(){
  if(!inlineVideoFocus)return;inlineVideoFocus=false;
  if(index>=0){active().evaluateJavascript("(function(){var st=document.getElementById('mh-video-pip-style');if(st)st.remove();document.querySelectorAll('[data-mh-pip]').forEach(function(p){p.style.removeProperty('transform');p.style.removeProperty('overflow');p.removeAttribute('data-mh-pip')});})()",null);}
  head.setVisibility(View.VISIBLE);bar.setVisibility(View.VISIBLE);
 }
 void enterVideoPip(){
  try{
   android.app.PictureInPictureParams params=new android.app.PictureInPictureParams.Builder()
    .setAspectRatio(new Rational(16,9)).build();
   enterPictureInPictureMode(params);
  }catch(Exception e){dialog("Picture in picture","Picture in picture is not available for this video or device.");}
 }
 void showVideo(View v,WebChromeClient.CustomViewCallback cb){
  if(fullscreenVideo!=null){cb.onCustomViewHidden();return;}
  fullscreenVideo=v;fullscreenCallback=cb;
  videoLayer=new FrameLayout(this);videoLayer.setBackgroundColor(Color.BLACK);
  if(v.getParent() instanceof ViewGroup)((ViewGroup)v.getParent()).removeView(v);
  videoLayer.addView(v,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
  ((ViewGroup)getWindow().getDecorView()).addView(videoLayer,new ViewGroup.LayoutParams(-1,-1));
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
 }
 void hideVideo(){
  if(fullscreenVideo==null)return;
  if(videoLayer!=null){videoLayer.removeAllViews();((ViewGroup)getWindow().getDecorView()).removeView(videoLayer);}
  fullscreenVideo=null;videoLayer=null;
  WebChromeClient.CustomViewCallback cb=fullscreenCallback;fullscreenCallback=null;
  if(cb!=null)cb.onCustomViewHidden();
  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  getWindow().getDecorView().setSystemUiVisibility(0);
 }
 @Override public void onPictureInPictureModeChanged(boolean pip,Configuration config){
  super.onPictureInPictureModeChanged(pip,config);inPip=pip;
  if(videoLayer!=null){videoLayer.setVisibility(View.VISIBLE);}
  if(!pip){restoreInlineVideo();if(fullscreenVideo==null)display();}
 }
 @Override public void onUserLeaveHint(){
  super.onUserLeaveHint();
  if(Build.VERSION.SDK_INT>=26&&prefs.getBoolean("audio",true)&&mediaPlaying)pictureInPicture();
 }

 class MhDialogBuilder extends AlertDialog.Builder {
 MhDialogBuilder(){super(BrowserActivity.this,android.R.style.Theme_Material_Dialog_Alert);}
 @Override public AlertDialog show(){AlertDialog d=super.show();styleDialog(d);return d;}
}
void styleDialog(AlertDialog d){
 if(d.getWindow()!=null)d.getWindow().setBackgroundDrawable(grad(0xff17324e,0xff10243c,18));
 if(d.getButton(-1)!=null)d.getButton(-1).setTextColor(0xff6ad9ff);
 if(d.getButton(-2)!=null)d.getButton(-2).setTextColor(0xffb6c9de);
 if(d.getButton(-3)!=null)d.getButton(-3).setTextColor(0xff6ad9ff);
}
AlertDialog.Builder themed(){return new MhDialogBuilder();}
 void dialog(String title,String message){themed().setTitle(title).setMessage(message).setPositiveButton("OK",null).show();}
 void settingRow(LinearLayout list,String label,String desc,Runnable act){
 LinearLayout card=row();card.setPadding(dp(14),dp(13),dp(13),dp(13));card.setBackground(grad(0xff17334f,0xff10253e,14));
 LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(9);list.addView(card,lp);
 LinearLayout content=column();card.addView(content,new LinearLayout.LayoutParams(0,-2,1));
 TextView name=text(label,16,WHITE);name.setTypeface(null,Typeface.BOLD);content.addView(name);
 TextView subtitle=text(desc,12,GRAY);subtitle.setPadding(0,dp(3),0,0);content.addView(subtitle);
 TextView arrow=text("›",24,0xff6fcfff);arrow.setGravity(Gravity.CENTER);card.addView(arrow,new LinearLayout.LayoutParams(dp(28),dp(40)));
 card.setOnClickListener(v->act.run());
}
 void switchRow(LinearLayout list,String label,String desc,String pref,boolean def){LinearLayout r=row();r.setPadding(20,12,12,12);r.setBackground(bg(PANEL));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=9;list.addView(r,lp);LinearLayout c=column();r.addView(c,new LinearLayout.LayoutParams(0,-2,1));c.addView(text(label,16,WHITE));c.addView(text(desc,12,GRAY));Switch s=new Switch(this);s.setChecked(prefs.getBoolean(pref,def));r.addView(s);s.setOnCheckedChangeListener((b,v)->{
  prefs.edit().putBoolean(pref,v).apply();
  for(Tab t:tabs){WebSettings ws=t.web.getSettings();
   if("javascript".equals(pref))ws.setJavaScriptEnabled(v);
   if("images".equals(pref))ws.setLoadsImagesAutomatically(v);
   if("zoom".equals(pref)){ws.setSupportZoom(v);ws.setBuiltInZoomControls(v);}
   if("popups".equals(pref))ws.setJavaScriptCanOpenWindowsAutomatically(v);
   if("blockCookies".equals(pref))CookieManager.getInstance().setAcceptThirdPartyCookies(t.web,!v&&!t.incognito);
  }
  if("location".equals(pref)&&v&&checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},201);
  }
 });}


 JSONArray readArray(String key){try{return new JSONArray(prefs.getString(key,"[]"));}catch(Exception e){return new JSONArray();}}
 void storeArray(String key,JSONArray a){prefs.edit().putString(key,a.toString()).apply();}
 JSONArray shortcuts(){
  String saved=prefs.getString("shortcuts",null);if(saved!=null){try{return new JSONArray(saved);}catch(Exception ignored){}}
  JSONArray a=new JSONArray();String[][] d={{"Google","https://www.google.com"},{"YouTube","https://www.youtube.com"},{"Amazon","https://www.amazon.com"},{"eBay","https://www.ebay.com"},{"AliExpress","https://www.aliexpress.com"},{"Facebook","https://www.facebook.com"},{"X","https://x.com"}};
  for(String[] item:d){JSONObject o=new JSONObject();try{o.put("name",item[0]);o.put("url",item[1]);a.put(o);}catch(Exception ignored){}}return a;
 }
 void editShortcuts(){
  JSONArray a=shortcuts();String[] names=new String[a.length()+1];
  for(int i=0;i<a.length();i++)names[i]=a.optJSONObject(i).optString("name","Shortcut");
  names[a.length()]="+ Add shortcut";
  themed().setTitle("Edit quick access").setItems(names,(d,n)->{
   if(n==a.length())shortcutForm(-1);else shortcutOptions(n);
  }).setNegativeButton("Done",null).show();
 }
 void shortcutOptions(int n){themed().setTitle(shortcuts().optJSONObject(n).optString("name"))
  .setItems(new String[]{"Edit shortcut","Remove shortcut"},(d,i)->{
   if(i==0)shortcutForm(n);else{JSONArray a=shortcuts(),b=new JSONArray();for(int k=0;k<a.length();k++)if(k!=n)b.put(a.opt(k));storeArray("shortcuts",b);display();}
  }).setNegativeButton("Cancel",null).show();}
 void shortcutForm(int position){
  JSONObject old=position<0?null:shortcuts().optJSONObject(position);
  LinearLayout c=column();c.setPadding(dp(22),dp(10),dp(22),dp(6));
  EditText name=new EditText(this),link=new EditText(this);
  name.setSingleLine(true);name.setHint("Shortcut name");name.setTextColor(WHITE);
  link.setSingleLine(true);link.setHint("https://example.com");link.setTextColor(WHITE);link.setInputType(17);
  if(old!=null){name.setText(old.optString("name"));link.setText(old.optString("url"));}
  c.addView(name);c.addView(link);
  AlertDialog d=themed().setTitle(position<0?"Add shortcut":"Edit shortcut").setView(c).setNegativeButton("Cancel",null)
   .setPositiveButton("Save",null).create();
  d.setOnShowListener(v->{styleDialog(d);d.getButton(-1).setOnClickListener(w->{
   String nm=name.getText().toString().trim(),u=link.getText().toString().trim();
   if(nm.isEmpty()){name.setError("Enter a name");return;}
   if(!u.startsWith("https://")&&!u.startsWith("http://")){link.setError("Enter an HTTP or HTTPS URL");return;}
   Uri parsed=Uri.parse(u);if(parsed.getHost()==null){link.setError("Invalid URL");return;}
   try{JSONArray a=shortcuts();JSONObject o=new JSONObject();o.put("name",nm);o.put("url",u);
    if(position<0)a.put(o);else a.put(position,o);storeArray("shortcuts",a);d.dismiss();display();
   }catch(Exception e){link.setError("Could not save shortcut");}
  });});d.show();
 }
 void saveTabs(){
  if(privateOnly)return;
  JSONArray a=new JSONArray();int selectedNormal=0;
  for(int i=0;i<tabs.size();i++){
   Tab t=tabs.get(i);if(t.incognito)continue;
   if(i==index)selectedNormal=a.length();
   String u=t.web.getUrl();a.put(u==null?"about:blank":u);
  }
  prefs.edit().putString("tabs",a.toString()).putInt("selectedTab",selectedNormal).apply();
 }
 void restoreTabs(){
  JSONArray a=prefs.getBoolean("restore",true)?readArray("tabs"):new JSONArray();
  if(a.length()==0){createTab(false);return;}
  for(int i=0;i<a.length()&&i<12;i++){createTab(false);String u=a.optString(i,"about:blank");if(u.startsWith("http"))navigate(u);}
  index=Math.max(0,Math.min(tabs.size()-1,prefs.getInt("selectedTab",0)));display();
 }

 void addHistory(String url,String title){
  if(url==null||!url.startsWith("http"))return;
  JSONArray old=readArray("history"),out=new JSONArray();
  JSONObject first=old.optJSONObject(0);
  if(first!=null&&url.equals(first.optString("url"))){
   try{first.put("title",title==null||title.trim().isEmpty()?url:title);first.put("time",System.currentTimeMillis());}catch(Exception ignored){}
   out.put(first);for(int i=1;i<old.length()&&out.length()<500;i++)out.put(old.opt(i));
  }else{
   JSONObject item=new JSONObject();try{item.put("url",url);item.put("title",title==null||title.isEmpty()?url:title);item.put("time",System.currentTimeMillis());}catch(Exception ignored){}
   out.put(item);for(int i=0;i<old.length()&&out.length()<500;i++)out.put(old.opt(i));
  }
  storeArray("history",out);
 }
 String itemUrl(JSONArray a,int i){
  JSONObject o=a.optJSONObject(i);return o==null?a.optString(i,""):o.optString("url","");
 }
 String itemTitle(JSONArray a,int i){
  JSONObject o=a.optJSONObject(i);return o==null?a.optString(i,""):o.optString("title",o.optString("url",""));
 }
 String historyGroup(long t){
  if(t<=0)return "Earlier (legacy entries)";
  Calendar now=Calendar.getInstance(),start=Calendar.getInstance();start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);
  long today=start.getTimeInMillis();
  if(t>=today)return "Today";
  if(t>=today-86400000L)return "Yesterday";
  if(t>=today-7*86400000L)return "Previous 7 days";
  start.set(Calendar.DAY_OF_MONTH,1);if(t>=start.getTimeInMillis())return "This month";
  start.set(Calendar.DAY_OF_YEAR,1);if(t>=start.getTimeInMillis())return "This year";
  return "Older";
 }
 String friendlyUrl(String u){
  try{Uri parsed=Uri.parse(u);String host=parsed.getHost();return (host==null?u:host.replaceFirst("^www\\.",""));}
  catch(Exception e){return u;}
 }
 void clearNavigationStacks(){for(Tab tab:tabs)tab.web.clearHistory();}
 void history(){
  showingSettings=true;screen=6;head.removeAllViews();body.removeAllViews();sectionHeader("Browsing history",this::display);
  JSONArray entries=readArray("history");
  ScrollView sc=new ScrollView(this);body.addView(sc);
  LinearLayout list=column();list.setPadding(dp(15),dp(10),dp(15),dp(25));sc.addView(list);

  String[] periods={"Today","Previous Day","Week","Monthly","Yearly","All"};
  HorizontalScrollView filterScroll=new HorizontalScrollView(this);filterScroll.setHorizontalScrollBarEnabled(false);list.addView(filterScroll);
  LinearLayout filters=row();filters.setPadding(0,dp(3),0,dp(10));filterScroll.addView(filters);
  for(int i=0;i<periods.length;i++){final int n=i;
   TextView chip=action(periods[i],12,()->{historyFilter=n;history();});
   chip.setPadding(dp(13),0,dp(13),0);
   chip.setBackground(grad(historyFilter==i?0xff2477ac:0xff172e48,historyFilter==i?0xff1b567e:0xff10253e,15));
   LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-2,dp(39));clp.rightMargin=dp(7);filters.addView(chip,clp);
  }
  if(entries.length()==0){TextView empty=text("No browsing history yet",17,GRAY);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(12),dp(55),dp(12),dp(40));list.addView(empty);return;}
  TextView sub=text("Select individual pages or whole periods to remove them. Private tabs are not recorded.",12,GRAY);
  sub.setPadding(dp(5),dp(6),dp(5),dp(16));list.addView(sub);
  ArrayList<Integer> selected=new ArrayList<>();HashMap<Integer,CheckBox> checks=new HashMap<>();
  LinkedHashMap<String,ArrayList<Integer>> groups=new LinkedHashMap<>();
  for(int i=0;i<entries.length();i++){
   JSONObject obj=entries.optJSONObject(i);long t=obj==null?0:obj.optLong("time",0);

   Calendar cal=Calendar.getInstance(),start=Calendar.getInstance();start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);
   long today=start.getTimeInMillis();boolean include=true;
   if(historyFilter==0)include=t>=today;
   else if(historyFilter==1)include=t>=today-86400000L&&t<today;
   else if(historyFilter==2)include=t>=today-7*86400000L;
   else if(historyFilter==3){start.set(Calendar.DAY_OF_MONTH,1);include=t>=start.getTimeInMillis();}
   else if(historyFilter==4){start.set(Calendar.DAY_OF_YEAR,1);include=t>=start.getTimeInMillis();}
   if(!include)continue;
   String g=historyGroup(t);if(!groups.containsKey(g))groups.put(g,new ArrayList<>());groups.get(g).add(i);
  }
  if(groups.isEmpty()){TextView empty=text("No visits for this period",16,GRAY);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(45),0,dp(35));list.addView(empty);}
  for(Map.Entry<String,ArrayList<Integer>> group:groups.entrySet()){
   LinearLayout heading=row();heading.setPadding(dp(5),dp(14),dp(5),dp(10));list.addView(heading);
   TextView label=text(group.getKey(),17,0xff77d5ff);label.setTypeface(null,Typeface.BOLD);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));
   TextView select=text("SELECT PERIOD",11,0xffa1e9ff);heading.addView(select);
   select.setOnClickListener(v->{
    boolean all=true;for(int n:group.getValue())if(!selected.contains(n))all=false;
    for(int n:group.getValue()){if(all)selected.remove(Integer.valueOf(n));else if(!selected.contains(n))selected.add(n);}
    for(int n:group.getValue())if(checks.containsKey(n))checks.get(n).setChecked(!all);
   });
   for(int n:group.getValue()){
    JSONObject obj=entries.optJSONObject(n);String url=itemUrl(entries,n),name=itemTitle(entries,n);
    LinearLayout card=row();card.setPadding(dp(12),dp(11),dp(12),dp(11));card.setBackground(grad(0xff152e4a,0xff0d2239,14));
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(7);list.addView(card,cp);
    CheckBox check=new CheckBox(this);checks.put(n,check);check.setButtonTintList(android.content.res.ColorStateList.valueOf(0xff67d6ff));card.addView(check);
    check.setOnCheckedChangeListener((v,on)->{if(on){if(!selected.contains(n))selected.add(n);}else selected.remove(Integer.valueOf(n));});
    LinearLayout info=column();card.addView(info,new LinearLayout.LayoutParams(0,-2,1));
    TextView title=text(name,14,WHITE);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(title);
    TextView detail=text(friendlyUrl(url),12,GRAY);detail.setSingleLine(true);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(detail);
    if(obj!=null&&obj.optLong("time",0)>0){
     TextView time=text(new java.text.SimpleDateFormat("MMM d, h:mm a",Locale.getDefault()).format(new Date(obj.optLong("time"))),10,0xff81a7ca);info.addView(time);
    }
    info.setOnClickListener(v->{display();navigate(url);});
   }
  }
  LinearLayout actions=row();actions.setPadding(0,dp(18),0,0);list.addView(actions);
  TextView remove=action("Delete selected",15,()->{
   if(selected.isEmpty()){Toast.makeText(this,"Select history entries first",Toast.LENGTH_SHORT).show();return;}
   themed().setTitle("Delete selected history?").setMessage(selected.size()+" visit(s) will be removed.")
    .setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,n)->{
     JSONArray kept=new JSONArray();for(int k=0;k<entries.length();k++)if(!selected.contains(k))kept.put(entries.opt(k));
     storeArray("history",kept);clearNavigationStacks();history();
    }).show();
  });remove.setBackground(grad(0xff1d547a,0xff163a59,12));actions.addView(remove,new LinearLayout.LayoutParams(0,dp(48),1));
  TextView all=action("Clear all",15,()->themed().setTitle("Clear all browsing history?")
   .setMessage("This also clears Back and Forward history in every open tab.")
   .setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,n)->{
    storeArray("history",new JSONArray());clearNavigationStacks();history();
   }).show());
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(48),1);ap.leftMargin=dp(10);all.setBackground(grad(0xff5a2b43,0xff472136,12));actions.addView(all,ap);
 }
 void googleAccount(){
  themed().setTitle("Google account").setMessage("Sign in securely using your system browser. Google does not permit many account sign-in flows inside embedded Android WebViews. MH Browser account sync is not yet available.")
   .setNegativeButton("Cancel",null)
   .setNeutralButton("Google homepage",(d,n)->navigate("https://www.google.com"))
   .setPositiveButton("Secure sign in",(d,n)->{
    Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse("https://accounts.google.com/"));
    try{startActivity(intent);}catch(Exception e){dialog("Google account","No compatible browser was found.");}
   }).show();
 }
 void bookmarks(){
  showingSettings=true;screen=7;head.removeAllViews();body.removeAllViews();sectionHeader("Bookmarks",this::display);
  ScrollView sc=new ScrollView(this);body.addView(sc);
  LinearLayout list=column();list.setPadding(dp(16),dp(12),dp(16),dp(20));sc.addView(list);
  TextView add=action("☆  Save current page",16,()->{
   String u=active().getUrl();if(u==null||!u.startsWith("http")){dialog("Bookmarks","Open a website first.");return;}
   JSONArray a=readArray("bookmarks");for(int i=0;i<a.length();i++)if(u.equals(itemUrl(a,i))){Toast.makeText(this,"Already bookmarked",Toast.LENGTH_SHORT).show();return;}
   JSONObject obj=new JSONObject();try{obj.put("url",u);obj.put("title",active().getTitle()==null?friendlyUrl(u):active().getTitle());}catch(Exception ignored){}
   a.put(obj);storeArray("bookmarks",a);bookmarks();
  });
  add.setBackground(grad(0xff23557c,0xff153653,14));list.addView(add,new LinearLayout.LayoutParams(-1,dp(52)));
  JSONArray a=readArray("bookmarks");
  if(a.length()==0){TextView empty=text("Your saved websites will appear here.",14,GRAY);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(35),0,dp(30));list.addView(empty);}
  for(int i=0;i<a.length();i++){
   final int n=i;String u=itemUrl(a,i);LinearLayout row=row();row.setPadding(dp(13),dp(12),dp(9),dp(12));
   row.setBackground(grad(0xff152d49,0xff0d2239,14));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(9);list.addView(row,rp);
   LinearLayout info=column();row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
   TextView name=text(itemTitle(a,i),15,WHITE);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(name);
   info.addView(text(friendlyUrl(u),12,GRAY));info.setOnClickListener(v->{display();navigate(u);});
   TextView del=action("×",24,()->themed().setTitle("Remove bookmark?").setMessage(itemTitle(a,n))
    .setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,k)->{
     JSONArray b=new JSONArray();for(int x=0;x<a.length();x++)if(x!=n)b.put(a.opt(x));storeArray("bookmarks",b);bookmarks();
    }).show());row.addView(del,new LinearLayout.LayoutParams(dp(45),dp(46)));
  }
 }
 void network(){
  String type="Not connected";
  try{ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
   android.net.Network net=cm.getActiveNetwork();NetworkCapabilities caps=cm.getNetworkCapabilities(net);
   if(caps!=null)type=caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"Mobile data":"Other network";
  }catch(Exception ignored){}
  String local="Unavailable";try{for(java.util.Enumeration<java.net.NetworkInterface> nets=java.net.NetworkInterface.getNetworkInterfaces();nets.hasMoreElements();){java.net.NetworkInterface net=nets.nextElement();if(!net.isUp()||net.isLoopback())continue;for(java.util.Enumeration<java.net.InetAddress> addresses=net.getInetAddresses();addresses.hasMoreElements();){java.net.InetAddress ip=addresses.nextElement();if(ip instanceof java.net.Inet4Address&&!ip.isLoopbackAddress()){local=ip.getHostAddress();break;}}if(!local.equals("Unavailable"))break;}}catch(Exception ignored){}
  String info="Connection: "+type+"\nLocal IPv4: "+local+"\nTime zone: "+java.util.TimeZone.getDefault().getID()+"\nCountry (device locale): "+Locale.getDefault().getDisplayCountry()+"\nRegion (device locale): "+Locale.getDefault().toString();
  themed().setTitle("Network diagnostics").setMessage(info+"\n\nPublic IP and approximate region require a separate online lookup.")
   .setNegativeButton("Close",null).setPositiveButton("Check public IP",(d,n)->lookupPublicIp()).show();
 }

 void mediaDownloads(){
  active().evaluateJavascript("(function(){var out=[];var seen={};document.querySelectorAll('video,audio').forEach(function(m){var sources=[m].concat(Array.from(m.querySelectorAll('source')));sources.forEach(function(x){var u=x.currentSrc||x.src;if(!u||!/^https?:/i.test(u)||seen[u])return;seen[u]=1;out.push({url:u,type:(x.type||m.tagName).toLowerCase(),label:x.getAttribute('label')||x.getAttribute('res')||x.getAttribute('size')||''});});});return JSON.stringify(out);})()",raw->{
   try{
    String decoded=new JSONArray("["+raw+"]").optString(0,"[]");JSONArray found=new JSONArray(decoded);
    JSONArray valid=new JSONArray();ArrayList<String> names=new ArrayList<>();
    for(int i=0;i<found.length();i++){
     JSONObject o=found.optJSONObject(i);if(o==null)continue;String link=o.optString("url"),type=o.optString("type"),label=o.optString("label");
     if(link.contains(".m3u8")||link.contains(".mpd"))continue;
     if(!link.startsWith("http"))continue;
     boolean audio=type.contains("audio")||link.matches("(?i).*\\.(mp3|m4a|aac|wav|ogg)(\\?.*)?$");
     names.add((audio?"Audio":"Video")+" • "+(label.isEmpty()?"Original source":label));valid.put(o);
    }
    if(names.isEmpty()){dialog("Media downloads","No directly downloadable audio or video file was exposed by this page.");return;}
    themed().setTitle("Available media").setItems(names.toArray(new String[0]),(d,n)->{
     JSONObject o=valid.optJSONObject(n);String link=o.optString("url");String mime=o.optString("type");
     String filename=URLUtil.guessFileName(link,null,mime);
     themed().setTitle("Download media?").setMessage(names.get(n)+"\n"+filename)
      .setNegativeButton("Cancel",null).setPositiveButton("Download",(x,y)->startDownload(link,filename,mime)).show();
    }).setNegativeButton("Cancel",null).show();
   }catch(Exception e){dialog("Media downloads","This page did not expose a supported direct media file.");}
  });
 }
 void lookupPublicIp(){
  themed().setTitle("Public IP lookup").setMessage("This sends your public IP to ipwho.is to retrieve approximate IP-based country, region and city. Continue?")
   .setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,n)->{
    new Thread(()->{
     String result="Lookup unavailable";
     try{
      java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL("https://ipwho.is/").openConnection();
      c.setConnectTimeout(6500);c.setReadTimeout(6500);
      try(java.io.InputStream in=c.getInputStream();java.util.Scanner scan=new java.util.Scanner(in,"UTF-8").useDelimiter("\\A")){
       JSONObject data=new JSONObject(scan.hasNext()?scan.next():"{}");
       if(data.optBoolean("success",false))result="Public IP: "+data.optString("ip")+"\nCountry: "+data.optString("country")+"\nRegion: "+data.optString("region")+"\nCity: "+data.optString("city")+"\nTime zone: "+data.optJSONObject("timezone").optString("id");
      }finally{c.disconnect();}
     }catch(Exception ignored){}
     final String response=result;runOnUiThread(()->dialog("Public IP information",response+"\nLocation is approximate and based on public IP, not GPS."));
    }).start();
   }).show();
 }
 void downloads(){
  showingSettings=true;screen=8;head.removeAllViews();body.removeAllViews();sectionHeader("Downloads",this::display);
  ScrollView sc=new ScrollView(this);body.addView(sc);LinearLayout list=column();list.setPadding(dp(15),dp(12),dp(15),dp(22));sc.addView(list);
  String[] labels={"Today","Previous Day","Week","Monthly","Yearly","All","Choose date"};
  HorizontalScrollView horizontal=new HorizontalScrollView(this);horizontal.setHorizontalScrollBarEnabled(false);list.addView(horizontal);
  LinearLayout filters=row();horizontal.addView(filters);
  for(int i=0;i<labels.length;i++){final int n=i;TextView chip=action(labels[i],12,()->{
    if(n==6){Calendar now=Calendar.getInstance();new DatePickerDialog(this,(picker,y,m,d)->{
     Calendar chosen=Calendar.getInstance();chosen.set(y,m,d,0,0,0);chosen.set(Calendar.MILLISECOND,0);
     prefs.edit().putLong("downloadDate",chosen.getTimeInMillis()).apply();downloadFilter=6;downloads();
    },now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();}
    else{downloadFilter=n;downloads();}
   });chip.setPadding(dp(13),0,dp(13),0);chip.setBackground(grad(downloadFilter==i?0xff2373a8:PANEL,downloadFilter==i?0xff1a4f7a:PANEL,14));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(39));lp.rightMargin=dp(7);filters.addView(chip,lp);
  }
  JSONArray a=readArray("downloads");int shown=0;
  DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
  for(int i=a.length()-1;i>=0;i--){
   JSONObject item=a.optJSONObject(i);if(item==null)continue;long t=item.optLong("time"),id=item.optLong("id");
   if(!downloadMatches(t))continue;shown++;
   String name=item.optString("name","Downloaded file");
   LinearLayout card=row();card.setPadding(dp(14),dp(12),dp(10),dp(12));card.setBackground(grad(0xff193753,0xff10253e,13));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(9);list.addView(card,lp);
   LinearLayout info=column();card.addView(info,new LinearLayout.LayoutParams(0,-2,1));
   TextView title=text("↓  "+name,15,WHITE);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(title);
   String date=new java.text.SimpleDateFormat("MMM d, yyyy • h:mm a",Locale.getDefault()).format(new Date(t));
   int status=0;try( android.database.Cursor cursor=dm.query(new DownloadManager.Query().setFilterById(id))){
    if(cursor!=null&&cursor.moveToFirst())status=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
   }catch(Exception ignored){}
   String state=status==DownloadManager.STATUS_SUCCESSFUL?"Completed":status==DownloadManager.STATUS_FAILED?"Failed":status==DownloadManager.STATUS_RUNNING?"Downloading":"Pending";
   info.addView(text(date+" • "+state,12,GRAY));
   if(status==DownloadManager.STATUS_SUCCESSFUL)card.setOnClickListener(v->{
    try{Uri uri=dm.getUriForDownloadedFile(id);Intent open=new Intent(Intent.ACTION_VIEW);open.setDataAndType(uri,item.optString("mime","*/*"));open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(open);}
    catch(Exception e){dialog("Open download","No compatible app is available to open this file.");}
   });
  }
  if(shown==0){TextView empty=text("No downloads for this period",16,GRAY);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(8),dp(55),dp(8),dp(40));list.addView(empty);}
 }
 boolean downloadMatches(long t){
  Calendar start=Calendar.getInstance();start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);
  long today=start.getTimeInMillis();
  if(downloadFilter==0)return t>=today;
  if(downloadFilter==1)return t>=today-86400000L&&t<today;
  if(downloadFilter==2)return t>=today-7*86400000L;
  if(downloadFilter==3){start.set(Calendar.DAY_OF_MONTH,1);return t>=start.getTimeInMillis();}
  if(downloadFilter==4){start.set(Calendar.DAY_OF_YEAR,1);return t>=start.getTimeInMillis();}
  if(downloadFilter==6){long d=prefs.getLong("downloadDate",today);return t>=d&&t<d+86400000L;}
  return true;
 }
 void startDownload(String link,String filename,String mime){
  try{
   Uri u=Uri.parse(link);if(!"https".equals(u.getScheme())&&!"http".equals(u.getScheme())){dialog("Download unavailable","Only HTTP/HTTPS files are supported.");return;}
   DownloadManager.Request req=new DownloadManager.Request(u);
   req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
   req.setTitle(filename);if(mime!=null&&!mime.isEmpty())req.setMimeType(mime);
   String cookie=CookieManager.getInstance().getCookie(link);if(cookie!=null&&!cookie.isEmpty()&&!tabs.get(index).incognito)req.addRequestHeader("Cookie",cookie);
   req.addRequestHeader("User-Agent",active().getSettings().getUserAgentString());
   req.setDestinationInExternalFilesDir(this,Environment.DIRECTORY_DOWNLOADS,filename);
   long id=((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
   JSONArray a=readArray("downloads");JSONObject entry=new JSONObject();entry.put("id",id);entry.put("name",filename);entry.put("mime",mime==null?"*/*":mime);entry.put("time",System.currentTimeMillis());a.put(entry);storeArray("downloads",a);
   Toast.makeText(this,"Download started",Toast.LENGTH_SHORT).show();
  }catch(Exception e){dialog("Download failed","The file could not be queued.");}
 }
 void general(){
  showingSettings=true;screen=2;head.removeAllViews();body.removeAllViews();sectionHeader("General",this::settings);
  LinearLayout l=settingContent();
  settingRow(l,"Search engine","Google Search",()->dialog("Search engine","Google Search is selected for V1.0."));
  switchRow(l,"Restore last page","Reopen the last normal URL on launch","restore",true);
  switchRow(l,"Enable JavaScript","Required for many websites","javascript",true);
  switchRow(l,"Load images","Show images on websites","images",true);
  switchRow(l,"Desktop mode by default","Request desktop user agent for new tabs","desktop",false);
  settingRow(l,"Home page","MH Browser start page",()->dialog("Home page","MH Browser start page is the default home page."));
 }
 void siteSettings(){
  showingSettings=true;screen=4;head.removeAllViews();body.removeAllViews();sectionHeader("Site settings",this::settings);
  LinearLayout l=settingContent();
  switchRow(l,"Allow website location","Requires Android location permission; websites must ask","location",false);
  switchRow(l,"Allow JavaScript popups","Blocked by default","popups",false);
  settingRow(l,"Location accuracy","Uses Android location, not public IP location",()->dialog("Location","Device location and public IP geolocation are separate."));
 }
 void multiWindow(){
  themed().setTitle("Multi-window").setItems(new String[]{"Android split screen","Picture in picture"},(d,i)->{
   if(i==0)dialog("Split screen","Open Android Recent Apps, tap MH Browser's icon, and select Split screen if available.");
   else pictureInPicture();
  }).show();
 }
 void advanced(){
  showingSettings=true;screen=5;head.removeAllViews();body.removeAllViews();sectionHeader("Advanced",this::settings);
  LinearLayout l=settingContent();
  switchRow(l,"Enable zoom","Pinch to zoom on websites","zoom",true);

  settingRow(l,"Network diagnostics","View active connection type",this::network);
  settingRow(l,"Browsing history","View and delete visits by date",this::history);
  settingRow(l,"Browser engine","Android System WebView",()->dialog("Browser engine","MH Browser uses the installed Android System WebView, which is Chromium-based."));
 }
 void sectionHeader(String label,Runnable back){
 LinearLayout r=row();r.setPadding(dp(10),dp(12),dp(14),dp(12));r.setBackground(grad(0xff102940,0xff071326,12));head.addView(r);
 r.addView(action("‹",28,back),new LinearLayout.LayoutParams(dp(48),dp(45)));
 TextView t=text(label,22,WHITE);t.setTypeface(null,Typeface.BOLD);r.addView(t);
}
 LinearLayout settingContent(){ScrollView sc=new ScrollView(this);body.addView(sc);LinearLayout l=column();l.setPadding(dp(15),dp(10),dp(15),dp(20));sc.addView(l);return l;}

 void settings(){
  showingSettings=true;screen=1;head.removeAllViews();body.removeAllViews();
  LinearLayout h=row();h.setPadding(dp(12),dp(8),dp(16),dp(8));head.addView(h);
  h.addView(action("‹",28,this::display),new LinearLayout.LayoutParams(dp(45),dp(48)));
  TextView title=text("Settings",23,WHITE);title.setTypeface(null,Typeface.BOLD);h.addView(title);
  ScrollView sc=new ScrollView(this);sc.setVerticalScrollBarEnabled(false);body.addView(sc);
  LinearLayout list=column();list.setPadding(dp(15),dp(8),dp(15),dp(20));sc.addView(list);
  TextView general=text("GENERAL",12,0xff64caff);general.setPadding(dp(8),dp(15),0,dp(13));list.addView(general);
  settingRow(list,"⚙  General","Search, JavaScript, images and startup",this::general);
  settingRow(list,"◉  Privacy & Security","Tracker protection, cookies, safe browsing",()->privacy());
  settingRow(list,"⇄  Proxy settings","HTTP, HTTPS, SOCKS5 and optional credentials",this::proxy);
  settingRow(list,"▣  Site settings","Location access and website permissions",this::siteSettings);
  settingRow(list,"↓  Downloads","Android Download Manager",this::downloads);
  TextView more=text("BROWSING & DISPLAY",12,0xff64caff);more.setPadding(dp(8),dp(22),0,dp(13));list.addView(more);
  switchRow(list,"♫  Background playback","Allow supported media to continue in background","audio",true);
  settingRow(list,"▤  Multi-window / Split screen","Video-only PiP when fullscreen; Android split screen",this::multiWindow);
  switchRow(list,"⊘  Do Not Track","Send DNT: 1 on direct page navigation","dnt",false);
  settingRow(list,"⚙  Advanced","Zoom, network and history controls",this::advanced);
  settingRow(list,"ⓘ  About MH Browser","Version 1.0 • MH Developers",()->dialog("About MH Browser","MH Browser\nVersion 1.0\n\nDevelopers\nMH Developers (Masum DEV)"));
 }
 void privacy(){
  showingSettings=true;screen=3;head.removeAllViews();body.removeAllViews();
  LinearLayout h=row();h.setPadding(dp(12),dp(8),dp(12),dp(8));head.addView(h);
  h.addView(action("‹",28,this::settings),new LinearLayout.LayoutParams(dp(45),dp(48)));
  TextView title=text("Privacy & Security",22,WHITE);title.setTypeface(null,Typeface.BOLD);h.addView(title);
  ScrollView sc=new ScrollView(this);body.addView(sc);LinearLayout list=column();list.setPadding(dp(15),dp(15),dp(15),dp(20));sc.addView(list);
  switchRow(list,"Block known trackers","Limited built-in domain blocklist","trackers",true);
  switchRow(list,"Block third-party cookies","Takes effect when opening a new tab","blockCookies",true);
  settingRow(list,"Safe Browsing","Android WebView Safe Browsing is enabled",()->dialog("Safe Browsing","Helps protect against known unsafe sites."));
  settingRow(list,"HTTPS protection","Mixed HTTP content blocked on HTTPS pages",()->dialog("HTTPS","Connections are protected by HTTPS when supported. This does not hide your identity or IP address."));
  settingRow(list,"Clear browsing data","Delete cookies, history, cache and web storage",()->themed().setTitle("Clear browsing data?").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,n)->{CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();for(Tab t:tabs)t.web.clearCache(true);storeArray("history",new JSONArray());clearNavigationStacks();prefs.edit().remove("lastUrl").apply();}).show());
  settingRow(list,"Private browsing limitations","Incognito isolation is not yet production-ready",()->dialog("Incognito","Current private tabs may share site cookies with normal tabs. Do not use this preview for sensitive private sessions."));
 }


 void proxy(){
  LinearLayout c=column();c.setPadding(dp(18),dp(8),dp(18),dp(8));
  TextView info=text("Choose the proxy protocol and server. Authentication depends on WebView support; credentials are not saved.",13,GRAY);
  c.addView(info);
  TextView protocol=text("PROXY TYPE",12,0xff80d7ff);protocol.setPadding(0,dp(9),0,dp(6));c.addView(protocol);
  Spinner type=new Spinner(this);String[] protocols={"HTTP","HTTPS","SOCKS5"};
  ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,protocols);
  type.setAdapter(adapter);String saved=prefs.getString("proxyScheme","http");type.setSelection("https".equals(saved)?1:"socks5".equals(saved)?2:0);c.addView(type);
  EditText host=new EditText(this),port=new EditText(this),username=new EditText(this),password=new EditText(this);
  host.setSingleLine(true);port.setSingleLine(true);username.setSingleLine(true);password.setSingleLine(true);port.setInputType(2);
  password.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
  host.setHint("Proxy server / IP");port.setHint("Port (1–65535)");username.setHint("Username (optional)");password.setHint("Password (optional)");
  host.setText(prefs.getString("proxyHost",""));port.setText(prefs.getString("proxyPort",""));username.setText(prefs.getString("proxyUser",""));
  c.addView(host);c.addView(port);c.addView(username);c.addView(password);
  if(!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)){
   dialog("Proxy unavailable","Your Android System WebView does not support proxy override.");return;
  }
  AlertDialog d=themed().setTitle("Proxy settings").setView(c)
   .setNegativeButton("Cancel",null)
   .setNeutralButton("Disable proxy",(x,n)->ProxyController.getInstance().clearProxyOverride((command)->runOnUiThread(command),()->{
     proxyUser="";proxyPassword="";prefs.edit().putBoolean("proxyActive",false).apply();
     Toast.makeText(this,"Proxy disabled",Toast.LENGTH_SHORT).show();
   }))
   .setPositiveButton("Apply proxy",null).create();
  d.setOnShowListener(v->{styleDialog(d);d.getButton(-1).setOnClickListener(w->{
   String h=host.getText().toString().trim(),p=port.getText().toString().trim();
   if(h.isEmpty()||h.contains(" ")||h.contains("/")||h.contains("@")){host.setError("Enter a valid hostname or IP");return;}
   int number;try{number=Integer.parseInt(p);}catch(Exception e){port.setError("Invalid port");return;}
   if(number<1||number>65535){port.setError("Port must be 1–65535");return;}
   String scheme=new String[]{"http","https","socks5"}[type.getSelectedItemPosition()];
   ProxyConfig config;
   try{config=new ProxyConfig.Builder().addProxyRule(scheme+"://"+h+":"+number).build();}
   catch(Exception e){dialog("Proxy unsupported","The selected proxy type is not supported by your WebView.");return;}
   proxyUser=username.getText().toString().trim();proxyPassword=password.getText().toString();
   ProxyController.getInstance().setProxyOverride(config,(command)->runOnUiThread(command),()->{
    prefs.edit().putString("proxyHost",h).putString("proxyPort",p).putString("proxyScheme",scheme)
     .putString("proxyUser",proxyUser).putBoolean("proxyActive",true).apply();
    Toast.makeText(this,"Proxy rule applied • verify connection and authentication",Toast.LENGTH_LONG).show();
   });d.dismiss();
  });});d.show();
 }
 @Override public void onBackPressed(){
  if(inlineVideoFocus){restoreInlineVideo();return;}
  if(fullscreenVideo!=null){hideVideo();return;}
  if(screen==6||screen==7||screen==8){display();return;}if(screen>=2){settings();return;}if(screen==1){display();return;}
  if(active().canGoBack()){active().goBack();return;}
  if(!"about:blank".equals(active().getUrl())){navigate("about:blank");return;}
  themed().setTitle("Exit MH Browser?").setMessage("Are you sure you want to exit?")
   .setNegativeButton("Stay",null).setPositiveButton("Exit",(d,i)->finish()).show();
 }
 @Override protected void onPause(){saveTabs();super.onPause();if(!prefs.getBoolean("audio",false)&&!inPip)for(Tab t:tabs)t.web.onPause();CookieManager.getInstance().flush();}
 @Override protected void onResume(){super.onResume();for(Tab t:tabs)t.web.onResume();}
 @Override protected void onDestroy(){if(fullscreenVideo!=null)hideVideo();for(Tab t:tabs)if(t.incognito){t.web.clearHistory();t.web.clearCache(true);}if(privateOnly){CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();}super.onDestroy();}
}