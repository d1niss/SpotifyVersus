package spotifyvs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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

    // NEW VISUAL FIELDS
    private SpotifyService spotifyService;
    private ImageView imgViewA;
    private ImageView imgViewB;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SpotifyVersus");

        // Initialize and authenticate Spotify connectivity asynchronously behind the scenes
        this.spotifyService = new SpotifyService();
        this.spotifyService.authenticate();

        // Always import the current CSV file right away on startup
        SpotifyImporter.autoImport();

        SongRep repo = new SongRep();
        int realSongCount = repo.getSongCount();

        if (realSongCount < 2) {
            showErrorMessage(primaryStage, "Not enough songs in the database to start a tournament. Please import more songs.");
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

        // Initialize Image Views for Album Covers
        imgViewA = new ImageView();
        imgViewA.setFitWidth(200);
        imgViewA.setFitHeight(200);
        imgViewA.setPreserveRatio(true);

        imgViewB = new ImageView();
        imgViewB.setFitWidth(200);
        imgViewB.setFitHeight(200);
        imgViewB.setPreserveRatio(true);

        btnMusicA = new Button();
        btnMusicB = new Button();

        String voteButtonStyle = "-fx-background-color: #1DB954; " + 
                                 "-fx-text-fill: white; " +
                                 "-fx-font-size: 14px; " +
                                 "-fx-font-weight: bold; " +
                                 "-fx-padding: 25px 40px; " +
                                 "-fx-background-radius: 15px; " +
                                 "-fx-cursor: hand; " +
                                 "-fx-alignment: center; " +
                                 "-fx-min-width: 280px; " +
                                 "-fx-text-alignment: center;";
        
        btnMusicA.setStyle(voteButtonStyle);
        btnMusicB.setStyle(voteButtonStyle);

        btnPlayA = new Button("▶ Open in Spotify");
        btnPlayB = new Button("▶ Open in Spotify");

        String playButtonStyle = "-fx-background-color: #282828; " +
                                 "-fx-text-fill: #B3B3B3; " + 
                                 "-fx-font-size: 12px; " +
                                 "-fx-font-weight: bold; " +
                                 "-fx-padding: 8px 20px; " +
                                 "-fx-background-radius: 20px; " +
                                 "-fx-cursor: hand; " +
                                 "-fx-min-width: 160px;";

        btnPlayA.setStyle(playButtonStyle);
        btnPlayB.setStyle(playButtonStyle);

        btnMusicA.setOnAction(e -> vote(1)); 
        btnMusicB.setOnAction(e -> vote(2)); 

        // Added the image views right above the song selection buttons
        VBox containerA = new VBox(15, imgViewA, btnMusicA, btnPlayA);
        containerA.setAlignment(Pos.CENTER);

        VBox containerB = new VBox(15, imgViewB, btnMusicB, btnPlayB);
        containerB.setAlignment(Pos.CENTER);

        HBox layoutBtn = new HBox(40, containerA, containerB);
        layoutBtn.setAlignment(Pos.CENTER);

        mainLayout = new VBox(30, lblStatus, layoutBtn);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-background-color: #121212; -fx-padding: 40px;"); 

        advanceConfront();

        // Increased window height slightly (from 420 to 600) to naturally accommodate the new album art frames
        Scene sceneOn = new Scene(mainLayout, 720, 600);
        primaryStage.setScene(sceneOn);
        primaryStage.show();
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

        if (s1.isBye()) {
            nextRoundWinners.add(s2);
            currentIndex += 2;
            advanceConfront(); 
            return;
        }
        if (s2.isBye()) {
            nextRoundWinners.add(s1);
            currentIndex += 2;
            advanceConfront(); 
            return;
        }

        lblStatus.setText("--- ROUND " + roundNumber + " (" + currentRound.size() + " songs remaining) ---");
        btnMusicA.setText(s1.getTrackName() + "\n👤 " + s1.getArtistNames());
        btnMusicB.setText(s2.getTrackName() + "\n👤 " + s2.getArtistNames());

        // DYNAMIC ARTWORK LOADER
        String artUrlA = spotifyService.getAlbumArtUrl(s1.getTrackUri());
        if (artUrlA != null) {
            // The true flag parameters tell JavaFX to download the image smoothly on a background worker thread
            imgViewA.setImage(new Image(artUrlA, true));
        } else {
            imgViewA.setImage(null);
        }

        String artUrlB = spotifyService.getAlbumArtUrl(s2.getTrackUri());
        if (artUrlB != null) {
            imgViewB.setImage(new Image(artUrlB, true));
        } else {
            imgViewB.setImage(null);
        }

        btnPlayA.setOnAction(e -> openInSpotify(s1.getTrackUri()));
        btnPlayB.setOnAction(e -> openInSpotify(s2.getTrackUri()));
    }

    private void vote(int choice) {
        if (choice == 1) {
            nextRoundWinners.add(currentRound.get(currentIndex));
        } else {
            nextRoundWinners.add(currentRound.get(currentIndex + 1));
        }
        currentIndex += 2;
        advanceConfront(); 
    }

    private void openInSpotify(String trackUri) {
        if (trackUri == null || trackUri.isEmpty()) return;

        try {
            getHostServices().showDocument(trackUri);
        } catch (Exception e) {
            System.out.println("App not found, opening in browser instead.");
            if (trackUri.startsWith("spotify:track:")) {
                String trackId = trackUri.substring("spotify:track:".length());
                String urlWeb = "https://open.spotify.com/track/" + trackId;
                getHostServices().showDocument(urlWeb);
            }
        }
    }

    private void showFinalWinner(Song winner) {
        lblStatus.setText("🏆 AND THE WINNER IS: 🏆");
        lblStatus.setStyle("-fx-font-size: 20px; -fx-text-fill: #FFD700; -fx-font-weight: bold;"); 

        Label lblWinner = new Label(winner.getTrackName().toUpperCase() + "\nby " + winner.getArtistNames());
        lblWinner.setStyle("-fx-font-size: 24px; -fx-text-fill: #1DB954; -fx-font-weight: bold; -fx-text-alignment: center;");

        // Display winning track artwork at the final screen
        ImageView imgWinner = new ImageView();
        imgWinner.setFitWidth(250);
        imgWinner.setFitHeight(250);
        imgWinner.setPreserveRatio(true);
        String finalArt = spotifyService.getAlbumArtUrl(winner.getTrackUri());
        if (finalArt != null) {
            imgWinner.setImage(new Image(finalArt, true));
        }

        Button btnPlayWinner = new Button("▶ Open Last Winner in Spotify");
        btnPlayWinner.setStyle("-fx-background-color: #1DB954; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 25px; -fx-background-radius: 20px; -fx-cursor: hand;");
        btnPlayWinner.setOnAction(e -> openInSpotify(winner.getTrackUri()));

        mainLayout.getChildren().clear();
        mainLayout.getChildren().addAll(lblStatus, imgWinner, lblWinner, btnPlayWinner);
    }

    private void showErrorMessage(Stage stage, String message) {
        Label lblErro = new Label(message);
        lblErro.setStyle("-fx-text-fill: #FF5555; -fx-font-size: 16px; -fx-font-weight: bold;");
        VBox layout = new VBox(lblErro);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #121212;");
        stage.setScene(new Scene(layout, 500, 200));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}