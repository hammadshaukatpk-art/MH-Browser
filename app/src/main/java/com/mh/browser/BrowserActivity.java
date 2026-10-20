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
public class BrowserActivity extends Activity {
 final int BG=0xff071326,PANEL=0xff10223a,BLUE=0xff269cff,WHITE=Color.WHITE,GRAY=0xffa4b6ce;
 LinearLayout root,head,body,bar; EditText address;
 ArrayList<Tab> tabs=new ArrayList<>();int index=-1; boolean privateNext=false;
 android.content.SharedPreferences prefs;
 static class Tab {WebView web; boolean incognito; Tab(WebView w,boolean p){web=w;incognito=p;}}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
 GradientDrawable bg(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(24);return g;}
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("mh",0);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
 root=column();root.setBackgroundColor(BG);setContentView(root);if(Build.VERSION.SDK_INT>=35){root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets z=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(z.left,z.top,z.right,z.bottom);return insets;});}getWindow().getDecorView().setSystemUiVisibility(0);head=column();root.addView(head);body=column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));bar=row();bar.setBackgroundColor(PANEL);root.addView(bar);
 createTab(false);String saved=prefs.getString("lastUrl","");if(!saved.isEmpty())navigate(saved);}

 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 GradientDrawable grad(int a,int b,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});g.setCornerRadius(dp(radius));return g;}
 TextView action(String glyph,int size,Runnable run){TextView t=text(glyph,size,WHITE);t.setGravity(Gravity.CENTER);t.setOnClickListener(v->run.run());return t;}
 void nav(String s,Runnable run){TextView t=action(s,25,run);bar.addView(t,new LinearLayout.LayoutParams(0,dp(58),1));}
 void controls(){bar.removeAllViews();bar.setPadding(dp(8),dp(2),dp(8),dp(2));nav("‹",()->{if(active().canGoBack())active().goBack();else navigate("about:blank");});nav("›",()->{if(active().canGoForward())active().goForward();});nav("⌂",()->navigate("about:blank"));nav("▣",this::tabList);nav("☰",this::menu);}

 WebView active(){return tabs.get(index).web;}
 String url(String s){s=s.trim();if(s.isEmpty())return "about:blank";if(s.startsWith("https://")||s.startsWith("http://")||s.equals("about:blank"))return s;if(s.contains(".")&&!s.contains(" "))return "https://"+s;return "https://www.google.com/search?q="+Uri.encode(s);}
 void navigate(String target){String dest=url(target);if(!tabs.get(index).incognito&&!dest.equals("about:blank"))prefs.edit().putString("lastUrl",dest).apply();if(dest.equals("about:blank")){active().loadUrl(dest);display();}else{body.removeAllViews();WebView w=active();if(w.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)w.getParent()).removeView(w);body.addView(w,new LinearLayout.LayoutParams(-1,-1));w.loadUrl(dest);}}

 void header(){
  head.removeAllViews();head.setBackgroundColor(BG);
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

 void createTab(boolean inc){WebView w=new WebView(this);w.setBackgroundColor(WHITE);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(!inc);s.setMediaPlaybackRequiresUserGesture(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setAllowFileAccess(false);s.setSafeBrowsingEnabled(true);s.setSupportZoom(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);s.setSupportMultipleWindows(false);if(inc)s.setCacheMode(WebSettings.LOAD_NO_CACHE);
 CookieManager.getInstance().setAcceptThirdPartyCookies(w,!inc&&!prefs.getBoolean("blockCookies",true));
 w.setWebChromeClient(new WebChromeClient(){@Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback cb){cb.invoke(origin,false,false);}});
 w.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String u){if(v==active()&&address!=null)address.setText(u.equals("about:blank")?"":u);}@Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){if(!prefs.getBoolean("trackers",true))return null;String host=req.getUrl().getHost();if(host!=null&&(host.endsWith("doubleclick.net")||host.endsWith("google-analytics.com")||host.endsWith("facebook.net")))return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));return null;}});
 tabs.add(new Tab(w,inc));index=tabs.size()-1;display();}
 void display(){header();body.removeAllViews();if(active().getUrl()==null||active().getUrl().equals("about:blank"))home();else{WebView w=active();if(w.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)w.getParent()).removeView(w);body.addView(w,new LinearLayout.LayoutParams(-1,-1));address.setText(w.getUrl());}controls();}

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
  TextView edit=text("EDIT",12,0xff62caff);section.addView(edit);edit.setOnClickListener(v->dialog("Shortcuts","Shortcut editing is planned for a future update."));
  String[] labels={"Google","YouTube","Amazon","eBay","AliExpress","Facebook","X","Add"};
  String[] symbols={"G","▶","a","e","A","f","𝕏","+"};
  int[] colors={0xff4285f4,0xffed2939,0xffff9c24,0xff3285dd,0xfffa6948,0xff2465d1,0xff18273d,0xff183954};
  String[] links={"https://www.google.com","https://www.youtube.com","https://www.amazon.com","https://www.ebay.com","https://www.aliexpress.com","https://www.facebook.com","https://x.com","about:blank"};
  for(int rowIndex=0;rowIndex<2;rowIndex++){
    LinearLayout line=row();line.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams llp=new LinearLayout.LayoutParams(-1,-2);llp.topMargin=dp(18);c.addView(line,llp);
    for(int j=0;j<4;j++){
      final int n=rowIndex*4+j;LinearLayout item=column();item.setGravity(Gravity.CENTER);
      line.addView(item,new LinearLayout.LayoutParams(0,dp(86),1));
      TextView circle=text(symbols[n],n==1?21:23,WHITE);circle.setTypeface(null,Typeface.BOLD);circle.setGravity(Gravity.CENTER);
      circle.setBackground(grad(colors[n],colors[n]==0xff183954?0xff26507c:colors[n],20));
      item.addView(circle,new LinearLayout.LayoutParams(dp(47),dp(47)));
      TextView lab=text(labels[n],11,0xffd7e7fb);lab.setGravity(Gravity.CENTER);
      LinearLayout.LayoutParams la=new LinearLayout.LayoutParams(-1,dp(30));item.addView(lab,la);
      item.setOnClickListener(v->{if(n==7)createTab(false);else navigate(links[n]);});
    }
  }
  LinearLayout hero=column();hero.setPadding(dp(20),dp(20),dp(20),dp(20));
  hero.setBackground(grad(0xff24548b,0xff0b264b,20));
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

 void menu(){String[] choices={"New tab","New incognito tab","Tabs","Bookmarks","History","Downloads","Settings","Share page","Desktop site"};new AlertDialog.Builder(this).setTitle("MH Browser").setItems(choices,(d,i)->{switch(i){case 0:createTab(false);break;case 1:createTab(true);break;case 2:tabList();break;case 3:dialog("Bookmarks","Bookmark manager planned for a future release.");break;case 4:dialog("History","Use Back and Forward to navigate saved session history.");break;case 5:dialog("Downloads","A download manager will be added in a future release.");break;case 6:settings();break;case 7:Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,active().getUrl());startActivity(Intent.createChooser(share,"Share page"));break;case 8:dialog("Desktop site","Desktop site toggle requires additional integration.");}}).show();}
 void dialog(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK",null).show();}
 void settingRow(LinearLayout list,String label,String desc,Runnable act){LinearLayout c=column();c.setPadding(20,16,20,16);c.setBackground(bg(PANEL));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=9;list.addView(c,lp);c.addView(text(label,16,WHITE));c.addView(text(desc,12,GRAY));c.setOnClickListener(v->act.run());}
 void switchRow(LinearLayout list,String label,String desc,String pref,boolean def){LinearLayout r=row();r.setPadding(20,12,12,12);r.setBackground(bg(PANEL));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=9;list.addView(r,lp);LinearLayout c=column();r.addView(c,new LinearLayout.LayoutParams(0,-2,1));c.addView(text(label,16,WHITE));c.addView(text(desc,12,GRAY));Switch s=new Switch(this);s.setChecked(prefs.getBoolean(pref,def));r.addView(s);s.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean(pref,v).apply());}

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
  settingRow(list,"Clear browsing data","Delete cookies, cache and web storage",()->new AlertDialog.Builder(this).setTitle("Clear browsing data?").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,n)->{CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();for(Tab t:tabs)t.web.clearCache(true);prefs.edit().remove("lastUrl").apply();}).show());
  settingRow(list,"Private browsing limitations","Incognito isolation is not yet production-ready",()->dialog("Incognito","Current private tabs may share site cookies with normal tabs. Do not use this preview for sensitive private sessions."));
 }

 void proxy(){LinearLayout list=column();list.setPadding(24,8,24,4);Spinner scheme=new Spinner(this);scheme.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"HTTP","HTTPS","SOCKS5"}));list.addView(scheme);EditText h=new EditText(this),p=new EditText(this),u=new EditText(this),pw=new EditText(this);h.setHint("Host / IP");p.setHint("Port");p.setInputType(2);u.setHint("Username (optional)");pw.setHint("Password (optional)");pw.setInputType(129);list.addView(h);list.addView(p);list.addView(u);list.addView(pw);new AlertDialog.Builder(this).setTitle("Proxy settings (not active)").setMessage("WebView cannot reliably apply authenticated HTTP/SOCKS proxies via its public API. Saved values are not used for network traffic.").setView(list).setPositiveButton("Save inactive settings",(d,i)->{prefs.edit().putString("proxyHost",h.getText().toString()).putString("proxyPort",p.getText().toString()).putString("proxyType",scheme.getSelectedItem().toString()).apply();Toast.makeText(this,"Saved, proxy is NOT active",Toast.LENGTH_LONG).show();}).setNegativeButton("Cancel",null).show();}
 @Override public void onBackPressed(){if(active().canGoBack())active().goBack();else if(!"about:blank".equals(active().getUrl()))navigate("about:blank");else new AlertDialog.Builder(this).setTitle("Exit MH Browser?").setMessage("Are you sure you want to exit?").setNegativeButton("Cancel",null).setPositiveButton("Exit",(d,i)->finish()).show();}
 @Override protected void onPause(){super.onPause();if(!prefs.getBoolean("audio",false))for(Tab t:tabs)t.web.onPause();CookieManager.getInstance().flush();}
 @Override protected void onResume(){super.onResume();for(Tab t:tabs)t.web.onResume();}
 @Override protected void onDestroy(){for(Tab t:tabs)if(t.incognito){t.web.clearHistory();t.web.clearCache(true);}super.onDestroy();}
}