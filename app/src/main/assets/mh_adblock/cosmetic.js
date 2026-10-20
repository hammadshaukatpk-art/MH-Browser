(function(){
 const css="ins.adsbygoogle,.adsbygoogle,[data-ad-client],[data-ad-slot],"+
 ".google-auto-placed,[id^='google_ads_iframe'],[id^='div-gpt-ad'],"+
 "iframe[src*='doubleclick.net'],iframe[src*='googlesyndication.com'],"+
 ".ad-banner,.advertisement,.ad-container{display:none!important}";
 function add(){if(document.getElementById("mh-gecko-ad-css"))return;
  const style=document.createElement("style");style.id="mh-gecko-ad-css";style.textContent=css;
  (document.head||document.documentElement).appendChild(style);
 }
 if(document.documentElement)add();else document.addEventListener("DOMContentLoaded",add,{once:true});
})();
