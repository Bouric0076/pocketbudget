# PocketBudget KE

## Project Overview
PocketBudget KE is an offline-first Android application designed to help users monitor and understand their spending habits by automatically extracting transaction data from M-Pesa SMS messages. The system parses transaction messages, stores structured transaction records locally, categorizes spending, and generates financial summaries and insights.

## Setup Instructions
1.  **Clone the repository.**
2.  **Open in Android Studio.**
3.  **Sync Gradle.**
4.  **Run on Emulator/Device.**

## Branch Naming Convention
-   `feature/feature-name` (e.g., `feature/sms-reader`)
-   `bugfix/bug-description`
-   `hotfix/critical-fix`


## Architecture
-   **UI**: Fragments + XML Layouts
-   **Navigation**: Android Navigation Component (Single Activity)
-   **Database**: Room Database
-   **Language**: Kotlin
