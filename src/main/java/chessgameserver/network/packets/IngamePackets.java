package chessgameserver.network.packets;

public class IngamePackets {

    public static class InitPacket {
        public int id;
    }

    public static class OpponentInfo{
        public String name;
        public int elo;
        public OpponentInfo(){}
        public OpponentInfo(String name, int elo) {
            this.name = name;
            this.elo = elo;
        }
    }

    public static class GameStateResponse {
        public int timeWhite;
        public int timeBlack;
        public String nextTurn;
        public String lastMove;
    }

    public static class MovePacket{
        public int stX;
        public int stY;
        public int enX;
        public int enY;

        public MovePacket(){
            
        }

        public MovePacket(int stX, int stY, int enX, int enY) {
            this.stX = stX;
            this.stY = stY;
            this.enX = enX;
            this.enY = enY;
        }
    }

    public static class GameEndResponse{
        public double state;
        public int totalsMoves;
        public int eloChange;
        public GameEndResponse(){}
        public GameEndResponse(double state, int totalsMoves, int eloChange) {
            this.state = state;
            this.totalsMoves = totalsMoves;
            this.eloChange = eloChange;
        }
        
    }
}
