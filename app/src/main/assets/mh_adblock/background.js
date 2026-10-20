"use strict";
let blocked = new Set();
let pathRules = new Map();
let enabled = true;
const coreMedia = ["googlevideo.com","ytimg.com","youtubei.googleapis.com","fbcdn.net","cdninstagram.com","tiktokcdn.com","tiktokv.com","nflxvideo.net","nflximg.net","vimeocdn.com"];
const ends = (h,d) => h===d || h.endsWith("."+d);
const media = h => coreMedia.some(d=>ends(h,d));
const adEndpoint = /^\/(pagead|ads|adserver|adservice|advertising|ad_delivery|ad-delivery|preroll|midroll|vast|vmap)\//;
async function load(){
 try {
  const text = await (await fetch(browser.runtime.getURL("ad_domains.txt"))).text();
  blocked = new Set(text.split(/\r?\n/).filter(s=>s&&s[0]!=="#"));
  const paths = await (await fetch(browser.runtime.getURL("ad_paths.txt"))).text();
  pathRules = new Map();
  for(const line of paths.split(/\r?\n/)){
   const sep=line.indexOf("|");if(sep<2)continue;
   const host=line.slice(0,sep),token=line.slice(sep+1);
   if(!pathRules.has(host))pathRules.set(host,[]);
   pathRules.get(host).push(token);
  }
 }catch(e){console.error("MH Browser filters unavailable",e);}
}
const loaded=load();
browser.storage.local.get({enabled:true}).then(v=>enabled=v.enabled!==false);
browser.storage.onChanged.addListener((changes,area)=>{
 if(area==="local"&&changes.enabled)enabled=changes.enabled.newValue!==false;
});
function domainBlocked(host){
 let h=host;
 while(h.includes(".")){
  if(blocked.has(h))return true;
  h=h.slice(h.indexOf(".")+1);
 }
 return false;
}
function pathBlocked(host,path){
 let h=host;
 while(h.includes(".")){
  const rules=pathRules.get(h);
  if(rules&&rules.some(x=>path.includes(x)))return true;
  h=h.slice(h.indexOf(".")+1);
 }
 return false;
}
const knownMedia = /\.(mp4|m4v|webm|mov|mp3|m4a|aac|wav|ogg|opus|flac|m3u8)(\?|$)/i;
function notify(message){
 try{browser.runtime.sendNativeMessage("mh_browser",message).catch(()=>{});}catch(e){}
}
browser.webRequest.onBeforeRequest.addListener(details=>{
 if(!/^https?:/i.test(details.url))return {};
 let u;
 try{u=new URL(details.url);}catch(e){return {};}
 const host=u.hostname.toLowerCase();
 if(knownMedia.test(u.pathname)&&details.type==="media")
  notify({type:"media",url:details.url,referer:details.originUrl||details.documentUrl||"",tabId:details.tabId});
 if(!enabled||details.type==="main_frame")return {};
 if(media(host))return {};
 if(adEndpoint.test(u.pathname)||domainBlocked(host)||pathBlocked(host,u.pathname+u.search))return {cancel:true};
 return {};
},{urls:["<all_urls>"]},["blocking"]);
// Also capture signed player URLs that have no .mp4 or .m3u8 suffix.
browser.webRequest.onHeadersReceived.addListener(details=>{
 if(!/^https?:/i.test(details.url)||!["media","xmlhttprequest","other"].includes(details.type))return;
 const content=(details.responseHeaders||[]).find(x=>x.name&&x.name.toLowerCase()==="content-type");
 const mime=content&&content.value?content.value.toLowerCase():"";
 if(/^(video\/(mp4|webm|quicktime)|audio\/(mpeg|mp4|aac|ogg|wav)|application\/(vnd\.apple\.mpegurl|x-mpegurl))/.test(mime))
  notify({type:"media",url:details.url,referer:details.originUrl||details.documentUrl||"",tabId:details.tabId});
},{urls:["<all_urls>"]},["responseHeaders"]);
browser.runtime.onMessage.addListener((message,sender)=>{
 if(!message||typeof message!=="object")return;
 if(message.type==="media"&&typeof message.url==="string")
  notify({type:"media",url:message.url,referer:message.referer||sender.url||"",tabId:sender.tab&&sender.tab.id});
 if(message.type==="playerTap")notify({type:"playerTap",url:sender.url||""});
});
