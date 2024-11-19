package chessgameserver.network.database;

public class PlayerData{
    public int playerId;
    public String name;
    public int elo;
    public int win; 
    public int lose;
    public int draw;
    public int connectionId;
    
    public PlayerData(int playerId, String name, int elo, int win, int lose, int draw) {
        this.playerId = playerId;
        this.name = name;
        this.elo = elo;
        this.win = win;
        this.lose = lose;
        this.draw = draw;
    }
}