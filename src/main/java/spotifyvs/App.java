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
    private Button btnMusicaA;
    private Button btnMusicaB;
    private Button btnPlayA;
    private Button btnPlayB;
    private VBox layoutPrincipal;

    // NEW VISUAL FIELDS
    private SpotifyService spotifyService;
    private ImageView imgViewA;
    private ImageView imgViewB;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SpotifyVersus - Torneio com Player");

        // Initialize and authenticate Spotify connectivity asynchronously behind the scenes
        this.spotifyService = new SpotifyService();
        this.spotifyService.authenticate();

        SongRep repo = new SongRep();

        if (repo.getSongCount() == 0) {
            SpotifyImporter.importarAutomatico();
        }

        int realSongCount = repo.getSongCount();

        if (realSongCount < 2) {
            mostrarJanelaErro(primaryStage, "Não existem músicas suficientes na base de dados para começar.");
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

        btnMusicaA = new Button();
        btnMusicaB = new Button();

        String estiloBotaoVoto = "-fx-background-color: #1DB954; " + 
                                 "-fx-text-fill: white; " +
                                 "-fx-font-size: 14px; " +
                                 "-fx-font-weight: bold; " +
                                 "-fx-padding: 25px 40px; " +
                                 "-fx-background-radius: 15px; " +
                                 "-fx-cursor: hand; " +
                                 "-fx-alignment: center; " +
                                 "-fx-min-width: 280px; " +
                                 "-fx-text-alignment: center;";
        
        btnMusicaA.setStyle(estiloBotaoVoto);
        btnMusicaB.setStyle(estiloBotaoVoto);

        btnPlayA = new Button("▶ Ouvir no Spotify");
        btnPlayB = new Button("▶ Ouvir no Spotify");

        String estiloBotaoPlay = "-fx-background-color: #282828; " +
                                 "-fx-text-fill: #B3B3B3; " + 
                                 "-fx-font-size: 12px; " +
                                 "-fx-font-weight: bold; " +
                                 "-fx-padding: 8px 20px; " +
                                 "-fx-background-radius: 20px; " +
                                 "-fx-cursor: hand; " +
                                 "-fx-min-width: 160px;";

        btnPlayA.setStyle(estiloBotaoPlay);
        btnPlayB.setStyle(estiloBotaoPlay);

        btnMusicaA.setOnAction(e -> votar(1)); 
        btnMusicaB.setOnAction(e -> votar(2)); 

        // Added the image views right above the song selection buttons
        VBox containerA = new VBox(15, imgViewA, btnMusicaA, btnPlayA);
        containerA.setAlignment(Pos.CENTER);

        VBox containerB = new VBox(15, imgViewB, btnMusicaB, btnPlayB);
        containerB.setAlignment(Pos.CENTER);

        HBox layoutBotoes = new HBox(40, containerA, containerB);
        layoutBotoes.setAlignment(Pos.CENTER);

        layoutPrincipal = new VBox(30, lblStatus, layoutBotoes);
        layoutPrincipal.setAlignment(Pos.CENTER);
        layoutPrincipal.setStyle("-fx-background-color: #121212; -fx-padding: 40px;"); 

        avancarConfronto();

        // Increased window height slightly (from 420 to 600) to naturally accommodate the new album art frames
        Scene cena = new Scene(layoutPrincipal, 720, 600);
        primaryStage.setScene(cena);
        primaryStage.show();
    }

    private void avancarConfronto() {
        if (currentIndex >= currentRound.size()) {
            currentRound = nextRoundWinners;
            nextRoundWinners = new ArrayList<>();
            currentIndex = 0;
            roundNumber++;
        }

        if (currentRound.size() == 1) {
            mostrarVencedorFinal(currentRound.get(0));
            return;
        }

        Song s1 = currentRound.get(currentIndex);
        Song s2 = currentRound.get(currentIndex + 1);

        if (s1.isBye()) {
            nextRoundWinners.add(s2);
            currentIndex += 2;
            avancarConfronto(); 
            return;
        }
        if (s2.isBye()) {
            nextRoundWinners.add(s1);
            currentIndex += 2;
            avancarConfronto(); 
            return;
        }

        lblStatus.setText("--- RONDA " + roundNumber + " (" + currentRound.size() + " músicas restantes) ---");
        btnMusicaA.setText(s1.getTrackName() + "\n👤 " + s1.getArtistNames());
        btnMusicaB.setText(s2.getTrackName() + "\n👤 " + s2.getArtistNames());

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

        btnPlayA.setOnAction(e -> abrirNoSpotify(s1.getTrackUri()));
        btnPlayB.setOnAction(e -> abrirNoSpotify(s2.getTrackUri()));
    }

    private void votar(int escolha) {
        if (escolha == 1) {
            nextRoundWinners.add(currentRound.get(currentIndex));
        } else {
            nextRoundWinners.add(currentRound.get(currentIndex + 1));
        }
        currentIndex += 2;
        avancarConfronto(); 
    }

    private void abrirNoSpotify(String trackUri) {
        if (trackUri == null || trackUri.isEmpty()) return;

        try {
            getHostServices().showDocument(trackUri);
        } catch (Exception e) {
            System.out.println("App do Spotify não encontrada. A abrir no browser como alternativa...");
            if (trackUri.startsWith("spotify:track:")) {
                String trackId = trackUri.substring("spotify:track:".length());
                String urlWeb = "https://open.spotify.com/track/" + trackId;
                getHostServices().showDocument(urlWeb);
            }
        }
    }

    private void mostrarVencedorFinal(Song vencedor) {
        lblStatus.setText("🏆 O TORNEIO TERMINOU! A VENCEDORA É: 🏆");
        lblStatus.setStyle("-fx-font-size: 20px; -fx-text-fill: #FFD700; -fx-font-weight: bold;"); 

        Label lblVencedor = new Label(vencedor.getTrackName().toUpperCase() + "\nby " + vencedor.getArtistNames());
        lblVencedor.setStyle("-fx-font-size: 24px; -fx-text-fill: #1DB954; -fx-font-weight: bold; -fx-text-alignment: center;");

        // Display winning track artwork at the final screen
        ImageView imgVencedor = new ImageView();
        imgVencedor.setFitWidth(250);
        imgVencedor.setFitHeight(250);
        imgVencedor.setPreserveRatio(true);
        String finalArt = spotifyService.getAlbumArtUrl(vencedor.getTrackUri());
        if (finalArt != null) {
            imgVencedor.setImage(new Image(finalArt, true));
        }

        Button btnPlayVencedor = new Button("▶ Ouvir Música Campeã");
        btnPlayVencedor.setStyle("-fx-background-color: #1DB954; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 25px; -fx-background-radius: 20px; -fx-cursor: hand;");
        btnPlayVencedor.setOnAction(e -> abrirNoSpotify(vencedor.getTrackUri()));

        layoutPrincipal.getChildren().clear();
        layoutPrincipal.getChildren().addAll(lblStatus, imgVencedor, lblVencedor, btnPlayVencedor);
    }

    private void mostrarJanelaErro(Stage stage, String mensagem) {
        Label lblErro = new Label(mensagem);
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