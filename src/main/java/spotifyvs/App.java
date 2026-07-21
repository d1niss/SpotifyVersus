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
    private Button btnPlayA;
    private Button btnPlayB;
    private VBox mainLayout;

    private SpotifyService spotifyService;
    private ImageView imgViewA;
    private ImageView imgViewB;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SpotifyVersus");

        // 1. Loading Screen
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
                // 2. Load the playlist selection UI instead of launching the tournament right away
                List<PlaylistSimplified> playlists = this.spotifyService.getCurrentUsersPlaylists();
                Platform.runLater(() -> showPlaylistSelectionUI(primaryStage, playlists));
            } else {
                Platform.runLater(() -> showErrorMessage(primaryStage, "Authentication failed."));
            }
        }).start();
    }

    /**
     * Renders a screen showing all the user's personal/collaborative playlists.
     */
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

        // Format list items nicely to show name + track totals
        listView.setCellFactory(param -> new ListCell<PlaylistSimplified>() {
            @Override
            protected void updateItem(PlaylistSimplified item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #181818;");
                } else {
                    // Safe guard: check if the tracks object is null before grabbing the total
                    int totalTracks = (item.getTracks() != null) ? item.getTracks().getTotal() : 0;
            
                    setText(item.getName() + " (" + totalTracks + " tracks)");
                    setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px;");
                }
            }
        });

        Button btnSelect = new Button("Load Tournament");
        btnSelect.setStyle("-fx-background-color: #1DB954; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 30px; -fx-background-radius: 20px; -fx-cursor: hand;");
        btnSelect.setDisable(true);

        // Enable button only when a selection is made
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            btnSelect.setDisable(newVal == null);
        });

        btnSelect.setOnAction(e -> {
            PlaylistSimplified selectedPlaylist = listView.getSelectionModel().getSelectedItem();
            if (selectedPlaylist != null) {
                // Here you would fetch the playlist tracks using selectedPlaylist.getId()
                // For now, it proceeds directly to the local game setup
                setupTournamentUI(primaryStage);
            }
        });

        selectionLayout.getChildren().addAll(lblTitle, listView, btnSelect);
        primaryStage.setScene(new Scene(selectionLayout, 720, 600));
    }

    private void setupTournamentUI(Stage primaryStage) {
        SpotifyImporter.autoImport();
        SongRep repo = new SongRep();
        int realSongCount = repo.getSongCount();

        if (realSongCount < 2) {
            showErrorMessage(primaryStage, "Not enough songs in the database. Please import more songs.");
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

        imgViewA = new ImageView(); imgViewA.setFitWidth(200); imgViewA.setFitHeight(200); imgViewA.setPreserveRatio(true);
        imgViewB = new ImageView(); imgViewB.setFitWidth(200); imgViewB.setFitHeight(200); imgViewB.setPreserveRatio(true);

        btnMusicA = new Button(); btnMusicB = new Button();
        String voteButtonStyle = "-fx-background-color: #1DB954; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 25px 40px; -fx-background-radius: 15px; -fx-cursor: hand; -fx-min-width: 280px; -fx-text-alignment: center;";
        btnMusicA.setStyle(voteButtonStyle); btnMusicB.setStyle(voteButtonStyle);

        btnPlayA = new Button("▶ Open in Spotify"); btnPlayB = new Button("▶ Open in Spotify");
        String playButtonStyle = "-fx-background-color: #282828; -fx-text-fill: #B3B3B3; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8px 20px; -fx-background-radius: 20px; -fx-cursor: hand; -fx-min-width: 160px;";
        btnPlayA.setStyle(playButtonStyle); btnPlayB.setStyle(playButtonStyle);

        btnMusicA.setOnAction(e -> vote(1)); 
        btnMusicB.setOnAction(e -> vote(2)); 

        VBox containerA = new VBox(15, imgViewA, btnMusicA, btnPlayA); containerA.setAlignment(Pos.CENTER);
        VBox containerB = new VBox(15, imgViewB, btnMusicB, btnPlayB); containerB.setAlignment(Pos.CENTER);
        HBox layoutBtn = new HBox(40, containerA, containerB); layoutBtn.setAlignment(Pos.CENTER);

        mainLayout = new VBox(30, lblStatus, layoutBtn);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-background-color: #121212; -fx-padding: 40px;"); 

        advanceConfront();
        primaryStage.setScene(new Scene(mainLayout, 720, 600));
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

        String artUrlA = spotifyService.getAlbumArtUrl(s1.getTrackUri());
        imgViewA.setImage(artUrlA != null ? new Image(artUrlA, true) : null);

        String artUrlB = spotifyService.getAlbumArtUrl(s2.getTrackUri());
        imgViewB.setImage(artUrlB != null ? new Image(artUrlB, true) : null);

        btnPlayA.setOnAction(e -> openInSpotify(s1.getTrackUri()));
        btnPlayB.setOnAction(e -> openInSpotify(s2.getTrackUri()));
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