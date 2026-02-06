package spotifyvs;

import java.util.Collections;
import java.util.List;

public class App {
    public static void main(String[] args) {
        System.out.println("Welcome to spotify versus!");

        SongRep repo = new SongRep();

        int realSongCount = repo.getSongCount();
        if (realSongCount < 2) {
            System.out.println("Not enough songs in the database to start a game. Please import more songs.");
            return;
        }

        int highestOneBit= Integer.highestOneBit(realSongCount);
        int bracketSize = (realSongCount == highestOneBit) ? realSongCount : highestOneBit << 1;

        System.out.println("Found " + realSongCount + " songs in the database.");
        System.out.println("Starting a game with " + bracketSize + " songs (including " + (bracketSize - realSongCount) + " byes).");

        List<Song> competitors = repo.getRandomSongs(realSongCount);

        while(competitors.size() < bracketSize) {
            competitors.add(Song.createBye());
        }

        Collections.shuffle(competitors);

        TournamentManager tournament = new TournamentManager();
        Song winner = tournament.startTournament(competitors);
        System.out.println("\n******************************************");
        System.out.println("THE WINNER IS: " + winner.getTrackName().toUpperCase());
        System.out.println("Artist: " + winner.getArtistNames());
        System.out.println("******************************************");
    
    }
}
