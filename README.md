# 🏒 Hockey Prediction App, CPEN 321 Project, UBC

A social mobile application for NHL fans to create and compete in hockey bingo challenges with friends. Built with Android (Kotlin) and Node.js/Express backend.

---

## 📱 Overview

The Hockey Prediction App combines the excitement of NHL hockey with social gaming. Users create custom bingo tickets with hockey-related events (goals, saves, penalties, etc.), challenge their friends, and compete in real-time as games unfold. The app integrates with the NHL API to track live game data and automatically update challenge statuses.

### Key Features

- **🎯 Custom Bingo Tickets**: Create personalized bingo cards with 9 hockey events
- **👥 Social Challenges**: Invite friends to compete using their own tickets
- **🔴 Live Game Tracking**: Real-time NHL game status updates via WebSocket
- **🏆 Friend System**: Send/accept friend requests and manage your network
- **📊 Challenge Management**: Track pending, active, live, and finished challenges

---

## 🏗️ Architecture

### Frontend (Android)

- **Language**: Kotlin 2.0.0
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **DI**: Hilt (Dagger)
- **Networking**: Retrofit + OkHttp
- **Real-time**: Socket.IO client
- **Min SDK**: API 33 (Android 13)

### Backend (Node.js)

- **Runtime**: Node.js 18+
- **Framework**: Express.js + TypeScript
- **Database**: MongoDB (Mongoose ODM)
- **Authentication**: JWT + Google OAuth
- **Real-time**: Socket.IO
- **External API**: NHL Stats API
- **Testing**: Jest + Supertest
- **Deployment**: Docker + Docker Compose

### Infrastructure

- **CI/CD**: GitHub Actions
- **Hosting**: AWS EC2
- **Containerization**: Docker
- **Database**: MongoDB (containerized)

---

## 🚀 Quick Start

### Prerequisites

**Backend:**

- Node.js 18+
- MongoDB instance
- Google OAuth credentials

**Frontend:**

- Android Studio (latest)
- Java 11+
- Android SDK 33+

### Backend Setup

1. **Install dependencies**:

   ```bash
   cd backend
   npm install
   ```

2. **Configure environment**:
   Create `backend/.env`:

   ```env
   PORT=3000
   JWT_SECRET=your_secret_key
   GOOGLE_CLIENT_ID=your_google_client_id
   MONGODB_URI=mongodb://localhost:27017/hockey-app
   ```

3. **Run development server**:

   ```bash
   npm run dev
   ```

4. **Run tests**:
   ```bash
   npm test
   npm test -- --coverage  # With coverage report
   ```

### Frontend Setup

1. **Open in Android Studio**:

   ```bash
   cd frontend
   # Open in Android Studio
   ```

2. **Configure local properties**:
   Create `frontend/local.properties`:

   ```properties
   sdk.dir=/path/to/Android/Sdk
   API_BASE_URL="http://10.0.2.2:3000/api/"
   IMAGE_BASE_URL="http://10.0.2.2:3000/"
   GOOGLE_CLIENT_ID="your_google_client_id.apps.googleusercontent.com"
   ```

3. **Sync Gradle** and **Run** the app on emulator or device

---

## 📡 API Endpoints

### Authentication

- `POST /api/auth/google` - Google OAuth login
- `POST /api/auth/verify-token` - Verify JWT token

### Users

- `GET /api/user/profile` - Get current user profile
- `GET /api/user/:id` - Get user by ID
- `PUT /api/user/profile` - Update profile
- `DELETE /api/user/profile` - Delete account

### Friends

- `POST /api/friends/send` - Send friend request
- `POST /api/friends/accept` - Accept friend request
- `POST /api/friends/reject` - Reject friend request
- `GET /api/friends/list` - Get friends list
- `GET /api/friends/pending` - Get pending requests
- `DELETE /api/friends/:friendId` - Remove friend

### Bingo Tickets

- `POST /api/tickets` - Create bingo ticket
- `GET /api/tickets/user/:userId` - Get user's tickets
- `GET /api/tickets/:id` - Get ticket by ID
- `PUT /api/tickets/crossedOff/:id` - Update crossed-off events
- `DELETE /api/tickets/:id` - Delete ticket

### Challenges

