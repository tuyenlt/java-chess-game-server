package chessgameserver.network.packets;


public class IngamePackets {

    public static class InitPacket {
        public int id;
        public InitPacket(){}
        public InitPacket(int id){
            this.id = id;
        }
    }

    public static class OpponentInfo{
        public String name;
        public int elo;
        public String side;
        public OpponentInfo(){}
        public OpponentInfo(String name, int elo, String side) {
            this.name = name;
            this.elo = elo;
            this.side = side;
        }
    }

    public static class GameStateResponse {
        public int timeWhite;
        public int timeBlack;
        public String nextTurn;
        public String lastMove;
    }

    public static class MovePacket{
        public String move;
        public MovePacket(){}
        public MovePacket(String move){
            this.move = move;
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
