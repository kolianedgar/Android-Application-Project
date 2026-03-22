# Career Link 📱

Career Link is an Android application that demonstrates a **secure, real-time chat system with authentication**, built using modern Android practices and Firebase services.

The project focuses on **security, real-time communication, offline handling, and user session management**, making it suitable as both an academic project and a scalable base for future development.

---

## ✨ Features

### 🔐 Authentication & User Management
- User registration with full name storage
- Secure login using Firebase Authentication
- Input validation for login credentials
- Session persistence and protected navigation
- Logout functionality with session clearing

### 💬 Real-Time Chat
- Global chat powered by Firebase Realtime Database
- Messages update instantly across all connected users
- Automatic “<FirstName>: connected!” message on login
- Logout and connection status messaging

### 🌐 Network Awareness
- Detects network connection and disconnection
- Displays:
  - "Network connected"
  - "Network disconnected"
- Sends messages when connection is lost/restored
- Offline message queue:
  - Messages are stored locally when offline
  - Automatically sent when connection returns

### 🔒 Security
- End-to-end message encryption using **AES (GCM)**
- Encrypted messages stored in Firebase
- Decryption handled client-side only

### 🧠 Smart UX Behavior
- Send button disabled until user data is loaded
- Prevents sending empty messages
- Auto-scroll chat to latest message
- Back button disabled after login (session protection)

---

## 🛠️ Tech Stack

- **Language:** Java  
- **Platform:** Android  
- **Authentication:** Firebase Authentication  
- **Database:** Firebase Realtime Database  
- **Encryption:** AES/GCM/NoPadding  
- **Networking:** ConnectivityManager / NetworkCallback  
- **Build System:** Gradle  
- **IDE:** Android Studio  

---

## ▶️ Running the Application

### Option 1: Android Studio (Recommended)

1. Clone the repository:
   ```sh
   git clone https://github.com/your-username/career-link.git
   ```
2. Open in Android Studio
3. Let Gradle sync
4. Run on:
  - Emulator, or
  - Physical Android device (USB debugging enabled)

### Option 2: Install APK
1. Build APK:
  - Build → Build APK(s)
2. Transfer to your phone
3. Install and launch

## 🔐 Authentication Flow
1. User registers with:
  - Email
  - Password
  - Full Name
2. A verification email is automatically sent
3. User must verify their email before logging in
4. User logs in with valid credentials
5. App verifies session
6. User is redirected to chat
7. On login:
  - First name is extracted
  - “<FirstName>: connected!” is sent to chat
8. On logout:
  - Session is cleared
  - User is redirected to login screen

## 💬 Chat Flow
1. User sends a message
2. Message is:
  - Encrypted using AES
  - Stored in Firebase
3. Other clients:
  - Receive message in real-time
  - Decrypt and display it
4. If offline:
  - Message is queued
  - Sent automatically when connection returns

## 🌐 Network Handling
- Uses ConnectivityManager.NetworkCallback
- Tracks:
  - Active connection
  - Connection loss
- Behavior:
  - Shows Toast messages
  - Queues messages when offline
  - Flushes queue when back online
 
## 🚀 Future Improvements
- Private messaging (1-to-1 chat)
- Chat rooms / groups
- Message timestamps UI improvements
- Typing indicators
- Push notifications (Firebase Cloud Messaging)
- User profile screen
- Material Design 3 UI upgrade
- Migration to MVVM architecture
