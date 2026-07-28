package spotifyvs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.Reader;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import se.michaelthelin.spotify.SpotifyApi;


import com.google.gson.JsonParser;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SpotifyImporter {

public static void autoImport() {
        String csvFile = "songs.csv"; 
        String jdbcUrl = "jdbc:sqlite:spotify_tracks.db";

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            if (conn != null) {
                // --- ADDED THIS LINE TO CLEAR OLD TRACKS ---
                try (Statement clearStmt = conn.createStatement()) {
                    clearStmt.execute("DROP TABLE IF EXISTS tracks;");
                    System.out.println("Deleted existing 'tracks' table.");
                }

                System.out.println("Iniciating automatic import from CSV to SQLite database...");
                createTable(conn);
                importCsv(conn, csvFile);
            }
        } catch (Exception e) {
            System.err.println("Error in automatic import: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        autoImport();

        SongRep repo = new SongRep();
        List<Song> mySongs = repo.getRandomSongs(4);

        for (Song s : mySongs) {
            System.out.println("Fetched: " + s);
        }
    }

    private static void createTable(Connection conn) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS tracks (" +
                     "track_uri TEXT PRIMARY KEY, " +
                     "track_name TEXT, " +
                     "album_name TEXT, " +
                     "artist_names TEXT, " +
                     "release_date TEXT, " +
                     "duration_ms INTEGER, " +
                     "popularity INTEGER, " +
                     "explicit BOOLEAN, " +
                     "added_by TEXT, " +
                     "added_at TEXT, " +
                     "genres TEXT, " +
                     "record_label TEXT, " +
                     "danceability REAL, " +
                     "energy REAL, " +
                     "key INTEGER, " +
                     "loudness REAL, " +
                     "mode INTEGER, " +
                     "speechiness REAL, " +
                     "acousticness REAL, " +
                     "instrumentalness REAL, " +
                     "liveness REAL, " +
                     "valence REAL, " +
                     "tempo REAL, " +
                     "time_signature INTEGER)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'tracks' created or already exists.");
        }
    }

    private static void importCsv(Connection conn, String filePath) throws Exception {
        // SQL query with placeholders for all 24 columns
        String insertSql = "INSERT OR IGNORE INTO tracks VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Reader in = new FileReader(filePath);
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            // Configure CSV parser
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .builder()
                    .setHeader() // Automatically reads the first line as header
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build()
                    .parse(in);

            int count = 0;
            for (CSVRecord record : records) {

                pstmt.setString(1, record.get("Track URI"));
                pstmt.setString(2, record.get("Track Name"));
                pstmt.setString(3, record.get("Album Name"));
                pstmt.setString(4, record.get("Artist Name(s)"));
                pstmt.setString(5, record.get("Release Date"));
                pstmt.setInt(6, Integer.parseInt(record.get("Duration (ms)")));
                pstmt.setInt(7, Integer.parseInt(record.get("Popularity")));
                pstmt.setBoolean(8, Boolean.parseBoolean(record.get("Explicit"))); 
                pstmt.setString(9, record.get("Added By"));
                pstmt.setString(10, record.get("Added At"));
                pstmt.setString(11, record.get("Genres"));
                pstmt.setString(12, record.get("Record Label"));
                pstmt.setDouble(13, Double.parseDouble(record.get("Danceability")));
                pstmt.setDouble(14, Double.parseDouble(record.get("Energy")));
                pstmt.setInt(15, Integer.parseInt(record.get("Key")));
                pstmt.setDouble(16, Double.parseDouble(record.get("Loudness")));
                pstmt.setInt(17, Integer.parseInt(record.get("Mode")));
                pstmt.setDouble(18, Double.parseDouble(record.get("Speechiness")));
                pstmt.setDouble(19, Double.parseDouble(record.get("Acousticness")));
                pstmt.setDouble(20, Double.parseDouble(record.get("Instrumentalness")));
                pstmt.setDouble(21, Double.parseDouble(record.get("Liveness")));
                pstmt.setDouble(22, Double.parseDouble(record.get("Valence")));
                pstmt.setDouble(23, Double.parseDouble(record.get("Tempo")));
                pstmt.setInt(24, Integer.parseInt(record.get("Time Signature")));

                pstmt.addBatch();
                count++;
            }

            pstmt.executeBatch();
            System.out.println("Successfully imported " + count + " tracks!");
        }
    }

    public static boolean importPlaylistFromSpotify(String playlistId, SpotifyApi spotifyApi) {
    String jdbcUrl = "jdbc:sqlite:spotify_tracks.db";

    try {
        String accessToken = spotifyApi.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            System.err.println("[DEBUG] No valid access token found.");
            return false;
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement clearStmt = conn.createStatement()) {
            clearStmt.execute("DROP TABLE IF EXISTS tracks;");
            System.out.println("Cleared database cache for live playlist download.");
            createTable(conn);
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spotify.com/v1/playlists/" + playlistId + "/items?limit=50"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("[DEBUG] Failed to fetch tracks. HTTP Status Code: " + response.statusCode());
            System.err.println("[DEBUG] Response body: " + response.body());
            return false;
        }

        JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray items = jsonObject.getAsJsonArray("items");


        if (items == null) {
            System.out.println("[DEBUG] The 'items' array is completely NULL.");
            return false;
        }
        System.out.println("[DEBUG] Total items received in JSON array: " + items.size());

        if (items.size() == 0) {
            System.out.println("[DEBUG] The playlist payload returned 0 items. Is the playlist empty?");
            return false;
        }


        System.out.println("[DEBUG] Structure of the first item: " + items.get(0).toString());

        String insertSql = "INSERT OR IGNORE INTO tracks (track_uri, track_name, album_name, artist_names) VALUES (?,?,?,?)";
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            
            int count = 0;
            for (JsonElement itemElement : items) {
                JsonObject itemObj = itemElement.getAsJsonObject();
    
                
                if (!itemObj.has("item") || itemObj.get("item").isJsonNull()) {
                    continue;
                }
    
                JsonObject trackObj = itemObj.getAsJsonObject("item");
    
                String trackUri = trackObj.has("uri") ? trackObj.get("uri").getAsString() : null;
                String trackName = trackObj.has("name") ? trackObj.get("name").getAsString() : "Unknown Track";
    
                String albumName = "Unknown Album";
                if (trackObj.has("album") && !trackObj.get("album").isJsonNull()) {
                    JsonObject albumObj = trackObj.getAsJsonObject("album");
                    if (albumObj.has("name")) {
                        albumName = albumObj.get("name").getAsString();
                    }
                }

                StringBuilder artistBuilder = new StringBuilder();
                if (trackObj.has("artists") && !trackObj.get("artists").isJsonNull()) {
                    JsonArray artistsArray = trackObj.getAsJsonArray("artists");
                    for (int i = 0; i < artistsArray.size(); i++) {
                        JsonObject artistObj = artistsArray.get(i).getAsJsonObject();
                        if (artistObj.has("name")) {
                            artistBuilder.append(artistObj.get("name").getAsString());
                if (i < artistsArray.size() - 1) {
                    artistBuilder.append(", ");
                }
            }
        }
    }

    if (trackUri != null) {
        pstmt.setString(1, trackUri);
        pstmt.setString(2, trackName);
        pstmt.setString(3, albumName);
        pstmt.setString(4, artistBuilder.toString());
        pstmt.addBatch();
        count++;
    }
}
            
            pstmt.executeBatch();
            System.out.println("Successfully imported " + count + " tracks live from Spotify API using direct HTTP /items endpoint!");
            

            if (count == 0) {
                System.out.println("[DEBUG] Loop finished with 0 matches. Raw response body: " + response.body());
            }
            
            return count > 0; 
        }

    } catch (Exception e) {
        System.err.println("Failed to fetch live playlist tracks: " + e.getMessage());
        e.printStackTrace();
    }
    return false;
}

    
}