- `GET /api/challenges` - Get all user challenges (grouped by status)
- `GET /api/challenges/:id` - Get challenge details
- `POST /api/challenges` - Create new challenge
- `PUT /api/challenges/:id` - Update challenge
- `DELETE /api/challenges/:id` - Delete challenge
- `POST /api/challenges/:id/join` - Join challenge
- `POST /api/challenges/:id/leave` - Leave challenge

### Media

- `POST /api/media/upload` - Upload image (multipart/form-data)

---

## 🔄 Real-Time Features

The app uses WebSocket (Socket.IO) for real-time updates:

- **Challenge Updates**: Live challenge status changes
- **Game Status Sync**: Background job polls NHL API every 60 seconds

### Socket Events

- `challenge:updated` - Challenge status changed
- `challenge:joined` - New participant joined
- `challenge:left` - Participant left
- `game:statusChanged` - NHL game status updated

---

## 🧪 Testing

### Backend Tests

```bash
cd backend
npm test                    # Run all tests
npm test -- --coverage      # Generate coverage report
npm test tests/unmocked     # Integration tests only
npm test tests/mocked       # Unit tests only
```

**Test Strategy:**

- **Integration Tests**: Real database, full request/response cycle
- **Unit Tests**: Mocked dependencies, focused on error handling
- **CI/CD**: Automated testing on every PR via GitHub Actions

### Test Coverage

- Controllers: Full integration + error scenarios
- Services: Business logic + external API integration
- Middleware: Authentication, validation, error handling

### Frontend Tests

1. Sign in to the app using both test gmail accounts found in frontend/app/src/androidTest/java/com/cpen321/usermanagement/E2ETests/E2Etests.kt 
2. Ensure both accounts have no pre-existing friends or bingo tickets. If there are, delete them.
3. Press the button in E2Etests.kt to run 'E2Etests'

---

## 🐳 Deployment

### Docker Deployment

```bash
cd backend
docker compose up -d
```

The `docker-compose.yml` orchestrates:

- Backend Node.js service
- MongoDB database
- Environment variables from `.env`

### Production Deployment (EC2)

The app auto-deploys to AWS EC2 on push to `main`:

1. GitHub Actions workflow triggers
2. Files copied to EC2 via SCP
3. Docker containers rebuilt with `--no-cache`
4. Services restart automatically

**Deployment Steps:**

- Stop existing containers
- Remove old backend files
- Copy fresh code from GitHub
- Create `.env` with production secrets
- Build and start Docker containers

---

## 📂 Project Structure

```
M2-hockey-prediction-app/
├── .github/workflows/      # CI/CD pipelines
├── backend/
│   ├── src/
│   │   ├── controllers/    # Request handlers
│   │   ├── models/         # MongoDB schemas
│   │   ├── routes/         # API route definitions
│   │   ├── services/       # Business logic
│   │   ├── jobs/           # Background tasks (NHL sync)
│   │   └── types/          # TypeScript interfaces
│   ├── tests/
│   │   ├── mocked/         # Unit tests
│   │   └── unmocked/       # Integration tests
│   ├── docker-compose.yml
│   └── dockerfile
├── frontend/
│   └── app/
│       └── src/main/java/com/cpen321/usermanagement/
│           ├── data/       # Repositories & API clients
│           ├── ui/         # Composables & ViewModels
│           └── local/      # Local storage & Socket manager
└── documentation/          # Requirements & design docs
```

---

## 🔧 Configuration

### Environment Variables

**Backend** (`.env`):

- `PORT` - Server port (default: 3000)
- `JWT_SECRET` - Secret for JWT signing
- `GOOGLE_CLIENT_ID` - Google OAuth client ID
- `MONGODB_URI` - MongoDB connection string

**Frontend** (`local.properties`):

- `API_BASE_URL` - Backend API URL
- `IMAGE_BASE_URL` - Media server URL
- `GOOGLE_CLIENT_ID` - Google OAuth client ID (Android)

### NHL API Integration

The backend integrates with the official NHL Stats API:

- **Schedule Endpoint**: `https://api-web.nhle.com/v1/schedule/now`
- **Game Details**: Fetched on-demand for specific game IDs
- **Caching**: 30-second TTL to reduce API calls
- **Sync Job**: Runs every 60 seconds to update active challenges

---
