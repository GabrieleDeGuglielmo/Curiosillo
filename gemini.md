# Curiosillo - AI Assistant Context & Instructions

Welcome! This document (`gemini.md`) provides critical context and instructions for AI coding assistants (like Gemini/Antigravity) working on the **Curiosillo** codebase. 

Please review this file to understand the app's architecture, tech stack, and coding guidelines.

## 📱 App Context

**Curiosillo** is an Android edutainment app designed to make learning a daily habit—fun, effortless, and bite-sized. It features daily curiosities, AI-powered deep dives, user duels, gamification (streaks, points), community comments, and content review modes.

## 🛠️ Tech Stack & Architecture

- **UI Framework:** Jetpack Compose + Material 3
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles
- **State Management:** Kotlin `StateFlow` / `Flow`
- **Local Database:** Room Database
- **Backend/Cloud:** Firebase (Auth, Firestore for NoSQL data, Storage for media)
- **AI Integration:** Google Gemini 2.5 Flash lite
- **Navigation:** Jetpack Navigation Compose
- **Image Loading:** Coil
- **Build System:** Kotlin DSL (`build.gradle.kts`) + KSP

## 📂 Project Structure

The project code is located under `app/src/main/java/com/example/curiosillo/` with a clear separation of concerns:

- `data/`: Data models, Room database configuration, DAOs, and DataStore.
- `domain/`: Business logic, use cases, and gamification engine logic.
- `firebase/`: FirebaseManager and cloud integration handling.
- `repository/`: Data source management (e.g., `CuriosityRepository`) acting as a single source of truth, mediating between Room and Firebase.
- `ui/`: Composable screens, UI components, and theme definitions.
- `viewmodel/`: UI state management, exposing `StateFlow`s to the UI.

## 🤖 Instructions for AI Assistants

When generating code, refactoring, or debugging, please adhere to the following guidelines:

### 1. Architecture & State Management
- **Follow MVVM:** UI components should not contain business logic. ViewModels should handle logic and expose state via `StateFlow`.
- **Unidirectional Data Flow:** UI events trigger ViewModel functions, which update the StateFlow, which in turn causes the UI to recompose.
- **Repositories:** ViewModels should interact with Repositories, not directly with Firebase or Room.

### 2. UI & Compose
- **Jetpack Compose:** Use modern Jetpack Compose for all UI development. Avoid legacy XML layouts.
- **Material 3:** Leverage Material 3 components and theming guidelines.
- **State Hoisting:** Keep composables as stateless as possible by hoisting state up to screen-level composables or ViewModels.

### 3. Firebase & Data Handling
- **Async Operations:** Handle Firebase and Room operations asynchronously using Kotlin Coroutines and `suspend` functions.
- **Error Handling:** Implement robust error handling for network requests and database operations. Ensure the UI gracefully reflects loading, success, and error states.
- **Synchronization Check:** Before implementing any logic that saves data to the local Room database, you MUST ask the user if this data needs to be synchronized with Firebase.

### 4. Code Quality & Formatting
- **Kotlin Idioms:** Use Kotlin standard library functions, extension functions, and null-safety features effectively.
- **Preserve Comments:** Do not remove existing comments or docstrings unless explicitly instructed or if they are directly rendered obsolete by a change.
- **Minimal Edits:** When fixing a bug, make targeted changes. Do not rewrite entire classes unless asked.

### 5. Persistent Context & KIs
- Always check the workspace Knowledge Items (KIs) and previous conversation logs if you encounter deceptively simple tasks or recurring bugs (e.g., Avatar Selection synchronization issues).

### 6. Feature Logging
- **Document Changes:** Every time you implement a new important feature or make a significant architectural change, you MUST record it in the **🚀 Main Features & Recent Changes** section at the bottom of this file. This ensures that the app's evolution is tracked and context is not lost in future sessions.

## 🚀 Main Features & Recent Changes

- **Email Verification Check:** Implemented a robust check during login to detect unverified emails and gracefully handle them, preventing app crashes. (May 2026)

---
*Note: This file should be kept up-to-date as the project evolves.*
