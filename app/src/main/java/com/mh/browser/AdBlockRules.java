package com.mh.browser;

import android.net.Uri;
import android.webkit.WebResourceRequest;
import java.util.Locale;

/**
 * Conservative ad/tracker host filter for WebView subresources.
 * Never blocks top-level navigation, first-party media, or CDN domains
 * that can carry legitimate video and authentication resources.
 */
final class AdBlockRules {
 private static final String[] AD_HOSTS={
  "doubleclick.net","googlesyndication.com","googleadservices.com",
  "adservice.google.com","adservice.google.co.uk","adnxs.com",
  "adsrvr.org","criteo.com","criteo.net","taboola.com","outbrain.com",
  "pubmatic.com","rubiconproject.com","openx.net","smartadserver.com",
  "adform.net","casalemedia.com","contextweb.com","lijit.com",
  "yieldmo.com","indexww.com","media.net","mgid.com",
  "popads.net","popcash.net","propellerads.com","exoclick.com",
  "exosrv.com","adsterra.com","onclickads.net","hilltopads.net",
  "revcontent.com","bidvertiser.com","adroll.com","bidswitch.net",
  "adskeeper.co.uk","adskeeper.com","trafficjunky.net"
 };
 private static final String[] TRACKER_HOSTS={
  "google-analytics.com","analytics.google.com","scorecardresearch.com",
  "hotjar.com","clarity.ms","mixpanel.com","segment.io",
  "amplitude.com","quantserve.com"
 };
 private AdBlockRules(){}
 private static boolean matches(String host,String[] rules){
  if(host==null)return false;
  host=host.toLowerCase(Locale.ROOT);
  for(String domain:rules)if(host.equals(domain)||host.endsWith("."+domain))return true;
  return false;
 }
 static boolean shouldBlock(WebResourceRequest request,boolean adBlock,boolean trackers){
  if(request==null||request.isForMainFrame())return false;
  Uri uri=request.getUrl();
  if(uri==null)return false;
  String scheme=uri.getScheme();
  if(!"https".equalsIgnoreCase(scheme)&&!"http".equalsIgnoreCase(scheme))return false;
  String host=uri.getHost();
  return (adBlock&&matches(host,AD_HOSTS))||(trackers&&matches(host,TRACKER_HOSTS));
 }
}
