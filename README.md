EkoPantri -Smart Sustainable Pantry & Food Waste Management System

EkoPantri is an Android application developed as a Final Year Project (FYP) to combat household food waste. It utilizes Generative AI and Computer Vision to help users manage inventory efficiently while providing expert-curated and AI-driven food preservation education.

Key Features
1. AI-Powered Receipt Scanner

   •Technique: Multimodal Generative AI
   
   •Function: Users take a photo of their grocery receipt; the AI semantically analyzes the image to extract item names, quantities, and automatically suggests categories and storage locations (Fridge, Freezer, or Pantry).

2. Smart Education & Assistant

   •Cloud Repository: Curated storage guides synced via Firebase Firestore.

   •AI Assistant: A floating "Ask AI" assistant powered by Google Gemini that provides real-time, context-aware tips based on the user's current pantry inventory.

3. Inventory & Expiry Tracking
  
   •Real-time Sync: Powered by Firebase Firestore for multi-device synchronization.

   •Smart Alerts: Automated tracking of "Expiring Soon" items with user-defined notification thresholds.

   •Waste Analytics: Visual insights into consumption vs. waste patterns over time.

4. Recipe Discovery (REST API)

   •Technique: Retrofit 2 integration with the Spoonacular API.

   •Function: Suggests recipes based on ingredients the user already has in their inventory to prevent food from spoiling.
