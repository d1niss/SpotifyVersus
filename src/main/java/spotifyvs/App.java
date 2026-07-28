package spotifyvs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

public class App extends Application {

    private List<Song> currentRound;
    private List<Song> nextRoundWinners;
    private int currentIndex;
    private int roundNumber;

    private Label lblStatus;
    private Button btnMusicA;
    private Button btnMusicB;
    private VBox mainLayout;

    private SpotifyService spotifyService;
    private WebView webViewA;
    private WebView webViewB;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SpotifyVersus");

        VBox loadingLayout = new VBox(20);
        loadingLayout.setAlignment(Pos.CENTER);
        loadingLayout.setStyle("-fx-background-color: #121212; -fx-padding: 40px;");

        Label lblWaiting = new Label("Waiting for Spotify Authentication...");
        lblWaiting.setStyle("-fx-font-size: 18px; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setStyle("-fx-progress-color: #1DB954;");

        loadingLayout.getChildren().addAll(lblWaiting, progressIndicator);
        primaryStage.setScene(new Scene(loadingLayout, 720, 600));
        primaryStage.show();

        this.spotifyService = new SpotifyService();
        
        new Thread(() -> {
            boolean success = this.spotifyService.authenticate();
            if (success) {
                List<PlaylistSimplified> playlists = this.spotifyService.getCurrentUsersPlaylists();
                Platform.runLater(() -> showPlaylistSelectionUI(primaryStage, playlists));
            } else {
                Platform.runLater(() -> showErrorMessage(primaryStage, "Authentication failed."));
            }
        }).start();
    }

    private void showPlaylistSelectionUI(Stage primaryStage, List<PlaylistSimplified> playlists) {
        VBox selectionLayout = new VBox(20);
        selectionLayout.setAlignment(Pos.CENTER);
        selectionLayout.setStyle("-fx-background-color: #121212; -fx-padding: 30px;");

        Label lblTitle = new Label("Select a Playlist to Start the Versus Bracket:");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        ListView<PlaylistSimplified> listView = new ListView<>();
        listView.setMaxWidth(500);
        listView.setPrefHeight(350);
        listView.setStyle("-fx-background-color: #181818; -fx-control-inner-background: #181818;");

        if (playlists != null) {
            listView.getItems().addAll(playlists);
        }

        listView.setCellFactory(param -> new ListCell<PlaylistSimplified>() {
            @Override
            protected void updateItem(PlaylistSimplified item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #181818;");
                } else {
                    setText("🎵  " + item.getName());
                    setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px;");
                }
            }
        });

