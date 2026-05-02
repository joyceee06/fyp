# 🌱 EkoPantri – Smart Sustainable Pantry & Food Waste Management System

EkoPantri is an Android application developed as a Final Year Project (FYP) to reduce household food waste. The system leverages Generative AI and Computer Vision to help users manage pantry inventory efficiently while providing intelligent food storage guidance.

---

## 📌 Overview

EkoPantri combines AI-driven automation with real-time inventory tracking to promote sustainable food consumption. It assists users in organizing groceries, monitoring expiry dates, and discovering recipes based on available ingredients.

---

## 🚀 Key Features

### 🧾 AI-Powered Receipt Scanner

* **Technique:** Multimodal Generative AI
* **Functionality:**

  * Capture grocery receipts using the camera
  * Extract item names and quantities automatically
  * Suggest categories and storage locations (Fridge, Freezer, Pantry)

---

### 🤖 Smart Education & AI Assistant

* **Cloud Repository:** Storage guidelines synced via Firebase Firestore
* **AI Assistant:**

  * Integrated with Google Gemini
  * Provides real-time, context-aware food storage tips
  * Accessible via floating "Ask AI" interface

---

### 📦 Inventory & Expiry Tracking

* Real-time synchronization using Firebase Firestore
* Automated alerts for "Expiring Soon" items
* Customizable notification thresholds
* Waste analytics to track consumption vs. waste trends

---

### 🍳 Recipe Discovery

* **API Integration:** Spoonacular API (via Retrofit 2)
* Suggests recipes based on available ingredients
* Helps reduce food waste by utilizing existing inventory

---

## 🛠️ Technologies Used

* **Android (Java/Kotlin)**
* **Firebase Firestore** (real-time database)
* **Generative AI (Google Gemini)**
* **Computer Vision**
* **Retrofit 2 (REST API integration)**
* **Spoonacular API**
