/* Prototype: domain filtering; production migration requires a full ABP/uBO-compatible engine. */
const domains = [
  "doubleclick.net","googlesyndication.com","googleadservices.com",
  "adnxs.com","adsrvr.org","criteo.com","criteo.net","taboola.com",
  "outbrain.com","pubmatic.com","rubiconproject.com","openx.net",
  "smartadserver.com","adform.net","casalemedia.com","media.net",
  "mgid.com","popads.net","popcash.net","propellerads.com",
  "exoclick.com","exosrv.com","adsterra.com","onclickads.net"
];
let enabled = true;
browser.storage.local.get({enabled:true}).then(state => enabled=state.enabled);
browser.storage.onChanged.addListener((changes,area)=>{
 if(area==="local"&&changes.enabled)enabled=changes.enabled.newValue;
});
browser.webRequest.onBeforeRequest.addListener(details=>{
 if(!enabled)return {};
 try{
  const host=new URL(details.url).hostname.toLowerCase();
  if(domains.some(d=>host===d||host.endsWith("."+d)))return {cancel:true};
 }catch(e){}
 return {};
},{urls:["<all_urls>"],types:["script","sub_frame","image","xmlhttprequest","media","object","other"]},["blocking"]);
