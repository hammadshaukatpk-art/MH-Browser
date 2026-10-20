(function(){
 "use strict";
 const css="ins.adsbygoogle,.adsbygoogle,[data-ad-client],[data-ad-slot],.google-auto-placed,"+
 "[id^='google_ads_iframe'],[id^='div-gpt-ad'],iframe[src*='doubleclick.net'],"+
 "iframe[src*='googlesyndication.com'],.ad-banner,.advertisement,.ad-container,"+
 ".adblock-ad,.ad-unit,.ad-slot,.ad-wrapper,.sponsored-content,"+
 "ytd-display-ad-renderer,ytd-promoted-sparkles-web-renderer,ytd-in-feed-ad-layout-renderer,"+
 "yt-ad-slot-renderer,#player-ads,#masthead-ad,.ytp-ad-overlay-container{display:none!important;visibility:hidden!important;height:0!important}";
 function inject(){
  if(document.getElementById("mh-ad-style"))return;
  const style=document.createElement("style");style.id="mh-ad-style";style.textContent=css;
  (document.head||document.documentElement).appendChild(style);
 }
 if(document.documentElement)inject();
 else document.addEventListener("DOMContentLoaded",inject,{once:true});
 const seen=new Set();
 function media(url){
  if(!url||typeof url!=="string"||url.length>4096)return;
  try{
   const u=new URL(url,location.href);
   if(!/^https?:$/.test(u.protocol)||!/\.(mp4|m4v|webm|mov|mp3|m4a|aac|wav|ogg|opus|flac|m3u8)$/i.test(u.pathname)||seen.has(u.href))return;
   seen.add(u.href);
   browser.runtime.sendMessage({type:"media",url:u.href,referer:location.href}).catch(()=>{});
  }catch(e){}
 }
 function scan(){
  try{
   document.querySelectorAll("video,audio,video source,audio source").forEach(el=>media(el.currentSrc||el.src));
   performance.getEntriesByType("resource").slice(-150).forEach(x=>media(x.name));
  }catch(e){}
 }
 document.addEventListener("DOMContentLoaded",scan,{once:true});
 document.addEventListener("play",scan,true);
 document.addEventListener("pointerdown",e=>{
  try{
   if(e.target.closest("video,audio,.jwplayer,.plyr,.video-js,.dplayer,[class*='video-player'],[id*='video-player']"))
    browser.runtime.sendMessage({type:"playerTap"}).catch(()=>{});
  }catch(err){}
 },true);
 setInterval(scan,2000);
})();