        Button btnSelect = new Button("Load Tournament");
        btnSelect.setStyle("-fx-background-color: #1DB954; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 30px; -fx-background-radius: 20px; -fx-cursor: hand;");
        btnSelect.setDisable(true);

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            btnSelect.setDisable(newVal == null);
        });

        btnSelect.setOnAction(e -> {
            PlaylistSimplified selectedPlaylist = listView.getSelectionModel().getSelectedItem();
            if (selectedPlaylist != null) {
                btnSelect.setDisable(true);
                lblTitle.setText("Downloading tracks live from Spotify...");
                
                new Thread(() -> {
                    boolean success = SpotifyImporter.importPlaylistFromSpotify(selectedPlaylist.getId(), spotifyService.getSpotifyApi());
                    Platform.runLater(() -> {
                        if (success) {
                            setupTournamentUI(primaryStage);
                        } else {
                            lblTitle.setText("Select a Playlist to Start the Versus Bracket:");
                            btnSelect.setDisable(false);
                            showErrorMessage(primaryStage, "Access Denied. You can only load playlists you own or collaborate on.");
                        }
                    });
                }).start();
            }
        });

        selectionLayout.getChildren().addAll(lblTitle, listView, btnSelect);
        primaryStage.setScene(new Scene(selectionLayout, 720, 600));
    }

    private void setupTournamentUI(Stage primaryStage) {
        SongRep repo = new SongRep();
        int realSongCount = repo.getSongCount();

        if (realSongCount < 2) {
            showErrorMessage(primaryStage, "Selected playlist does not contain enough valid tracks to form a tournament bracket.");
            return;
        }

        int highestOneBit = Integer.highestOneBit(realSongCount);
        int bracketSize = (realSongCount == highestOneBit) ? realSongCount : highestOneBit << 1;

        List<Song> competitors = repo.getRandomSongs(realSongCount);
        while (competitors.size() < bracketSize) {
            competitors.add(Song.createBye());
        }
        Collections.shuffle(competitors);

        this.currentRound = competitors;
        this.nextRoundWinners = new ArrayList<>();
        this.currentIndex = 0;
        this.roundNumber = 1;

        lblStatus = new Label();
        lblStatus.setStyle("-fx-font-size: 16px; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        webViewA = new WebView();
        webViewA.setMaxSize(300, 380);
        webViewA.setMinSize(300, 380);

        webViewB = new WebView();
        webViewB.setMaxSize(300, 380);
        webViewB.setMinSize(300, 380);

        btnMusicA = new Button(); btnMusicB = new Button();
        String voteButtonStyle = "-fx-background-color: #1DB954; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 25px 40px; -fx-background-radius: 15px; -fx-cursor: hand; -fx-min-width: 300px; -fx-text-alignment: center;";
        btnMusicA.setStyle(voteButtonStyle); btnMusicB.setStyle(voteButtonStyle);

        btnMusicA.setOnAction(e -> vote(1)); 
        btnMusicB.setOnAction(e -> vote(2)); 

        VBox containerA = new VBox(15, webViewA, btnMusicA); containerA.setAlignment(Pos.CENTER);
        VBox containerB = new VBox(15, webViewB, btnMusicB); containerB.setAlignment(Pos.CENTER);
        HBox layoutBtn = new HBox(40, containerA, containerB); layoutBtn.setAlignment(Pos.CENTER);

        mainLayout = new VBox(30, lblStatus, layoutBtn);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-background-color: #121212; -fx-padding: 40px;"); 

        advanceConfront();
        primaryStage.setScene(new Scene(mainLayout, 820, 680));
    }

    private void advanceConfront() {
        if (currentIndex >= currentRound.size()) {
            currentRound = nextRoundWinners;
            nextRoundWinners = new ArrayList<>();
            currentIndex = 0;
            roundNumber++;
        }

        if (currentRound.size() == 1) {
            showFinalWinner(currentRound.get(0));
            return;
        }

        Song s1 = currentRound.get(currentIndex);
        Song s2 = currentRound.get(currentIndex + 1);

        if (s1.isBye()) { nextRoundWinners.add(s2); currentIndex += 2; advanceConfront(); return; }
        if (s2.isBye()) { nextRoundWinners.add(s1); currentIndex += 2; advanceConfront(); return; }

        lblStatus.setText("--- ROUND " + roundNumber + " (" + currentRound.size() + " songs remaining) ---");
        btnMusicA.setText(s1.getTrackName() + "\n🎤 " + s1.getArtistNames());
        btnMusicB.setText(s2.getTrackName() + "\n🎤 " + s2.getArtistNames());

        if (s1.getTrackUri() != null && s1.getTrackUri().startsWith("spotify:track:")) {
            String trackIdA = s1.getTrackUri().substring("spotify:track:".length());
            String embedUrlA = "https://open.spotify.com/embed/track/" + trackIdA;
            webViewA.getEngine().load(embedUrlA);
        }

        if (s2.getTrackUri() != null && s2.getTrackUri().startsWith("spotify:track:")) {
            String trackIdB = s2.getTrackUri().substring("spotify:track:".length());
            String embedUrlB = "https://open.spotify.com/embed/track/" + trackIdB;
            webViewB.getEngine().load(embedUrlB);
        }
    }

    private void vote(int choice) {
        nextRoundWinners.add(choice == 1 ? currentRound.get(currentIndex) : currentRound.get(currentIndex + 1));
        currentIndex += 2;
        advanceConfront(); 
    }

    private void openInSpotify(String trackUri) {
        if (trackUri == null || trackUri.isEmpty()) return;
        try {
            getHostServices().showDocument(trackUri);
        } catch (Exception e) {
            if (trackUri.startsWith("spotify:track:")) {
                getHostServices().showDocument("https://open.spotify.com/track/" + trackUri.substring(14));
            }
        }
    }

    private void showFinalWinner(Song winner) {
        lblStatus.setText("🏆 AND THE WINNER IS: 🏆");
        lblStatus.setStyle("-fx-font-size: 20px; -fx-text-fill: #FFD700; -fx-font-weight: bold;"); 

        Label lblWinner = new Label(winner.getTrackName().toUpperCase() + "\nby " + winner.getArtistNames());
        lblWinner.setStyle("-fx-font-size: 24px; -fx-text-fill: #1DB954; -fx-font-weight: bold; -fx-text-alignment: center;");

        ImageView imgWinner = new ImageView(); imgWinner.setFitWidth(250); imgWinner.setFitHeight(250); imgWinner.setPreserveRatio(true);
        String finalArt = spotifyService.getAlbumArtUrl(winner.getTrackUri());
        if (finalArt != null) imgWinner.setImage(new Image(finalArt, true));

        Button btnPlayWinner = new Button("▶ Open Last Winner in Spotify");
        btnPlayWinner.setStyle("-fx-background-color: #1DB954; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 25px; -fx-background-radius: 20px; -fx-cursor: hand;");
        btnPlayWinner.setOnAction(e -> openInSpotify(winner.getTrackUri()));

        mainLayout.getChildren().clear();
        mainLayout.getChildren().addAll(lblStatus, imgWinner, lblWinner, btnPlayWinner);
    }

    private void showErrorMessage(Stage stage, String message) {
        Label lblErro = new Label(message);
        lblErro.setStyle("-fx-text-fill: #FF5555; -fx-font-size: 16px; -fx-font-weight: bold;");
        VBox layout = new VBox(lblErro); layout.setAlignment(Pos.CENTER); layout.setStyle("-fx-background-color: #121212;");
        stage.setScene(new Scene(layout, 500, 200));
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}