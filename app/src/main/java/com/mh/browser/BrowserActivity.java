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
public class BrowserActivity extends Activity {
 final int BG=0xff071326,PANEL=0xff10223a,BLUE=0xff269cff,WHITE=Color.WHITE,GRAY=0xffa4b6ce;
 LinearLayout root,head,body,bar; EditText address; boolean showingSettings=false;
 ArrayList<Tab> tabs=new ArrayList<>();int index=-1; boolean privateNext=false;
 android.content.SharedPreferences prefs;
 static class Tab {WebView web; boolean incognito; Tab(WebView w,boolean p){web=w;incognito=p;}}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
 GradientDrawable bg(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(24);return g;}
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("mh",0);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
 root=column();root.setBackgroundColor(BG);setContentView(root);if(Build.VERSION.SDK_INT>=35){root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets z=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(z.left,z.top,z.right,z.bottom);return insets;});}getWindow().getDecorView().setSystemUiVisibility(0);head=column();root.addView(head);body=column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));bar=row();bar.setBackgroundColor(PANEL);root.addView(bar);
 createTab(false);String saved=prefs.getBoolean("restore",true)?prefs.getString("lastUrl",""):"";if(!saved.isEmpty())navigate(saved);showSplash();}

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
 void navigate(String target){String dest=url(target);if(!tabs.get(index).incognito&&!dest.equals("about:blank"))prefs.edit().putString("lastUrl",dest).apply();if(dest.equals("about:blank")){active().loadUrl(dest);display();}else{body.removeAllViews();WebView w=active();if(w.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)w.getParent()).removeView(w);body.addView(w,new LinearLayout.LayoutParams(-1,-1));w.loadUrl(dest);display();}}

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
  TextView reload=action("⟳",25,()->active().reload());r.addView(reload,new LinearLayout.LayoutParams(dp(46),dp(48)));
 }

 void createTab(boolean inc){WebView w=new WebView(this);w.setBackgroundColor(WHITE);WebSettings s=w.getSettings();s.setJavaScriptEnabled(prefs.getBoolean("javascript",true));s.setLoadsImagesAutomatically(prefs.getBoolean("images",true));s.setDomStorageEnabled(!inc);s.setMediaPlaybackRequiresUserGesture(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setAllowFileAccess(false);s.setSafeBrowsingEnabled(true);s.setSupportZoom(prefs.getBoolean("zoom",true));s.setBuiltInZoomControls(prefs.getBoolean("zoom",true));s.setDisplayZoomControls(false);s.setSupportMultipleWindows(false);if(prefs.getBoolean("desktop",false))s.setUserAgentString(s.getUserAgentString().replace("Mobile","").replace("Android","X11; Linux x86_64"));if(inc)s.setCacheMode(WebSettings.LOAD_NO_CACHE);
 CookieManager.getInstance().setAcceptThirdPartyCookies(w,!inc&&!prefs.getBoolean("blockCookies",true));
 w.setWebChromeClient(new WebChromeClient(){@Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback cb){cb.invoke(origin,false,false);}});
 w.setWebViewClient(new WebViewClient(){@Override public void onPageStarted(WebView v,String u,android.graphics.Bitmap icon){if(v==active()&&!showingSettings)display();}@Override public void onPageFinished(WebView v,String u){if(v==active()&&address!=null)address.setText(u.equals("about:blank")?"":u);for(Tab t:tabs)if(t.web==v&&!t.incognito&&u.startsWith("http")){addHistory(u);break;}}@Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){if(!prefs.getBoolean("trackers",true))return null;String host=req.getUrl().getHost();if(host!=null&&(host.endsWith("doubleclick.net")||host.endsWith("google-analytics.com")||host.endsWith("facebook.net")))return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));return null;}});
 tabs.add(new Tab(w,inc));index=tabs.size()-1;display();}
 void display(){showingSettings=false;head.removeAllViews();tabStrip();body.removeAllViews();if(active().getUrl()==null||active().getUrl().equals("about:blank")){address=null;home();}else{header();WebView w=active();if(w.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)w.getParent()).removeView(w);body.addView(w,new LinearLayout.LayoutParams(-1,-1));address.setText(w.getUrl());}controls();}
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
  LinearLayout brand=row();brand.setGravity(Gravity.CENTER);c.addView(brand);
  ImageView logo=new ImageView(this);logo.setImageResource(com.mh.browser.R.drawable.app_logo);
  brand.addView(logo,new LinearLayout.LayoutParams(dp(70),dp(70)));
  LinearLayout brandWords=column();brandWords.setPadding(dp(12),0,0,0);brand.addView(brandWords);
  TextView title=text(tabs.get(index).incognito?"Private Browsing":"MH Browser",26,WHITE);title.setTypeface(null,Typeface.BOLD);brandWords.addView(title);
  brandWords.addView(text("Fast  •  Private  •  Secure",12,0xffa8bcd5));
  if(tabs.get(index).incognito){
    TextView warning=text("Private tab preview: full cookie isolation is not yet implemented.",13,0xffffd597);
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
  LinearLayout hero=column();hero.setPadding(dp(20),dp(20),dp(20),dp(20));
  hero.setBackgroundResource(R.drawable.hero_mountains);
  LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(174));hp.topMargin=dp(24);c.addView(hero,hp);
  TextView tag=text("EXPLORE MORE",11,0xff89eaff);tag.setTypeface(null,Typeface.BOLD);hero.addView(tag);
  TextView htitle=text("Discover the World",24,WHITE);htitle.setTypeface(null,Typeface.BOLD);
  LinearLayout.LayoutParams ht=new LinearLayout.LayoutParams(-1,-2);ht.topMargin=dp(14);hero.addView(htitle,ht);
  hero.addView(text("Explore, search and browse with MH.",13,0xffd5e8fa));
  TextView go=text("Explore  →",14,WHITE);go.setTypeface(null,Typeface.BOLD);
  LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,-2);gp.topMargin=dp(16);hero.addView(go,gp);
  hero.setOnClickListener(v->navigate("https://www.google.com"));
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

 void menu(){String[] choices={"New tab","New incognito tab","Tabs","Bookmarks","History","Downloads","Settings","Share page","Desktop site"};themed().setTitle("MH Browser").setItems(choices,(d,i)->{switch(i){case 0:createTab(false);break;case 1:createTab(true);break;case 2:tabList();break;case 3:dialog("Bookmarks","Bookmark manager planned for a future release.");break;case 4:dialog("History","Use Back and Forward to navigate saved session history.");break;case 5:dialog("Downloads","A download manager will be added in a future release.");break;case 6:settings();break;case 7:Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,active().getUrl());startActivity(Intent.createChooser(share,"Share page"));break;case 8:dialog("Desktop site","Desktop site toggle requires additional integration.");}}).show();}
 AlertDialog.Builder themed(){return new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert);}
 void dialog(String title,String message){themed().setTitle(title).setMessage(message).setPositiveButton("OK",null).show();}
 void settingRow(LinearLayout list,String label,String desc,Runnable act){LinearLayout c=column();c.setPadding(20,16,20,16);c.setBackground(bg(PANEL));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=9;list.addView(c,lp);c.addView(text(label,16,WHITE));c.addView(text(desc,12,GRAY));c.setOnClickListener(v->act.run());}
 void switchRow(LinearLayout list,String label,String desc,String pref,boolean def){LinearLayout r=row();r.setPadding(20,12,12,12);r.setBackground(bg(PANEL));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=9;list.addView(r,lp);LinearLayout c=column();r.addView(c,new LinearLayout.LayoutParams(0,-2,1));c.addView(text(label,16,WHITE));c.addView(text(desc,12,GRAY));Switch s=new Switch(this);s.setChecked(prefs.getBoolean(pref,def));r.addView(s);s.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean(pref,v).apply());}


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
  d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{
   String nm=name.getText().toString().trim(),u=link.getText().toString().trim();
   if(nm.isEmpty()){name.setError("Enter a name");return;}
   if(!u.startsWith("https://")&&!u.startsWith("http://")){link.setError("Enter an HTTP or HTTPS URL");return;}
   Uri parsed=Uri.parse(u);if(parsed.getHost()==null){link.setError("Invalid URL");return;}
   try{JSONArray a=shortcuts();JSONObject o=new JSONObject();o.put("name",nm);o.put("url",u);
    if(position<0)a.put(o);else a.put(position,o);storeArray("shortcuts",a);d.dismiss();display();
   }catch(Exception e){link.setError("Could not save shortcut");}
  }));d.show();
 }
 void addHistory(String u){
  if(u==null||!u.startsWith("http"))return;JSONArray old=readArray("history"),a=new JSONArray();a.put(u);
  for(int i=0;i<old.length()&&a.length()<100;i++){String v=old.optString(i);if(!u.equals(v))a.put(v);}storeArray("history",a);
 }
 void bookmarks(){
  String current=active().getUrl();JSONArray a=readArray("bookmarks");
  ArrayList<String> names=new ArrayList<>();names.add("+ Bookmark current page");for(int i=0;i<a.length();i++)names.add(a.optString(i));
  themed().setTitle("Bookmarks").setItems(names.toArray(new String[0]),(d,n)->{
   if(n==0){if(current!=null&&current.startsWith("http")){boolean exists=false;for(int i=0;i<a.length();i++)if(current.equals(a.optString(i)))exists=true;
     if(!exists){a.put(current);storeArray("bookmarks",a);}dialog("Saved","Page added to bookmarks.");}
     else dialog("Bookmarks","Open a website before bookmarking.");}
   else navigate(a.optString(n-1));
  }).setNegativeButton("Close",null).show();
 }
 void history(){
  JSONArray a=readArray("history");String[] names=new String[a.length()];for(int i=0;i<a.length();i++)names[i]=a.optString(i);
  themed().setTitle("History").setItems(names,(d,n)->navigate(a.optString(n))).setNeutralButton("Clear",(d,n)->{storeArray("history",new JSONArray());}).setNegativeButton("Close",null).show();
 }
 void network(){
  String type="Not connected";
  try{ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
   android.net.Network net=cm.getActiveNetwork();NetworkCapabilities caps=cm.getNetworkCapabilities(net);
   if(caps!=null)type=caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"Mobile data":"Other network";
  }catch(Exception ignored){}
  dialog("Network information","Connection: "+type+"\nPublic IP and IP-based city cannot be reliably determined offline. Device GPS location and network IP location are different.");
 }
 void downloads(){dialog("Downloads","Direct file downloads use Android Download Manager. Downloads from streaming/DRM websites or video quality selection are not generally available.");}
 void general(){
  head.removeAllViews();body.removeAllViews();sectionHeader("General",this::settings);
  LinearLayout l=settingContent();
  settingRow(l,"Search engine","Google Search",()->dialog("Search engine","Google Search is selected for V1.0."));
  switchRow(l,"Restore last page","Reopen the last normal URL on launch","restore",true);
  switchRow(l,"Enable JavaScript","Required for many websites","javascript",true);
  switchRow(l,"Load images","Show images on websites","images",true);
  switchRow(l,"Desktop mode by default","Request desktop user agent for new tabs","desktop",false);
  settingRow(l,"Home page","MH Browser start page",()->dialog("Home page","MH Browser start page is the default home page."));
 }
 void advanced(){
  head.removeAllViews();body.removeAllViews();sectionHeader("Advanced",this::settings);
  LinearLayout l=settingContent();
  switchRow(l,"Enable zoom","Pinch to zoom on websites","zoom",true);
  switchRow(l,"Allow third-party cookies","Control is in Privacy & Security","dummy",false);
  settingRow(l,"Network diagnostics","View active connection type",this::network);
  settingRow(l,"Clear history","Remove saved visited-page list",()->themed().setTitle("Clear history?").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,n)->storeArray("history",new JSONArray())).show());
  settingRow(l,"Browser engine","Android System WebView",()->dialog("Browser engine","MH Browser uses the installed Android System WebView, which is Chromium-based."));
 }
 void sectionHeader(String label,Runnable back){LinearLayout r=row();r.setPadding(dp(10),dp(10),dp(12),dp(10));head.addView(r);r.addView(action("‹",28,back),new LinearLayout.LayoutParams(dp(48),dp(45)));TextView t=text(label,22,WHITE);t.setTypeface(null,Typeface.BOLD);r.addView(t);}
 LinearLayout settingContent(){ScrollView sc=new ScrollView(this);body.addView(sc);LinearLayout l=column();l.setPadding(dp(15),dp(10),dp(15),dp(20));sc.addView(l);return l;}

 void settings(){
  head.removeAllViews();body.removeAllViews();
  LinearLayout h=row();h.setPadding(dp(12),dp(8),dp(16),dp(8));head.addView(h);
  h.addView(action("‹",28,this::display),new LinearLayout.LayoutParams(dp(45),dp(48)));
  TextView title=text("Settings",23,WHITE);title.setTypeface(null,Typeface.BOLD);h.addView(title);
  ScrollView sc=new ScrollView(this);sc.setVerticalScrollBarEnabled(false);body.addView(sc);
  LinearLayout list=column();list.setPadding(dp(15),dp(8),dp(15),dp(20));sc.addView(list);
  TextView general=text("GENERAL",12,0xff64caff);general.setPadding(dp(8),dp(15),0,dp(13));list.addView(general);
  settingRow(list,"⚙  General","Google search • appearance • home page",()->dialog("General","MH Browser V1.0 uses Google Search. More customization is planned."));
  settingRow(list,"◉  Privacy & Security","Tracker protection, cookies, safe browsing",()->privacy());
  settingRow(list,"⇄  Proxy settings","HTTP / HTTPS / SOCKS5 configuration",this::proxy);
  settingRow(list,"▣  Site settings","Location, permissions, notifications",()->dialog("Site settings","Location requests are currently blocked. Per-site permissions are not yet implemented."));
  settingRow(list,"↓  Downloads","Files and download location",()->dialog("Downloads","Download manager is not yet implemented."));
  TextView more=text("BROWSING & DISPLAY",12,0xff64caff);more.setPadding(dp(8),dp(22),0,dp(13));list.addView(more);
  switchRow(list,"♫  Background playback","Keep media playing when minimized (site dependent)","audio",false);
  settingRow(list,"▤  Multi-window / Split screen","Use Android's split-screen controls",()->dialog("Multi-window","Open Android Recent Apps and select Split screen if supported."));
  settingRow(list,"⊘  Do Not Track","Advisory request; websites can ignore it",()->dialog("Do Not Track","Do Not Track is an advisory preference, not a guarantee of anonymity."));
  settingRow(list,"⚙  Advanced","Browser information and experimental options",()->dialog("Advanced","Additional advanced options are under development."));
  settingRow(list,"ⓘ  About MH Browser","Version 1.0 • Android",()->dialog("MH Browser","V1.0 test build. Advanced privacy, proxy routing, and background media need further work."));
 }
 void privacy(){
  head.removeAllViews();body.removeAllViews();
  LinearLayout h=row();h.setPadding(dp(12),dp(8),dp(12),dp(8));head.addView(h);
  h.addView(action("‹",28,this::settings),new LinearLayout.LayoutParams(dp(45),dp(48)));
  TextView title=text("Privacy & Security",22,WHITE);title.setTypeface(null,Typeface.BOLD);h.addView(title);
  ScrollView sc=new ScrollView(this);body.addView(sc);LinearLayout list=column();list.setPadding(dp(15),dp(15),dp(15),dp(20));sc.addView(list);
  switchRow(list,"Block known trackers","Limited built-in domain blocklist","trackers",true);
  switchRow(list,"Block third-party cookies","Takes effect when opening a new tab","blockCookies",true);
  settingRow(list,"Safe Browsing","Android WebView Safe Browsing is enabled",()->dialog("Safe Browsing","Helps protect against known unsafe sites."));
  settingRow(list,"HTTPS protection","Mixed HTTP content blocked on HTTPS pages",()->dialog("HTTPS","Connections are protected by HTTPS when supported. This does not hide your identity or IP address."));
  settingRow(list,"Clear browsing data","Delete cookies, cache and web storage",()->themed().setTitle("Clear browsing data?").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,n)->{CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();for(Tab t:tabs)t.web.clearCache(true);prefs.edit().remove("lastUrl").apply();}).show());
  settingRow(list,"Private browsing limitations","Incognito isolation is not yet production-ready",()->dialog("Incognito","Current private tabs may share site cookies with normal tabs. Do not use this preview for sensitive private sessions."));
 }

 void proxy(){LinearLayout list=column();list.setPadding(24,8,24,4);Spinner scheme=new Spinner(this);scheme.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"HTTP","HTTPS","SOCKS5"}));list.addView(scheme);EditText h=new EditText(this),p=new EditText(this),u=new EditText(this),pw=new EditText(this);h.setHint("Host / IP");p.setHint("Port");p.setInputType(2);u.setHint("Username (optional)");pw.setHint("Password (optional)");pw.setInputType(129);list.addView(h);list.addView(p);list.addView(u);list.addView(pw);themed().setTitle("Proxy settings (not active)").setMessage("WebView cannot reliably apply authenticated HTTP/SOCKS proxies via its public API. Saved values are not used for network traffic.").setView(list).setPositiveButton("Save inactive settings",(d,i)->{prefs.edit().putString("proxyHost",h.getText().toString()).putString("proxyPort",p.getText().toString()).putString("proxyType",scheme.getSelectedItem().toString()).apply();Toast.makeText(this,"Saved, proxy is NOT active",Toast.LENGTH_LONG).show();}).setNegativeButton("Cancel",null).show();}
 @Override public void onBackPressed(){if(active().canGoBack())active().goBack();else if(!"about:blank".equals(active().getUrl()))navigate("about:blank");else themed().setTitle("Exit MH Browser?").setMessage("Are you sure you want to exit?").setNegativeButton("Cancel",null).setPositiveButton("Exit",(d,i)->finish()).show();}
 @Override protected void onPause(){super.onPause();if(!prefs.getBoolean("audio",false))for(Tab t:tabs)t.web.onPause();CookieManager.getInstance().flush();}
 @Override protected void onResume(){super.onResume();for(Tab t:tabs)t.web.onResume();}
 @Override protected void onDestroy(){for(Tab t:tabs)if(t.incognito){t.web.clearHistory();t.web.clearCache(true);}super.onDestroy();}
}