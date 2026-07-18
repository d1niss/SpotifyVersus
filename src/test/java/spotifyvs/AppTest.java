package spotifyvs;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class AppTest 
{

    /**
     * Verifies that standard Song objects are instantiated correctly
     * and maintain their properties.
     */
    @Test
    public void testSongCreation() {
        Song song = new Song("spotify:track:123", "Blinding Lights", "The Weeknd", "After Hours");
        
        assertEquals("spotify:track:123", song.getTrackUri());
        assertEquals("Blinding Lights", song.getTrackName());
        assertEquals("The Weeknd", song.getArtistNames());
        assertEquals("After Hours", song.getAlbumName());
        assertFalse(song.isBye());
    }

    /**
     * Verifies that the factory method correctly generates a standard "Bye" song
     * to manage odd numbers of contestants in a bracket round.
     */
    @Test
    public void testSongCreateBye() {
        Song bye = Song.createBye();
        
        assertTrue(bye.isBye());
        assertNull(bye.getTrackUri());
        assertEquals("Bye Song", bye.getTrackName());
    }

    /**
     * Tests the bracket scaling mathematical logic used inside App.java.
     * Ensures the total number of entries always safely elevates to the next power of 2.
     */
    @Test
    public void testBracketSizeCalculation() {
        // Case 1: 5 songs should round up to an 8-slot bracket layout
        int songCount5 = 5;
        int highestOneBit5 = Integer.highestOneBit(songCount5);
        int bracketSize5 = (songCount5 == highestOneBit5) ? songCount5 : highestOneBit5 << 1;
        assertEquals(8, bracketSize5);

        // Case 2: 4 songs (already a perfect power of 2) should remain 4
        int songCount4 = 4;
        int highestOneBit4 = Integer.highestOneBit(songCount4);
        int bracketSize4 = (songCount4 == highestOneBit4) ? songCount4 : highestOneBit4 << 1;
        assertEquals(4, bracketSize4);
        
        // Case 3: 9 songs should round up to a 16-slot bracket layout
        int songCount9 = 9;
        int highestOneBit9 = Integer.highestOneBit(songCount9);
        int bracketSize9 = (songCount9 == highestOneBit9) ? songCount9 : highestOneBit9 << 1;
        assertEquals(16, bracketSize9);
    }

    /**
     * Simulates a live tournament using an isolated InputStream stream.
     * Tests how the tournament manager processes a mix of valid tracks and "Byes".
     */
    @Test
    public void testTournamentWithByes() {
        // Set up 4 bracket participants (2 active songs, 2 byes)
        Song s1 = new Song("uri1", "Song A", "Artist A", "Album A");
        Song s2 = new Song("uri2", "Song B", "Artist B", "Album B");
        Song bye1 = Song.createBye();
        Song bye2 = Song.createBye();

        List<Song> participants = new ArrayList<>();
        participants.add(s1);
        participants.add(s2);
        participants.add(bye1);
        participants.add(bye2);

        // Simulate typing "1" into terminal for the Matchup between Song A and Song B
        String simulatedUserInput = "1\n";
        InputStream originalSystemIn = System.in;
        System.setIn(new ByteArrayInputStream(simulatedUserInput.getBytes()));

        try {
            TournamentManager tm = new TournamentManager();
            Song tournamentWinner = tm.startTournament(participants);
            
            /* 
             * Breakdown of Execution Simulation:
             * Round 1, Match 1: s1 vs s2 -> User enters "1" -> s1 wins.
             * Round 1, Match 2: bye1 vs bye2 -> bye1 is a bye -> returns bye2 automatically.
             * Round 2 (Final): s1 vs bye2 -> bye2 is a bye -> returns s1 automatically without prompt.
             */
            assertEquals(s1, tournamentWinner);
            
        } finally {
            // Restore System.in to prevent bleeding into other tests
            System.setIn(originalSystemIn);
        }
    }
}
