const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const HOST = '0.0.0.0';

const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Shopzy Haiti</title>
  <style>
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      -webkit-tap-highlight-color: transparent;
    }
    body, html {
      width: 100%;
      height: 100%;
      overflow: hidden;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      background-color: #0f172a;
      display: flex;
      justify-content: center;
      align-items: center;
    }
    #app-container {
      width: 100%;
      max-width: 480px;
      height: 100%;
      background: #ffffff;
      position: relative;
      display: flex;
      flex-direction: column;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
    }
    /* Top Bar */
    header {
      background: #1d4ed8;
      color: #ffffff;
      height: 56px;
      min-height: 56px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 16px;
      z-index: 10;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .brand-title {
      font-size: 1.15rem;
      font-weight: 700;
      letter-spacing: 0.5px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .nav-actions {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .btn-icon {
      background: none;
      border: none;
      color: #ffffff;
      padding: 8px;
      border-radius: 50%;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background-color 0.2s;
    }
    .btn-icon:hover, .btn-icon:active {
      background: rgba(255, 255, 255, 0.15);
    }
    .btn-simulate {
      font-size: 0.72rem;
      padding: 4px 8px;
      background: rgba(255,255,255,0.2);
      border: 1px solid rgba(255,255,255,0.4);
      color: white;
      border-radius: 12px;
      cursor: pointer;
      font-weight: 600;
      transition: all 0.2s;
    }
    .btn-simulate:hover {
      background: rgba(255,255,255,0.3);
    }
    /* Loading bar */
    #progress-bar {
      height: 3px;
      width: 100%;
      background: #ea580c;
      position: absolute;
      top: 56px;
      left: 0;
      z-index: 20;
      transition: width 0.3s ease;
    }
    /* Main WebView */
    #webview-frame {
      flex: 1;
      width: 100%;
      height: calc(100% - 56px);
      border: none;
      background: #f8fafc;
    }
    /* Splash Screen */
    #splash-screen {
      position: absolute;
      inset: 0;
      background: linear-gradient(180deg, #1e3a8a 0%, #1d4ed8 50%, #0f172a 100%);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      z-index: 100;
      transition: opacity 0.5s ease, visibility 0.5s;
    }
    #splash-screen.hidden {
      opacity: 0;
      visibility: hidden;
      pointer-events: none;
    }
    .splash-card {
      width: 220px;
      height: 220px;
      border-radius: 32px;
      overflow: hidden;
      box-shadow: 0 20px 30px rgba(0,0,0,0.35);
      animation: pulse 2s infinite ease-in-out;
      background: rgba(255,255,255,0.1);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .splash-card img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    @keyframes pulse {
      0% { transform: scale(0.97); }
      50% { transform: scale(1.03); }
      100% { transform: scale(0.97); }
    }
    .splash-title {
      color: #ffffff;
      font-size: 1.75rem;
      font-weight: 800;
      margin-top: 24px;
      letter-spacing: 1px;
    }
    .splash-subtitle {
      color: rgba(255, 255, 255, 0.85);
      font-size: 0.9rem;
      margin-top: 8px;
    }
    .splash-loader {
      margin-top: 32px;
      width: 32px;
      height: 32px;
      border: 3px solid rgba(255, 255, 255, 0.2);
      border-top-color: #ea580c;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    /* Offline Screen */
    #offline-screen {
      position: absolute;
      inset: 56px 0 0 0;
      background: #f8fafc;
      display: none;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px;
      z-index: 50;
    }
    #offline-screen.visible {
      display: flex;
    }
    .offline-card {
      background: #ffffff;
      border-radius: 24px;
      padding: 36px 24px;
      width: 100%;
      max-width: 380px;
      text-align: center;
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08);
      border: 1px solid #e2e8f0;
    }
    .offline-icon-wrapper {
      width: 88px;
      height: 88px;
      border-radius: 50%;
      background: #fff7ed;
      color: #ea580c;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 24px auto;
    }
    .offline-icon-wrapper svg {
      width: 44px;
      height: 44px;
    }
    .offline-title {
      font-size: 1.45rem;
      font-weight: 700;
      color: #0f172a;
      margin-bottom: 10px;
    }
    .offline-desc {
      font-size: 0.92rem;
      color: #64748b;
      line-height: 1.5;
      margin-bottom: 28px;
    }
    .retry-btn {
      width: 100%;
      height: 50px;
      background: #1d4ed8;
      color: #ffffff;
      border: none;
      border-radius: 14px;
      font-size: 1rem;
      font-weight: 700;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      transition: background 0.2s;
    }
    .retry-btn:hover {
      background: #1e40af;
    }
    .retry-btn:active {
      transform: scale(0.99);
    }
  </style>
