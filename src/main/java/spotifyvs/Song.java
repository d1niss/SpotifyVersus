package spotifyvs;

public class Song {
    private String trackUri;
    private String trackName;
    private String artistNames;
    private String albumName;
    // You can add more fields (like popularity or danceability) if you want to use them later

    public Song(String trackUri, String trackName, String artistNames, String albumName) {
        this.trackUri = trackUri;
        this.trackName = trackName;
        this.artistNames = artistNames;
        this.albumName = albumName;
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
