# MH Browser — Android V1.0 (development preview)

Android browser using Android System WebView (Chromium-based). Branded navy/cyan UI with horizontal tab strip, editable home shortcuts, tab switching, Google search, bookmarks and history, dark dialogs, settings and direct-file downloads.

## Implemented
- One home search field (the page address bar appears only on web pages).
- Horizontal tab strip with per-tab close, tab creation, switching and independent WebViews.
- Saved normal-tab URLs and current selection across app relaunches; private tabs are not restored.
- Editable/addable/removable Quick Access shortcuts stored on device.
- Dark themed menu, confirmations, edit dialogs, settings and exit prompt.
- General settings: JavaScript, image loading, zoom, desktop user agent for new tabs and restore tabs.
- Privacy: limited tracker domain blocking, third-party cookie toggle, Safe Browsing and mixed-content blocking; DNT header on direct app navigations.
- Bookmarks, recent URL history and clear browsing data.
- Direct HTTP/HTTPS file downloads using Android Download Manager.
- AndroidX WebKit HTTP proxy override (host and port, if supported by the installed Android System WebView), with apply/disable.
- Android system split-screen eligibility and experimental picture-in-picture.
- Optional background media continuation where websites and Android allow it.
- Permission-gated website geolocation.

## October 2026 UI and browsing updates
- Video-only Picture-in-Picture is available for fullscreen WebChromeClient video views, not for the entire browser. The user must first enter video fullscreen.
- The browser can avoid pausing its WebView when background playback is enabled. Sites such as YouTube may still pause playback under their own restrictions; this is not a guarantee of continuous YouTube background audio.
- History now stores page titles, hostnames, visit timestamps and groups by Today, Yesterday, Previous 7 Days, This Month, This Year, and Older. Selected or full deletion also clears the open WebViews' Back/Forward stacks.
- Bookmarks now have a management page with add, open and delete actions.
- The main menu uses branded grouped cards, centered logo and icons. Settings cards are organized and styled.
- Home advertising artwork and **For Advertising, Contact with Us** are placeholders until the owner provides a contact number.
- Home Google account icon opens an external system browser for secure Google authentication; this **does not** synchronize Google credentials or sessions into MH Browser. Official account integration needs Google-approved OAuth configuration and credentials.
- Desktop site toggle is now per-tab and requests a desktop user agent with wide viewport.

## Not complete / important limitations
- **Incognito is not securely isolated from normal WebView cookies in this preview.** Do not use it for sensitive private sessions.
- Tab URL restoration does not preserve every in-page form, scroll position, playback state, or complete navigation stack.
- HTTP proxy support is unauthenticated only; username/password and SOCKS5 proxies are **not supported**. No VPN or IP/GPS spoofing.
- Android split-screen availability depends on device OS; PiP and background audio depend on Android and individual websites.
- Downloads require a direct file URL; arbitrary streaming video/audio extraction, DRM bypass and resolution picking are not provided.
- DNT headers are advisory and are not guaranteed for every subresource or redirect.
- Tracker blocking is limited, not equivalent to Brave's full protections.
- Not yet tested on physical Android devices. APK compilation does not establish functional correctness.

## Build
JDK 17, Android SDK 35, Gradle 8.9. GitHub Actions builds a debug APK. This is a development preview, not a production privacy browser.
