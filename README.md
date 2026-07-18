# SpotifyVersus

SpotifyVersus is a JavaFX-based desktop application that turns your music library into a tournament-style bracket game. Users vote between head-to-head match-ups of random songs from their database until a single champion is crowned. It features seamless integration with the Spotify desktop application or web player so you can preview songs dynamically during matchups.

---

## Features

* **Automated Data Lifecycle:** On your very first run, the app autonomously detects an empty database, initializes the SQLite layout, and populates it using a native `songs.csv` file without requiring manual terminal inputs.
* **Dynamic Bracket Optimization:** Automatically scales the tournament to the closest power of two, injecting "Bye" rounds smoothly to support any number of songs.
* **Interactive JavaFX UI:** A clean, dark-themed user interface inspired by Spotify’s visual aesthetic.
* **In-App Media Redirection:** Includes action buttons that directly open the track inside your local Spotify desktop app or fallback to the Spotify Web Player via your default browser.

---

## Tech Stack

* **Language:** Java 11+
* **GUI Framework:** JavaFX
* **Database:** SQLite (via JDBC)
* **CSV Parsing:** Apache Commons CSV
* **Build Tool:** Maven

---

## Project Structure

```text
SpotifyVersus/
├── src/
│   └── main/
│       └── java/
│           └── spotifyvs/
│               ├── App.java              # Main JavaFX application & tournament runner
│               ├── Song.java             # Data model representation for track items
│               ├── SongRep.java          # Repository layer managing database queries
│               └── SpotifyImporter.java  # CSV Ingestion & schema building logic
├── songs.csv                             # Source CSV file containing track data
└── pom.xml                               # Project dependencies configuration
```

---

## How It Works

### 1. Database Initialization
When launching `App.java`, the system queries the SQLite database (`spotify_tracks.db`). If the database is completely empty or missing, it triggers the automated CSV parser, importing all track data from `songs.csv` directly into the database.

### 2. Tournament Structuring
The app grabs a randomized set of competitors from your library. If the total song count does not perfectly match a standard double-elimination power grid (e.g., 8, 16, 32, 64), the application injects placeholder **"Bye Songs"** to safely advance individual candidates evenly across matchups.

### 3. Matchups & Previews
Songs are pitted against each other two by two. Users can click **"▶ Ouvir no Spotify"** to listen to the song before submitting their vote. Clicking either track name progresses the bracket to the next matchup.

---

## Setup & Installation

### Prerequisites
* **Java Development Kit (JDK 11 or higher)**
* **Apache Maven** installed and configured

### Running the App

1. Ensure your track metadata spreadsheet is saved exactly as `songs.csv` in the root folder of the project. The CSV should utilize standard Spotify export headers (`Track URI`, `Track Name`, `Artist Name(s)`, `Album Name`, etc.).
2. Compile and run the application via Maven:
   ```bash
   mvn clean javafx:run
   ```

---

## Future Roadmap
* [ ] Integration with the official **Spotify Web API** to allow importing personal user playlists dynamically rather than using local CSV data.
* [ ] Live playback progress indicator inside the UI.
* [ ] Historical match leaderboards and track win/loss statistics.
