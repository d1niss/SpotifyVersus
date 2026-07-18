package spotifyvs;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class SpotifyService {

    private SpotifyApi spotifyApi;

    public SpotifyService() {
        Properties prop = new Properties();
        String clientId = null;
        String clientSecret = null;

        // Load the credentials safely from the local file
        try (FileInputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            clientId = prop.getProperty("spotify.client.id");
            clientSecret = prop.getProperty("spotify.client.secret");
        } catch (IOException ex) {
            System.err.println("Error: Could not find or read config.properties!");
            System.err.println("Make sure the file exists in the root directory.");
        }

        // Initialize the Spotify API configuration with the loaded tokens
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
            System.out.println("Successfully authenticated with Spotify! Token fetched dynamically.");
            return true;
        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
            return false;
        }
    }

    public SpotifyApi getSpotifyApi() {
        return this.spotifyApi;
    }
}