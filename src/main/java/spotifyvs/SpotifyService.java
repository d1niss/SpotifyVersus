package spotifyvs;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class SpotifyService {

    private SpotifyApi spotifyApi;
    private boolean isAuthenticated = false;

    public SpotifyService() {
        Properties prop = new Properties();
        String clientId = null;
        String clientSecret = null;

        try (FileInputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            clientId = prop.getProperty("spotify.client.id");
            clientSecret = prop.getProperty("spotify.client.secret");
        } catch (IOException ex) {
            System.err.println("Error: Could not find or read config.properties!");
        }

        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .build();
    }

    public boolean authenticate() {
        try {
            ClientCredentialsRequest request = spotifyApi.clientCredentials().build();
            ClientCredentials credentials = request.execute();

            spotifyApi.setAccessToken(credentials.getAccessToken());
            System.out.println("Successfully authenticated with Spotify!");
            this.isAuthenticated = true;
            return true;
        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
            this.isAuthenticated = false;
            return false;
        }
    }

    /**
     * Fetches the URL of the album artwork for a given track URI.
     */
    public String getAlbumArtUrl(String trackUri) {
        if (!isAuthenticated || trackUri == null || !trackUri.startsWith("spotify:track:")) {
            return null;
        }
        try {
            // Extract the track ID from the full URI string
            String trackId = trackUri.substring("spotify:track:".length());
            
            // Call Spotify API to retrieve track metadata
            Track track = spotifyApi.getTrack(trackId).build().execute();            
            if (track != null && track.getAlbum() != null && track.getAlbum().getImages().length > 0) {
                // index 0 = 640x640, index 1 = 300x300 (perfect for our UI)
                return track.getAlbum().getImages()[1].getUrl();
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch artwork for URI " + trackUri + ": " + e.getMessage());
        }
        return null;
    }

    public SpotifyApi getSpotifyApi() {
        return this.spotifyApi;
    }
}