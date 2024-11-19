package chessgameserver.network;

import com.esotericsoftware.kryonet.Server;

import chessgameserver.network.packets.*;
import chessgameserver.network.packets.GeneralPackets.*;
import chessgameserver.network.packets.IngamePackets.*;
import chessgameserver.logic.Board;
import chessgameserver.logic.Move;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.IOException;



public class GameServer{
    private Server server;
    private PlayerConnection whitePlayer;
    private PlayerConnection blackPlayer;
    private int tcpPort;
    private int udpPort;
    private Board board = new Board();

    private ScheduledExecutorService timerExecutor;
    private int whitePlayerTime = 10 * 60; 
    private int blackPlayerTime = 10 * 60; 
    
    public GameServer(int whitePlayerId, int blackPlayerId){
        server = new Server();
        PacketsRegester.register(server);
        try{
            whitePlayer = new PlayerConnection(whitePlayerId);
            blackPlayer = new PlayerConnection(blackPlayerId);
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        try{
            int[] ports = getTwoFreePorts();
            tcpPort = ports[0];
            udpPort = ports[1];
            server.bind(tcpPort, udpPort);
        }catch(IOException ex){
            System.err.println(ex);
        }
    }

    private int[] getTwoFreePorts() throws IOException {
        int[] ports = new int[2]; 
        try (
            ServerSocket serverSocket1 = new ServerSocket(0);
            ServerSocket serverSocket2 = new ServerSocket(0)
            ) { 
            ports[0] = serverSocket1.getLocalPort(); 
            ports[1] = serverSocket2.getLocalPort(); 
        } 
        return ports; 
    }

    public int[] getServerPorts(){
        int[] ports = {tcpPort, udpPort};
        return ports;
    }

    public String getState(){
        StringBuilder state = new StringBuilder();
        return state.toString();
    }

    public void run(){
        server.start();
        System.out.println("start listening for you guy");
        server.addListener(new Listener(){  
            
            @Override
            public void connected(Connection connection) {
                super.connected(connection);
            }

            public void received (Connection connection, Object object) {
                if (object instanceof InitPacket){
                    handlePlayerConnected(connection, (InitPacket)object);
                }

                if (object instanceof MsgPacket) {
                   handleMsgPacket(connection, object);
                }

                if (object instanceof MovePacket){
                    handleMoveRequest(connection, object);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                handlePlayerDisconnect(connection); 
                super.disconnected(connection);
            } 
        });
    }

     private void startPlayerTimers() {
        timerExecutor.scheduleAtFixedRate(() -> {
            if (board.getCurrentTurn().equals("w")) {
                whitePlayerTime--;
                if (whitePlayerTime <= 0) {
                    handleGameEnd(0); // Black wins
                }
            } else {
                blackPlayerTime--;
                if (blackPlayerTime <= 0) {
                    handleGameEnd(1); // White wins
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void handlePlayerConnected(Connection connection,InitPacket initPacket){
        if(initPacket.id == whitePlayer.getId()){
            whitePlayer.setConnection(connection);
        }
        if(initPacket.id == blackPlayer.getId()){
            blackPlayer.setConnection(connection);
        }
        if(server.getConnections().length == 2){
            whitePlayer.connection.sendTCP(new OpponentInfo(blackPlayer.getName(),blackPlayer.getElo()));
            blackPlayer.connection.sendTCP(new OpponentInfo(whitePlayer.getName(),whitePlayer.getElo()));
            startPlayerTimers();
        }
    }

    private void handleMsgPacket(Connection connection, Object object){
        MsgPacket request = (MsgPacket)object;
        System.out.println(request.msg);

        MsgPacket response = new MsgPacket();
        response.msg = "Welcome to our house bitch";
        connection.sendTCP(response);
    }

    private void handleMoveRequest(Connection connection, Object object){
        MovePacket request = (MovePacket)object;
        Move newMove = new Move(request.stX, request.stY, request.enX, request.enY);
        if(connection.getID() == blackPlayer.connectionId){
            newMove.reverseBoard();
        }
        if(board.isValidMove(request.stX, request.stY, request.enX, request.enY)){
            board.movePiece(newMove);
        }else{
            //TODO  handle player cheating
        }
        
        for(Connection conn : server.getConnections()){
            conn.sendTCP(request);
        }

        if(board.gameState().equals("ongoing")){
            return;
        }

        
        if(board.gameState().equals("win")){
            if(board.getCurrentTurn().equals("w")){
                handleGameEnd(1);
            }else{
                handleGameEnd(0);
            }
        }

        if(board.gameState().equals("draw")){
            handleGameEnd(0.5);
        }

    }


    private void handleGameEnd(double whiteScore){
        List<Move> allMoves = board.getAllMoves();
        int whiteEloChange = whitePlayer.gameEndWith(blackPlayer, whiteScore);
        int blackEloChange = blackPlayer.gameEndWith(whitePlayer, 1 - whiteScore);
        whitePlayer.connection.sendTCP(new GameEndResponse(
            whiteScore,
            allMoves.size(),
            whiteEloChange
        ));                

        blackPlayer.connection.sendTCP(new GameEndResponse(
            1 - whiteScore,
            allMoves.size(),
            blackEloChange
        ));     
        // TODO save game history


        whitePlayer.saveToDatabase();
        blackPlayer.saveToDatabase();
        server.stop();
    }

    private void handlePlayerDisconnect(Connection connection){
        if(connection.getID() == whitePlayer.connectionId){
            handleGameEnd(0);            
        }else{
            handleGameEnd(1);
        } 
    }
}
