# MH Browser — Android v1.0

Android WebView browser foundation based on the approved MH Browser concept.

## Implemented foundation
- Navy branded start page, Google search, shortcuts and nav controls.
- WebView tabs, forward/back/home/reload, normal and private tabs.
- Persistent normal browser cookies and last-visited URL.
- Site Safe Browsing, HTTPS mixed-content blocking, a basic tracker blocklist and third-party cookie preference.
- Privacy and appearance/settings screens, clear browsing data, proxy configuration form, background-playback preference and exit dialog.
- Android Github Actions workflow for a debug APK.

## Important limitations
This is an **unverified v1.0 starter**, not a finished competitor to Chrome or Brave. It has NOT been built or device-tested. WebView cookie storage is shared across tabs; private-mode isolation and data erasure are not guaranteed in this implementation. Tab/form/history state across process death is not fully restored. Proxy settings are saved but **not connected to the network stack**, so the proxy does not run. Background media is website-dependent. Split-screen depends on the Android OS, not just this app. No VPN, IP address changer, fake GPS, or device-identity spoofing. HTTPS encryption does not imply anonymity.

Build with Android SDK 35, JDK 17, Gradle 8.9; see Actions tab for build status.
