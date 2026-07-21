package spotifyvs;

import com.sun.net.httpserver.HttpServer;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.awt.Desktop;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class SpotifyService {

    private SpotifyApi spotifyApi;
    private boolean isAuthenticated = false;
    private static final int PORT = 8080;
    private static final URI REDIRECT_URI = SpotifyHttpManager.makeUri("http://127.0.0.1:" + PORT + "/callback");
    
    private final CountDownLatch authLatch = new CountDownLatch(1);
    private String authorizationCode = null;
    private String currentUserId = null; // Caches the logged-in user's profile ID

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
                .setRedirectUri(REDIRECT_URI)
                .build();
    }

    public boolean authenticate() {
        HttpServer server = null;
        try {
            String state = UUID.randomUUID().toString();

            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/callback", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.contains("code=")) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("code=")) {
                            authorizationCode = param.substring(5);
                        }
                    }
                    String response = "<h1>Authentication Successful!</h1><p>You can close this window now.</p>";
                    exchange.sendResponseHeaders(200, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
                authLatch.countDown(); 
            });
            server.start();

            AuthorizationCodeUriRequest uriRequest = spotifyApi.authorizationCodeUri()
                    .scope("playlist-read-private,playlist-read-collaborative")
                    .state(state)
                    .build();

            URI uri = uriRequest.execute();
            System.out.println("Opening browser for login: " + uri);
            
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else {
                System.out.println("Please open this link manually: " + uri);
            }

            authLatch.await();
            server.stop(1);

            if (authorizationCode == null) {
                throw new IllegalStateException("Authorization code was not received.");
            }

            AuthorizationCodeRequest authCodeRequest = spotifyApi.authorizationCode(authorizationCode)
                    .build();

            AuthorizationCodeCredentials credentials = authCodeRequest.execute();
            
            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());
            
            // Fetch and store the authenticated user's ID immediately
            try {
                User userProfile = spotifyApi.getCurrentUsersProfile().build().execute();
                this.currentUserId = userProfile.getId();
                System.out.println("Logged in as User ID: " + currentUserId);
            } catch (Exception ue) {
                System.err.println("Could not resolve profile details: " + ue.getMessage());
            }

            System.out.println("Successfully authenticated with Spotify User Auth!");
            this.isAuthenticated = true;
            return true;

        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
            if (server != null) server.stop(0);
            this.isAuthenticated = false;
            return false;
        }
    }

    public String getAlbumArtUrl(String trackUri) {
        if (!isAuthenticated || trackUri == null || !trackUri.startsWith("spotify:track:")) {
            return null;
        }
        try {
            String trackId = trackUri.substring("spotify:track:".length());
            Track track = spotifyApi.getTrack(trackId).build().execute();            
            if (track != null && track.getAlbum() != null && track.getAlbum().getImages().length > 0) {
                return track.getAlbum().getImages()[1].getUrl();
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch artwork for URI " + trackUri + ": " + e.getMessage());
        }
        return null;
    }

    public List<PlaylistSimplified> getCurrentUsersPlaylists() {
        if (!isAuthenticated) return null;
    try {
        Paging<PlaylistSimplified> playlistPaging = spotifyApi
                .getListOfCurrentUsersPlaylists()
                .limit(50)
                .build()
                .execute();
    
        List<PlaylistSimplified> filteredPlaylists = new ArrayList<>();
        if (playlistPaging.getItems() != null) {
            for (PlaylistSimplified playlist : playlistPaging.getItems()) {
                // Check if the current user is the owner
                boolean isOwner = currentUserId != null && playlist.getOwner() != null && currentUserId.equals(playlist.getOwner().getId());
                
                // FIX: Changed getCollaborative() to getIsCollaborative() to match the library's naming convention
                boolean isCollaborator = playlist.getIsCollaborative() != null && playlist.getIsCollaborative();
                
                if (isOwner || isCollaborator) {
                    filteredPlaylists.add(playlist);
                }
            }
        }
        return filteredPlaylists;
    } catch (Exception e) {
        System.err.println("Failed to fetch playlists: " + e.getMessage());
        return null;
    }
    }

    public SpotifyApi getSpotifyApi() {
        return this.spotifyApi;
    }
}