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


    public int gameEndWith(PlayerConnection other, double score){
        double expectedElo = 1.0 / (1 + Math.pow(10, (other.elo - this.elo) / 400.0));
        int newElo = (int) Math.round(this.elo + 200 * (score - expectedElo));
        this.elo = newElo;
        if(score == 1){
            win++;
        }
        if(score == 0.5){
            draw++;
        }
        if(score == 0){
            lose++;
        }
        return (int)(200 * (score - expectedElo));
    }

    public void saveToDatabase(){
        try{
            DatabaseConnection.updatePlayerToDB(new PlayerData(playerId, name, elo, win, lose, draw));
        }catch(Exception e){
            System.out.println(e.getMessage());     
        }
    }

}