</head>
<body>
  <div id="app-container">
    <!-- Header Controls -->
    <header>
      <div class="brand-title">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"/><path d="M3 6h18"/><path d="M16 10a4 4 0 0 1-8 0"/>
        </svg>
        Shopzy Haiti
      </div>
      <div class="nav-actions">
        <button id="toggle-sim-btn" class="btn-simulate" title="Test Offline State Handler">Simulate Offline</button>
        <button id="btn-home" class="btn-icon" title="Home" data-testid="home_button">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
        </button>
        <button id="btn-refresh" class="btn-icon" title="Refresh" data-testid="refresh_button">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M8 16H3v5"/></svg>
        </button>
      </div>
    </header>

    <!-- Linear Progress Bar -->
    <div id="progress-bar" style="width: 0%;"></div>

    <!-- Live Seamless WebView -->
    <iframe id="webview-frame" src="https://shopzyhaiti.lovable.app" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>

    <!-- Offline State Handler -->
    <div id="offline-screen">
      <div class="offline-card">
        <div class="offline-icon-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="1" y1="1" x2="23" y2="23"></line>
            <path d="M16.72 11.06A10.94 10.94 0 0 1 19 12.55"></path>
            <path d="M5 12.55a10.94 10.94 0 0 1 5.17-2.39"></path>
            <path d="M10.71 5.05A16 16 0 0 1 22.58 9"></path>
            <path d="M1.42 9a15.91 15.91 0 0 1 4.7-2.88"></path>
            <path d="M8.53 16.11a6 6 0 0 1 6.95 0"></path>
            <line x1="12" y1="20" x2="12.01" y2="20"></line>
          </svg>
        </div>
        <h2 class="offline-title">No connection</h2>
        <p class="offline-desc">Please check your internet connection and try again.</p>
        <button id="retry-btn" class="retry-btn" data-testid="retry_button">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M8 16H3v5"/></svg>
          <span id="retry-text">Retry</span>
        </button>
      </div>
    </div>

    <!-- Splash Screen -->
    <div id="splash-screen">
      <div class="splash-card">
        <img src="/splash.jpg" alt="Shopzy Haiti Splash">
      </div>
      <div class="splash-title">Shopzy Haiti</div>
      <div class="splash-subtitle">Your Premier Online Marketplace</div>
      <div class="splash-loader"></div>
    </div>
  </div>

  <script>
    const STORE_URL = 'https://shopzyhaiti.lovable.app';
    const splashScreen = document.getElementById('splash-screen');
    const offlineScreen = document.getElementById('offline-screen');
    const webview = document.getElementById('webview-frame');
    const progressBar = document.getElementById('progress-bar');
    const retryBtn = document.getElementById('retry-btn');
    const retryText = document.getElementById('retry-text');
    const refreshBtn = document.getElementById('btn-refresh');
    const homeBtn = document.getElementById('btn-home');
    const toggleSimBtn = document.getElementById('toggle-sim-btn');

    let isSimulatedOffline = false;

    // Splash dismiss after 1.8s
    setTimeout(() => {
      splashScreen.classList.add('hidden');
      checkNetworkAndLoad();
    }, 1800);

    function setProgressBar(progress) {
      progressBar.style.width = progress + '%';
      if (progress >= 100) {
        setTimeout(() => { progressBar.style.width = '0%'; }, 400);
      }
    }

    function checkNetworkAndLoad() {
      if (isSimulatedOffline || !navigator.onLine) {
        showOfflineState();
      } else {
        showOnlineState();
      }
    }

    function showOfflineState() {
      offlineScreen.classList.add('visible');
      setProgressBar(0);
    }

    function showOnlineState() {
      offlineScreen.classList.remove('visible');
      setProgressBar(40);
      setTimeout(() => setProgressBar(85), 300);
      setTimeout(() => setProgressBar(100), 700);
    }

    // Refresh action
    refreshBtn.addEventListener('click', () => {
      setProgressBar(30);
      if (isSimulatedOffline || !navigator.onLine) {
        showOfflineState();
      } else {
        webview.src = STORE_URL;
        showOnlineState();
      }
    });

    // Home action
    homeBtn.addEventListener('click', () => {
      webview.src = STORE_URL;
      checkNetworkAndLoad();
    });

    // Retry action
    retryBtn.addEventListener('click', () => {
      retryText.textContent = 'Checking...';
      retryBtn.style.opacity = '0.7';

      setTimeout(() => {
        retryText.textContent = 'Retry';
        retryBtn.style.opacity = '1';
        if (isSimulatedOffline) {
          alert("Simulation mode is active. Click 'Simulate Offline' button in header to exit offline mode.");
        } else if (navigator.onLine) {
          showOnlineState();
          webview.src = STORE_URL;
        } else {
          showOfflineState();
        }
      }, 700);
    });

    // Toggle simulated offline for demonstration
    toggleSimBtn.addEventListener('click', () => {
      isSimulatedOffline = !isSimulatedOffline;
      toggleSimBtn.textContent = isSimulatedOffline ? 'Go Online' : 'Simulate Offline';
      toggleSimBtn.style.background = isSimulatedOffline ? '#ea580c' : 'rgba(255,255,255,0.2)';
      checkNetworkAndLoad();
    });

    // Network status events
    window.addEventListener('online', () => {
      if (!isSimulatedOffline) {
        showOnlineState();
        webview.src = STORE_URL;
      }
    });

    window.addEventListener('offline', () => {
      showOfflineState();
    });
  </script>
</body>
</html>`;

const server = http.createServer((req, res) => {
  if (req.url === '/splash.jpg') {
    const imgPath = path.join(__dirname, 'public', 'splash.jpg');
    if (fs.existsSync(imgPath)) {
      res.writeHead(200, { 'Content-Type': 'image/jpeg' });
      return fs.createReadStream(imgPath).pipe(res);
    }
  }

  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(htmlContent);
});

server.listen(PORT, HOST, () => {
  console.log(`Shopzy Haiti app running at http://${HOST}:${PORT}`);
});
