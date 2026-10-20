package com.mh.browser;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** WebView ad blocking: bundled community host rules plus safe cosmetic filters. */
final class AdBlockRules {
 private static volatile Set<String> blockedHosts=Collections.emptySet();
 private static final String[] CORE_AD_HOSTS={
  "doubleclick.net","googlesyndication.com","googleadservices.com",
  "adnxs.com","adsrvr.org","criteo.com","criteo.net",
  "taboola.com","outbrain.com","pubmatic.com","rubiconproject.com",
  "openx.net","smartadserver.com","adform.net","casalemedia.com",
  "contextweb.com","lijit.com","yieldmo.com","indexww.com","media.net",
  "mgid.com","popads.net","popcash.net","propellerads.com",
  "exoclick.com","exosrv.com","adsterra.com","onclickads.net",
  "hilltopads.net","revcontent.com","bidvertiser.com","adroll.com",
  "bidswitch.net","adskeeper.com","trafficjunky.net"
 };
 private static final String[] TRACKERS={
  "google-analytics.com","analytics.google.com","scorecardresearch.com",
  "hotjar.com","clarity.ms","mixpanel.com","segment.io",
  "amplitude.com","quantserve.com"
 };
 // Never interfere with core media/CDN playback or login resources.
 private static final String[] MEDIA_ALLOW={
  "googlevideo.com","youtube.com","youtu.be","ytimg.com","youtubei.googleapis.com",
  "facebook.com","fbcdn.net","fbsbx.com","instagram.com","cdninstagram.com",
  "tiktok.com","tiktokcdn.com","tiktokv.com","byteoversea.com",
  "netflix.com","nflxvideo.net","nflximg.net","nflxso.net",
  "spotify.com","scdn.co","soundcloud.com","sndcdn.com",
  "vimeo.com","vimeocdn.com","cloudfront.net","akamaized.net",
  "akamaihd.net","fastly.net","vercel.app"
 };
 private AdBlockRules(){}
 static void initialize(Context app){
  if(!blockedHosts.isEmpty())return;
  try(BufferedReader r=new BufferedReader(new InputStreamReader(
    app.getApplicationContext().getAssets().open("ad_hosts.txt"),"UTF-8"))){
   Set<String> parsed=new HashSet<>();
   String line;
   while((line=r.readLine())!=null){
    String host=line.trim().toLowerCase(Locale.ROOT);
    if(host.length()<4||host.length()>240||host.startsWith("#")||host.contains(" ")||host.contains("/"))continue;
    parsed.add(host);
   }
   blockedHosts=Collections.unmodifiableSet(parsed);
  }catch(Exception ignored){
   // Minimal built-in rules still apply to builds without the optional asset.
  }
 }
 private static boolean suffixMatch(String host,String[] rules){
  if(host==null)return false;
  for(String d:rules)if(host.equals(d)||host.endsWith("."+d))return true;
  return false;
 }
 private static boolean bundledMatch(String host){
  Set<String> list=blockedHosts;
  if(list.contains(host))return true;
  int i=host.indexOf('.');
  while(i>=0){
   String parent=host.substring(i+1);
   if(list.contains(parent))return true;
   i=host.indexOf('.',i+1);
  }
  return false;
 }
 static boolean shouldBlock(WebResourceRequest request,boolean adBlock,boolean trackers){
  if(request==null||request.isForMainFrame())return false;
  Uri uri=request.getUrl();
  if(uri==null)return false;
  String scheme=uri.getScheme();
  if(!"https".equalsIgnoreCase(scheme)&&!"http".equalsIgnoreCase(scheme))return false;
  String host=uri.getHost();
  if(host==null)return false;
  host=host.toLowerCase(Locale.ROOT);
  if(suffixMatch(host,MEDIA_ALLOW))return false;
  if(trackers&&suffixMatch(host,TRACKERS))return true;
  return adBlock&&(suffixMatch(host,CORE_AD_HOSTS)||bundledMatch(host));
 }
 static String cosmeticScript(){
  return "(function(){"+
   "var id='mh-browser-ad-css';"+
   "var old=document.getElementById(id);"+
   "if(!window.MHAdBlockEnabled){if(old)old.remove();return;}"+
   "if(old)return;"+
   "var css='"+
    "ins.adsbygoogle, .adsbygoogle, "+
    "[id^=\\\"google_ads_iframe\\\"], [id^=\\\"div-gpt-ad\\\"], "+
    ".ad-banner, .ad-container, .advertisement, .advertising-banner, "+
    "[data-ad-slot], [data-ad-client], "+
    "ytd-display-ad-renderer, ytd-promoted-sparkles-web-renderer, "+
    "ytd-in-feed-ad-layout-renderer, yt-ad-slot-renderer, "+
    "#player-ads, #masthead-ad, .ytp-ad-overlay-container"+
   "{display:none!important;visibility:hidden!important;height:0!important;min-height:0!important;}';"+
   "var el=document.createElement('style');el.id=id;el.textContent=css;"+
   "(document.head||document.documentElement).appendChild(el);"+
   "})()";
 }
}
