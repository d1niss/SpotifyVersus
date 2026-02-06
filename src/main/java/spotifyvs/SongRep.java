package spotifyvs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SongRep {

    private static final String JDBC_URL = "jdbc:sqlite:spotify_tracks.db";

    /**
     * Connects to the database and fetches 'count' number of random songs.
     */
    public List<Song> getRandomSongs(int count) {
        List<Song> songs = new ArrayList<>();
        
        // SQL query to get random rows efficiently
        String sql = "SELECT track_uri, track_name, artist_names, album_name FROM tracks ORDER BY RANDOM() LIMIT ?";

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, count);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Map the database columns to our Song object
                    Song song = new Song(
                        rs.getString("track_uri"),
                        rs.getString("track_name"),
                        rs.getString("artist_names"),
                        rs.getString("album_name")
                    );
                    songs.add(song);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching songs: " + e.getMessage());
            e.printStackTrace();
        }

        return songs;
    }
}