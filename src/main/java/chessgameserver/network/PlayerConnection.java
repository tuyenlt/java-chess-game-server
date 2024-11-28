package chessgameserver.network;
import com.esotericsoftware.kryonet.Connection;

import chessgameserver.network.database.DatabaseConnection;
import chessgameserver.network.database.PlayerData;

public class PlayerConnection{
    public int playerId;
    public String name;
    public int elo;
    public int win; 
    public int lose;
    public int draw;
    public int connectionId;
    public Connection connection;
    
    public PlayerConnection(int playerId) {
        this.playerId = playerId;
        try{
            PlayerData  playerData = DatabaseConnection.getPlayerInfoById(playerId);
            name = playerData.name;
            elo = playerData.elo;
            win = playerData.win;
            lose = playerData.lose;
            draw = playerData.draw;
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    public void setConnection(Connection connection){
        this.connectionId = connection.getID();
        this.connection = connection;
    }

    
    public int getId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public void updateEloAfterGame(int eloChange, double score) {    
        elo += eloChange;
        if(elo < 0){
            elo = 0;
        }
        if (score == 1) {
            win++;
        } else if (score == 0.5) {
            draw++;
        } else if (score == 0) {
            lose++;
        }
    }

    public void saveToDatabase(){
        try{
            DatabaseConnection.updatePlayerToDB(new PlayerData(playerId, name, elo, win, lose, draw));
        }catch(Exception e){
            System.out.println(e.getMessage());     
        }
    }

}