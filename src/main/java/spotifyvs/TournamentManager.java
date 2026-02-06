package spotifyvs;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TournamentManager { 
    private final Scanner scanner;

    public TournamentManager() {
        this.scanner = new Scanner(System.in);
    }

    public Song startTournament(List<Song> participants) {
        List<Song> currentRound = new ArrayList<>(participants);
        int roundNumber = 1;

        while (currentRound.size() > 1) {
            System.out.println("\n--- ROUND " + roundNumber + " (" + currentRound.size() + " songs remaining) ---");
            List<Song> nextRoundWinners = new ArrayList<>();

            for (int i = 0; i < currentRound.size(); i += 2) {
                Song song1 = currentRound.get(i);
                Song song2 = currentRound.get(i + 1);

                Song winner = faceOff(song1, song2);
                nextRoundWinners.add(winner);
            }

            currentRound = nextRoundWinners;
            roundNumber++;
        }
        return currentRound.get(0);
    }

    private Song faceOff(Song s1, Song s2) {
        // Logic for "Bye" / Blank songs
        if (s1.isBye()) {
            System.out.println("-> " + s2.getTrackName() + " gets a free pass!");
            return s2;
        }
        if (s2.isBye()) {
            System.out.println("-> " + s1.getTrackName() + " gets a free pass!");
            return s1;
        }

        // Normal Face-off
        System.out.println("\nMATCH UP:");
        System.out.println("1. " + s1.getTrackName() + " - " + s1.getArtistNames());
        System.out.println("   VS");
        System.out.println("2. " + s2.getTrackName() + " - " + s2.getArtistNames());

        while (true) {
            System.out.print("Type 1 or 2 to vote: ");
            String input = scanner.nextLine();

            if (input.equals("1")) {
                return s1;
            } else if (input.equals("2")) {
                return s2;
            } else {
                System.out.println("Invalid input.");
            }
        }
    }
    
}
