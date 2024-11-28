package chessgameserver.network.packets;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class GeneralPackets {
    public static class LoginRequest {
        public String userName;
        public String passwd;
        
        public LoginRequest(){

        }
        
        public LoginRequest(String userName, String passwd) {
            this.userName = userName;
            this.passwd = passwd;
        }  
    }
    
    public static class LoginResponse{
        public int userId;
        public String userName;
        public int elo;
        public int win;
        public int lose;
        public int draw;
        public boolean isSuccess;
        public String message;
    }

    public static class RegisterRequest {
        public String userName;
        public String email;
        public String password;

        public RegisterRequest(){

        }

        public RegisterRequest(String userName,String email, String password) {
            this.userName = userName;
            this.password = password;
            this.email = email;
        }
        
    }

    public static class RegisterResponse {
        public boolean isSuccess;
        public String message;
    }

    public static class ErrorResponse {
        public String error;
    }
    
    public static class HistoryGameRequest {
        public int userId;
        public HistoryGameRequest(){

        }

        public HistoryGameRequest(int userId) {
            this.userId = userId;
        }
    }

    public static class HistoryGame{
        public String playerName;
        public String opponentName;
        public String moves;
        public String result;
        public boolean onWhite;
        public HistoryGame(){

        }
        public HistoryGame(String playerName, String opponentName, String moves, String result, boolean onWhite){
            this.playerName = playerName;
            this.opponentName = opponentName;
            this.moves = moves;
            this.result = result;
            this.onWhite = onWhite;
        }
    }


    public static class HistoryGameResponse {
        public ArrayList<HistoryGame> historyGameList = new ArrayList<>();
        public void addHistoryGameToList(String playerName, String opponentName, String moves, String result, boolean onWhite){
            historyGameList.add(new HistoryGame(playerName, opponentName, moves, result, onWhite));
        }
    }

    public static class RankingListRequest {
        public String option;

        public RankingListRequest(){

        }

        public RankingListRequest(String option) {
            this.option = option;
        }
        
    }

    public static class ImageChunk {
        public String fileName;
        public int chunkIndex;
        public int totalChunks;
        public byte[] imageData;
    }

    public static class ImageUpload {
        public String fileName;
        public byte[] imageData;
    }
    

    public static class UserRank{
        public String userName;
        public int elo;

        public UserRank(){
            
        }

        public UserRank(String userName, int elo){
            this.userName = userName;
            this.elo = elo;
        }
    }
    public static class RankingListResponse {
        public ArrayList<UserRank> rankingList = new ArrayList<>();

        public void addUserRankToList(String userName, int elo){
            rankingList.add(new UserRank(userName, elo));
        }
    }

    public static class ProfileViewRequest{
        public int userId;
        public ProfileViewRequest(){

        }

        public ProfileViewRequest(int userId) {
            this.userId = userId;
        }
        
    }
    
    public static class ProfileViewResponse{
        public String userName;
        public int elo;
        public int win;
        public int lose;
        public int draw;
    }

    public static class MsgPacket {
        public String msg;
        public MsgPacket(){

        }
        public MsgPacket(String msg) {
            this.msg = msg;
        }
        
    }


    public static class FindGameRequest{
        public int userId;
        public String name;
        public int elo;
        public FindGameRequest(){

        }
        public FindGameRequest(int userId, String name, int elo) {
            this.userId = userId;
            this.name = name;
            this.elo = elo;
        }
    }

    public static class FindGameResponse{
        public int tcpPort;
        public int udpPort;
        public String side;
    }

}