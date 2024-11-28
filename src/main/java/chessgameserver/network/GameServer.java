package chessgameserver.network;

import com.esotericsoftware.kryonet.Server;

import chessgameserver.network.database.DatabaseConnection;
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
import java.util.concurrent.Executors;




public class GameServer{
    private Server server;
    private PlayerConnection whitePlayer;
    private PlayerConnection blackPlayer;
    private int tcpPort;
    private int udpPort;
    private Board board = new Board();
    private boolean isEndGame = false;

    private ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(1);
    private int whitePlayerTime = 10 * 60; 
    private int blackPlayerTime = 10 * 60; 
    private int connectionCount = 0;
    
    public GameServer(int whitePlayerId, int blackPlayerId){
        server = new Server();
        PacketsRegester.register(server);
        try{
            whitePlayer = new PlayerConnection(whitePlayerId);
            blackPlayer = new PlayerConnection(blackPlayerId);
            System.out.println("white id" + whitePlayerId + " blackid" + blackPlayerId);
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        boolean isFindNewPort = false;
        while (!isFindNewPort) {   
            try{
                int[] ports = getTwoFreePorts();
                tcpPort = ports[0];
                udpPort = ports[1];
                server.bind(tcpPort, udpPort);
                isFindNewPort = true;
            }catch(IOException ex){
                System.err.println(ex);
            }
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
            serverSocket1.close();
            serverSocket2.close();
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
                    handleGameEnd(0);
                }
            } else {
                blackPlayerTime--;
                if (blackPlayerTime <= 0) {
                    handleGameEnd(1);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void handlePlayerConnected(Connection connection,InitPacket initPacket){
        System.out.println("Init Packet" + initPacket.id);
        if(initPacket.id == whitePlayer.getId()){
            whitePlayer.setConnection(connection);
            connectionCount++;
        }
        if(initPacket.id == blackPlayer.getId()){
            blackPlayer.setConnection(connection);
            connectionCount++;
        }
        if(connectionCount == 2){
            whitePlayer.connection.sendTCP(new OpponentInfo(blackPlayer.getName(),blackPlayer.getElo(), "b"));
            blackPlayer.connection.sendTCP(new OpponentInfo(whitePlayer.getName(),whitePlayer.getElo(), "w"));
            startPlayerTimers();
        }
    }

    private void handleMsgPacket(Connection connection, Object object){
        MsgPacket request = (MsgPacket)object;
        if(request.msg.equals("/surrender")){
            if(connection.getID() == whitePlayer.connectionId){
                handleGameEnd(0);
            }else{
                handleGameEnd(1);
            }
        }
    }

    private void handleMoveRequest(Connection connection, Object object){
        MovePacket request = (MovePacket)object;
        Move newMove = new Move(request.move);
        System.out.println(newMove);
        if(board.isValidMove(newMove.getStartRow(), newMove.getStartCol(), newMove.getEndRow(), newMove.getEndCol())){
            board.movePiece(newMove);
            System.out.println(newMove);
        }else{
            //TODO  handle player cheating
        }
        
        if(connection.equals(whitePlayer.connection)){
            blackPlayer.connection.sendTCP(new MovePacket(newMove.toString()));
        }else{
            whitePlayer.connection.sendTCP(new MovePacket(newMove.toString()));
        }

        if(board.gameState().equals("ongoing")){
            return;
        }
        
        if(board.gameState().equals("win")){
            if(board.getCurrentTurn().equals("b")){
                handleGameEnd(1);
            }else{
                handleGameEnd(0);
            }
            return;
        }

        if(board.gameState().equals("draw")){
            handleGameEnd(0.5);
        }

    }


    private void handleGameEnd(double whiteScore){
        if (timerExecutor != null && !timerExecutor.isShutdown()) {
            timerExecutor.shutdown();
        }
        List<String> allMoves = board.getMoves("a");
        int eloChange = calculateEloChange(whitePlayer.getElo(), blackPlayer.getElo(), whiteScore);
        int whiteEloChange = eloChange;
        int blackEloChange = -eloChange;
        whitePlayer.updateEloAfterGame(whiteEloChange, whiteScore);
        blackPlayer.updateEloAfterGame(blackEloChange, 1 - whiteScore);

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

        String result = "draw";
        if(whiteScore == 1){
            result = "win";
        }else if(whiteScore == 0){  
            result = "lose";
        }
        System.out.println(allMoves.size());
        System.out.println(String.join(" ", allMoves));
        try{
            DatabaseConnection.saveGameHistory(whitePlayer.getId(), blackPlayer.getId(), result, String.join(" ", allMoves));  
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        
        whitePlayer.saveToDatabase();
        blackPlayer.saveToDatabase();
        isEndGame = true;
        server.stop();
    }

    private void handlePlayerDisconnect(Connection connection){
        if(isEndGame){
            return;
        }
        if(connection.getID() == whitePlayer.connectionId){
            handleGameEnd(0);            
        }else{
            handleGameEnd(1);
        } 
    }

    public int calculateEloChange(int whiteElo, int blackElo, double whiteScore){
        if (whiteScore != 0 && whiteScore != 0.5 && whiteScore != 1) {
            throw new IllegalArgumentException("Score must be 0 (loss), 0.5 (draw), or 1 (win).");
        }
    
        final int K_FACTOR = 150;
        double expectedScore = 1.0 / (1 + Math.pow(10, (blackElo - whiteElo) / 400.0));
        return (int) Math.round(K_FACTOR * (whiteScore - expectedScore));
    }
}
