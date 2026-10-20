package com.mh.browser;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.ContentBlocking;
import java.util.ArrayList;
import java.util.List;

/**
 * EXPERIMENTAL GeckoView integration. Deliberately not the launcher.
 * This preview must NOT replace the production WebView until every feature
 * (downloads, media, history, privacy, proxy, tabs and permissions) is ported.
 */
public final class GeckoBrowserActivity extends Activity {
 private static final int BG=0xff071a2e, PANEL=0xff112740, BLUE=0xff75d6ff;
 private static GeckoRuntime runtime;
 private final List<GeckoSession> tabs=new ArrayList<>();
 private GeckoView view;
 private EditText address;
 private int selected=-1;
 private boolean canBack,canForward;
 private SharedPreferences prefs;

 private int dp(int v){return (int)(getResources().getDisplayMetrics().density*v+0.5f);}
 private TextView button(String label, Runnable action){
  TextView text=new TextView(this);text.setText(label);text.setTextColor(Color.WHITE);
  text.setTextSize(21);text.setGravity(Gravity.CENTER);
  text.setOnClickListener(v->action.run());return text;
 }
 private void addButton(LinearLayout bar,String label,Runnable action){
  bar.addView(button(label,action),new LinearLayout.LayoutParams(0,dp(55),1));
 }
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  prefs=getSharedPreferences("mh",0);
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(BG);setContentView(root);
  LinearLayout top=new LinearLayout(this);top.setPadding(dp(12),dp(10),dp(12),dp(10));
  root.addView(top,new LinearLayout.LayoutParams(-1,dp(72)));
  address=new EditText(this);address.setSingleLine(true);
  address.setTextColor(Color.WHITE);address.setHintTextColor(0xff9eb6cd);
  address.setHint("Search or enter URL");address.setTextSize(15);
  address.setImeOptions(EditorInfo.IME_ACTION_GO);
  address.setBackgroundColor(PANEL);
  top.addView(address,new LinearLayout.LayoutParams(0,-1,1));
  top.addView(button("↻",()->current().reload()),new LinearLayout.LayoutParams(dp(48),-1));
  address.setOnEditorActionListener((v,action,event)->{navigate(address.getText().toString());return true;});
  view=new GeckoView(this);view.setBackgroundColor(Color.WHITE);
  root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout bottom=new LinearLayout(this);bottom.setBackgroundColor(PANEL);
  root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(60)));
  addButton(bottom,"‹",()->{if(canBack)current().goBack();});
  addButton(bottom,"›",()->{if(canForward)current().goForward();});
  addButton(bottom,"⌂",()->navigate("https://www.google.com"));
  addButton(bottom,"▣",()->newTab("https://www.google.com"));
  addButton(bottom,"☰",this::tabPicker);

  if(runtime==null){
   ContentBlocking.Settings blocking=new ContentBlocking.Settings.Builder()
    .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
    .strictSocialTrackingProtection(true)
    .queryParameterStrippingEnabled(true).build();
   runtime=GeckoRuntime.create(getApplicationContext(),
    new GeckoRuntimeSettings.Builder().contentBlocking(blocking).build());
  }
  runtime.getWebExtensionController()
   .ensureBuiltIn("resource://android/assets/mh_adblock/","mh-adblock@mh-browser")
   .accept(ext->{},error->android.util.Log.w("MHGecko","Ad extension unavailable",error));
  newTab("https://www.google.com");
 }
 private GeckoSession current(){return tabs.get(selected);}
 private void navigate(String input){
  String url=input.trim();
  if(url.isEmpty())return;
  if(!url.startsWith("https://")&&!url.startsWith("http://")){
   url=url.contains(".")&&!url.contains(" ")?"https://"+url:
    "https://www.google.com/search?q="+Uri.encode(url);
  }
  current().loadUri(url);
  address.clearFocus();
 }
 private void newTab(String url){
  GeckoSession session=new GeckoSession();
  session.setContentDelegate(new GeckoSession.ContentDelegate(){});
  session.setNavigationDelegate(new GeckoSession.NavigationDelegate(){
   @Override public void onLocationChange(GeckoSession s,String location,
     List<GeckoSession.PermissionDelegate.ContentPermission> permissions,Boolean gesture){
    if(s==current()&&location!=null)address.setText(location);
   }
   @Override public void onCanGoBack(GeckoSession s,boolean can){
    if(s==current())canBack=can;
   }
   @Override public void onCanGoForward(GeckoSession s,boolean can){
    if(s==current())canForward=can;
   }
  });
  session.open(runtime);
  tabs.add(session);select(tabs.size()-1);
  session.loadUri(url);
 }
 private void select(int index){
  if(index<0||index>=tabs.size())return;
  selected=index;view.setSession(current());
  canBack=false;canForward=false;
 }
 private void tabPicker(){
  String[] names=new String[tabs.size()+1];
  for(int i=0;i<tabs.size();i++)names[i]="Tab "+(i+1);
  names[tabs.size()]="＋ New tab";
  new android.app.AlertDialog.Builder(this).setTitle("Gecko preview tabs")
   .setItems(names,(d,which)->{
    if(which==tabs.size())newTab("https://www.google.com");
    else select(which);
   }).setNegativeButton("Close",null).show();
 }
 @Override public void onBackPressed(){
  if(selected>=0&&canBack)current().goBack();else super.onBackPressed();
 }
 @Override protected void onDestroy(){
  for(GeckoSession session:tabs)session.close();
  tabs.clear();super.onDestroy();
 }
}
