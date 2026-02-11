package spotifyvs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import java.io.FileReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

public class SpotifyImporter {

    public static void main(String[] args) {
        String csvFile = "songs.csv"; // Make sure this file is in the project root
        String jdbcUrl = "jdbc:sqlite:spotify_tracks.db"; // This will create the DB file

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            if (conn != null) {
                System.out.println("Connected to database.");
                createTable(conn);
                importCsv(conn, csvFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
}
