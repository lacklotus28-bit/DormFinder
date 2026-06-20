# DormFinder

A native Android application that helps students find dormitories and boarding houses around Batangas and Lipa. Built as an undergraduate thesis project for the Bachelor of Science in Computer Science program at Rizal College of Taal.

## Features

- Browse and search dormitory and boarding house listings with photos and details
- Interactive map view using Google Maps API to locate listings near campus
- Real-time messaging between students and landlords, with unread message indicators
- Reviews and ratings system for listed properties
- Booking and payment request flow
- Push notifications for booking updates and new messages
- Offline access through local caching with Room database, syncing automatically once back online

## Tech Stack

- Android (Java)
- Firebase Firestore (database)
- Firebase Authentication
- Firebase Cloud Messaging (push notifications)
- Room Database (offline storage)
- Google Maps API
- Cloudinary (image hosting)

## Getting Started

This project requires a Firebase project of your own to run (Firestore, Auth, and Cloud Messaging enabled), since the original `google-services.json` and service account credentials are not included in this repo for security reasons.

1. Clone the repo and open it in Android Studio.
2. Create a Firebase project and add an Android app with package name `com.rct.dormfinder`.
3. Download your own `google-services.json` and place it in `app/`.
4. Build and run on an emulator or device (minSdk 26).

## Status

Completed as a thesis capstone project for the College of Computer Studies, Rizal College of Taal.
