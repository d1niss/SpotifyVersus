# SpotifyVersus 🎵🏆

SpotifyVersus is a Java-based command-line tournament application designed to help you find your ultimate favorite tracks through a series of head-to-head song brackets. By integrating with the Spotify API using secure PKCE authentication, it allows you to pull tracks, manage them, and run tournaments to rank your library.

## ✨ Features

- **Spotify API Integration:** Connects securely to Spotify using PKCE (Proof Key for Code Exchange) OAuth flow.
- **Tournament Manager:** Simulates or interactive runs a bracket-style tournament to determine winning tracks.
- **Local Database support:** Seeds and retains track data using a lightweight CSV-based local repository (`songs.csv`).
- **Containerized Environment:** Fully dockerized for seamless setup and isolated execution.
- **Robust Build System:** Managed with Maven for dependency handling and automated testing.

## 📁 Project Structure

```text
SpotifyVersus/
├── src/
│   ├── main/java/spotifyvs/
│   │   ├── App.java                 # Application Entry Point
│   │   ├── PKCEUtil.java            # Spotify PKCE Authentication Helper
│   │   ├── Song.java                # Song Model
│   │   ├── SongRep.java             # CSV Data Repository Layer
│   │   ├── SpotifyImporter.java     # Logic to import tracks via Spotify API
│   │   ├── SpotifyService.java      # Spotify API wrapper and interaction
│   │   └── TournamentManager.java   # Bracket & tournament logic
│   └── test/java/spotifyvs/
│       └── AppTest.java             # Unit tests
├── songs.csv                        # Local storage/cache for song datasets
├── pom.xml                          # Maven configuration file
└── Dockerfile                       # Multi-stage Docker build recipe
```

## 🚀 Prerequisites

Before running the project, ensure you have the following installed:
- **Java Development Kit (JDK) 17** or higher
- **Apache Maven 3.8+**
- **Docker** (Optional, for containerized running)
- **Spotify Developer Credentials** (Client ID)

## 🛠️ Setup & Configuration

1. **Register your application on Spotify:**
   - Go to the [Spotify Developer Dashboard](https://developer.spotify.com/).
   - Create a new app.
   - Edit the settings and add your Redirect URI (e.g., `http://localhost:8080/callback` or as configured in `PKCEUtil.java`).

2. **Clone the repository:**
   ```bash
   git clone https://github.com/d1niss/spotifyversus.git
   cd spotifyversus
   ```

3. **Configure Environment Variables (If required by your setup):**
   Ensure your Spotify Client ID and Redirect URIs are passed to the environment or properly set within your configuration/argument parser.

## 📦 Building and Running

### Method 1: Local Maven Build

1. Build and package the application:
   ```bash
   mvn clean package
   ```

2. Run the generated JAR file:
   ```bash
   java -jar target/SpotifyVersus-1.0-SNAPSHOT.jar
   ```

### Method 2: Docker (Recommended)

1. Build the lightweight Docker image:
   ```bash
   docker build -t spotify-versus .
   ```

2. Run the interactive tournament container:
   ```bash
   docker run -it spotify-versus
   ```

## 🧪 Running Tests

To execute the unit tests included in the project:
```bash
mvn test
```

## 📝 License

This project is open-source and available under the MIT License.