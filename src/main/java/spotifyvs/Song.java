package spotifyvs;

public class Song {
    private String trackUri;
    private String trackName;
    private String artistNames;
    private String albumName;
    private boolean isBye = false; // flag to indicate if this is a "bye" song (used for odd number of songs in a round)
   
    public Song(String trackUri, String trackName, String artistNames, String albumName) {
        this.trackUri = trackUri;
        this.trackName = trackName;
        this.artistNames = artistNames;
        this.albumName = albumName;
    }

    public static Song createBye() {
        Song bye = new Song(null, "Bye Song", "None", "None");
        bye.isBye = true;
        return bye;
    }

    public boolean isBye() {
        return isBye;
    }

    public String getTrackUri() {
        return trackUri; 
    }
    public String getTrackName() {
        return trackName;
    }
    public String getArtistNames() { 
        return artistNames; 
    }
    public String getAlbumName() { 
        return albumName; 
    }

    @Override
    public String toString() {
        return trackName + " by " + artistNames;
    }
    
}
