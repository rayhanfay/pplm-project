# Inventory Lending Management System

![](images/teknovaris.png)

An Android-based application for managing and tracking asset items at the Faculty of Engineering, University of Riau. Designed to address asset loss issues, inaccurate lending data, and weak identity verification present in the current manual system. This application integrates with the Faculty of Engineering student database to ensure that borrowing is only done by authorized academic community members.

### **Project Team:**

| Name                     | Student ID |
| ------------------------ | ---------- |
| Agus Syuhada             | 2207125092 |
| Ahmadi Ihsan Ananda      | 2207113380 |
| Danil Zalma Hendra Putra | 2207112600 |
| Edi Putra Yuni           | 2207111395 |
| Rally Mizanur            | 2207111392 |
| Rayhan Al Farassy        | 2207135776 |
| Zacky Julio Putra        | 2207135753 |

## 🚀 Features

- **Barcode-Based Inventory Lending**: Lending system with identity verification through barcode scanning for security and data accuracy
- **Integrated Login System & Real-Time Database**: Login system integrated with student and faculty databases with real-time data updates
- **Location Tracking & Timer Reminder**: Tracking of device's last location and automatic reminder system for return deadlines

## 🔍 Project Scope

### 📱 Mobile Application Development

- **Advanced Lending Management**: Comprehensive tracking of borrower details, lending dates, and item conditions
- **Multi-Level Authentication**: Integrated verification system ensuring only authorized faculty members can access
- **Real-Time Synchronization**: Live updates across all connected devices and admin panels
- **Intuitive User Interface**: Clean, modern design optimized for both student and admin workflows

### 💾 Database & Backend Infrastructure

- **Firebase Real-Time Database**: Scalable cloud database with instant synchronization
- **Secure Authentication System**: Multi-layer security with encrypted user data
- **Backup & Recovery**: Automated data backup and recovery systems
- **Analytics Dashboard**: Usage statistics and system performance monitoring

### ⚙️ Advanced Features

- **Smart Notifications**: Contextual reminders and status updates
- **GPS Integration**: Location-based services for item tracking
- **Offline Capability**: Limited offline functionality with sync when connected
- **Report Generation**: Automated reports for lending statistics and inventory status

## 🛠️ Tech Stack

### Frontend

- **Android Studio** - Primary IDE for Android development
- **Java/Kotlin** - Programming languages
- **XML** - UI layout design
- **Material Design** - UI/UX components

### Backend & Database

- **Firebase Realtime Database** - Real-time data synchronization
- **Firebase Authentication** - User authentication and management
- **Firebase Cloud Messaging** - Push notifications
- **Firebase Storage** - File and image storage

### Libraries & APIs

- **CameraX** - Camera functionality and image capture
- **MLKit Barcode Scanner** - QR code and barcode scanning
- **Google Play Services** - Location services and authentication
- **Retrofit** - HTTP client for API communication
- **Glide** - Image loading and caching

### Security & Performance

- **ProGuard** - Code obfuscation and optimization
- **SSL/TLS** - Secure data transmission
- **Firebase Security Rules** - Database access control

## 📲 Installation

### Prerequisites

- Android device with API level 21+ (Android 5.0 Lollipop)
- Internet connection for real-time features
- Camera permission for QR code scanning
- Location permission for tracking features

### Download Options

#### Option 1: APK Download

```bash
# Download the latest APK from releases
wget https://github.com/your-username/inventory-lending-app/releases/latest/download/app-release.apk

# Install using ADB (if connected to computer)
adb install app-release.apk
```

#### Option 2: Build from Source

```bash
# Clone the repository
git clone https://github.com/your-username/inventory-lending-app.git

# Open in Android Studio
cd inventory-lending-app
# Open project in Android Studio and build
```

### First-Time Setup

1. **Download and Install** the APK on your Android device
2. **Grant Permissions** when prompted (Camera, Location, Storage)
3. **Complete Onboarding** process to understand app features
4. **Login** with your faculty credentials
5. **Update Password** if logging in for the first time
6. **Verify Phone Number** for notification services

## 🎯 Usage

### For Students

1. **Login** with your student credentials
2. **Browse Available Items** in the student dashboard
3. **Scan QR Code** of desired item
4. **Fill Lending Form** with required details
5. **Set Lending Duration** according to your needs
6. **Submit Request** and wait for admin approval

### For Administrators

1. **Login** with admin credentials
2. **Monitor Lending Requests** in admin dashboard
3. **Approve/Reject** lending requests
4. **Manage Inventory** - add, edit, or remove items
5. **Process Returns** using QR scanner
6. **Generate Reports** for inventory tracking

### QR Code Operations

- **Lending**: Scan item QR code to initiate lending process
- **Returning**: Scan same QR code to process return
- **Inventory**: Each item has unique QR code for tracking
