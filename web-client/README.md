# 🌐 YourOwnAI Web Client (View-Only)

Beautiful web interface for viewing and browsing your YourOwnAI conversations on desktop.

## ✨ Features

### ✅ Implemented
- 📱 **View Conversations** - Browse all your chats with real-time sync
- 🔍 **Search** - Quickly find conversations by title
- 🧠 **Memory Viewer** - Explore your AI's memories
- 💾 **Export** - Save conversations as Markdown or Text files
- 📋 **Copy Messages** - One-click copy to clipboard
- 📱 **QR Code** - Instant connect from your phone
- 🎨 **Modern UI** - Clean, responsive design
- ⚡ **Real-time Status** - Live connection indicator
- 👀 **View-Only Mode** - Safe browsing without accidentally sending messages

### 🎯 Future Ideas (Not Planned for View-Only)
- Dark mode toggle
- Advanced search (by content)
- Memory clustering visualization
- Statistics dashboard

## 🚀 Quick Start

### 1. Start the Android Server
1. Open YourOwnAI on your phone
2. Go to **Settings → Local Network Sync**
3. Tap **Start Server**
4. Tap **📱 Show QR Code**

### 2. Connect from Desktop
**Option A: Scan QR Code** (Easiest)
- Scan the QR code with your phone's camera or any QR scanner
- The link will open automatically in your desktop browser

**Option B: Manual Connection**
1. Make sure your desktop is on the same Wi-Fi network as your phone
2. Note the IP address from the Android app (e.g., `192.168.1.50:8765`)
3. Open your browser: `http://192.168.1.50:8765`

### 3. Browse Your Chats
- **Search** conversations using the search bar
- **Click** any conversation to view messages
- **Export** chats as `.md` or `.txt` files
- **View Memories** by clicking the 🧠 button
- **Copy** any message with the 📋 button

## 🔧 Development Setup

### Prerequisites
- Node.js 16+ and npm
- Android server running on your phone (same Wi-Fi network)

### Installation
```bash
cd web-client
npm install
```

### Run Development Server
```bash
npm start
```
Opens at `http://localhost:3000` and proxies API calls to `http://localhost:8765`

### Build for Production
```bash
npm run build
```
Creates optimized build in `build/` directory.

## 📁 Project Structure
```
web-client/
├── public/
│   └── index.html         # HTML template
├── src/
│   ├── api/
│   │   └── client.ts      # API client
│   ├── styles/
│   │   └── App.css        # Global styles
│   ├── types/
│   │   └── index.ts       # TypeScript interfaces
│   ├── App.tsx            # Main component
│   └── index.tsx          # Entry point
├── package.json
└── tsconfig.json
```

## 🌐 API Endpoints

### Status & Info
- `GET /status` - Server status and device info

### Conversations
- `GET /api/conversations` - List all conversations
- `GET /api/conversations/{id}/messages` - Get messages for a conversation

### Memories
- `GET /memories` - List all memories

## 🎨 Features Walkthrough

### 🔍 Search
Type in the search box to filter conversations by title. Clear search with the ✕ button.

### 🧠 Memory Viewer
Click **🧠 Memories** in the sidebar to view all saved facts the AI has learned about you.

### 💾 Export Options
- **📄 MD** - Markdown format with timestamps and formatting
- **📝 TXT** - Plain text format for simple reading

### 📋 Copy to Clipboard
Every message has a copy button (📋) that appears in the message footer.

### 👀 View-Only Notice
A yellow notice bar reminds you that this is a view-only interface. To send messages, use your phone.

## 🔍 Troubleshooting

### Connection Failed
- ✅ Both devices on **same Wi-Fi network**
- ✅ Server is **running** on phone (check Settings)
- ✅ Try **QR code** instead of manual IP
- ✅ Disable **VPN** on desktop if active
- ✅ Check **firewall** isn't blocking port 8765

### Can't Find Server
- Use the QR code feature for automatic connection
- Verify IP address in Android app settings
- Try `http://<phone-ip>:8765` directly in browser

### Conversations Not Loading
- Check server status indicator (green = connected)
- Click 🔄 refresh button in chat header
- Restart the server on your phone

### Export Not Working
- Make sure a conversation is selected
- Check browser's download settings
- Try a different browser if issues persist

## 📱 Using QR Code

The QR code feature makes connecting incredibly easy:

1. **On Phone**: Settings → Local Network Sync → Show QR Code
2. **On Desktop**: 
   - Scan with phone camera → link opens in desktop browser
   - OR use any QR scanner app
   - OR manually type the URL shown below QR code

The QR code contains the complete URL with your phone's IP and port.

## ⚙️ Configuration

### Change Server Port
Edit `package.json` proxy setting:
```json
"proxy": "http://localhost:8765"
```

### API Base URL (Production)
If deploying to a hosted server, update `src/api/client.ts`:
```typescript
const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8765';
```

## 🚀 Production Deployment

### Option 1: Serve from Android App (Recommended)
The web client can be built and served directly from the Android app:

1. Build the web client:
   ```bash
   npm run build
   ```

2. Copy `build/` contents to Android `assets/web/`

3. Update Ktor server to serve static files from assets

### Option 2: Separate Web Server
Deploy `build/` to any static hosting (Netlify, Vercel, etc.) and configure API URL.

## 🧪 Testing Checklist

- [ ] Server starts and shows correct IP/port
- [ ] QR code displays and opens correct URL
- [ ] Desktop browser connects successfully
- [ ] Conversations list loads
- [ ] Search filters conversations
- [ ] Messages display correctly
- [ ] Export MD works
- [ ] Export TXT works
- [ ] Copy to clipboard works
- [ ] Memory viewer displays all memories
- [ ] Refresh button reloads data
- [ ] View-only notice is visible
- [ ] Responsive on different screen sizes

## 🎨 Customization

### Colors
Edit CSS variables in `src/styles/App.css`:
```css
:root {
  --primary: #2563eb;     /* Main brand color */
  --surface: #f8fafc;     /* Card backgrounds */
  --text-primary: #0f172a; /* Main text */
}
```

### Export Format
Customize export templates in `App.tsx`:
- `exportToMarkdown()` - Markdown format
- `exportToTxt()` - Plain text format

## 📄 License

Part of the YourOwnAI project.

---

**Made with ❤️ for seamless Android ↔ Desktop sync**
