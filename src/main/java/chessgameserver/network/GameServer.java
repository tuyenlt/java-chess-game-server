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
import java.util.concurrent.Executors;




public class GameServer{
    private Server server;
    private PlayerConnection whitePlayer;
    private PlayerConnection blackPlayer;
    private int tcpPort;
    private int udpPort;
    private Board board = new Board();

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
                handleGameEnd(1);
            }else{
                handleGameEnd(0);
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
        if (timerExecutor != null && !timerExecutor.isShutdown()) {
            timerExecutor.shutdown();
        }
        List<String> allMoves = board.getMoves("a");
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
