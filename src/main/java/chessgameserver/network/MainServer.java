package chessgameserver.network;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.security.auth.login.FailedLoginException;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;


import chessgameserver.network.database.DatabaseConnection;
import chessgameserver.network.packets.PacketsRegester;
import chessgameserver.network.packets.GeneralPackets.*;

public class MainServer {
    private Server server;
    private int tcpPort;
    private int udpPort;
    private ArrayList<WaitingPlayer> waitingPlayers = new ArrayList<>();
    
    class WaitingPlayer{
        public Connection connection;
        public int playerId;
        public String name;
        public int elo;

        public WaitingPlayer(Connection connection,int playerId, String name, int elo) {
            this.connection = connection;
            this.playerId = playerId;
            this.name = name;
            this.elo = elo;
        }
    }

    public MainServer(int tcpPort ,int udpPort){
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;

        server = new Server(2 * 1024 * 1024, 2 * 1024 * 1024); // max 2MB
        PacketsRegester.register(server);

    }


    public void run() throws IOException{
        server.start();
        server.bind(tcpPort, udpPort);
        System.out.println(server.toString());
        server.addListener(new Listener(){

            public void received(Connection connection, Object object){
                if(object instanceof LoginRequest){
                    handleLogin(connection, object);
                }

                if(object instanceof RegisterRequest){
                    handleRegister(connection, object);
                }

                if(object instanceof RankingListRequest){
                    handleGetRankingList(connection, object);
                }

                if(object instanceof HistoryGameRequest){
                    handleHistoryGame(connection, object);
                }

                if(object instanceof ProfileViewRequest){
                    handleViewProfile(connection, object);
                }

                if(object instanceof FindGameRequest){
                    handleWatingPlayer(connection, object);
                }

                if(object instanceof MsgPacket){
                    handleMsgPacket(connection, object);
                }

            }

            public void disconnected(Connection connection){
                for(WaitingPlayer player : waitingPlayers){
                    if(player.connection.equals(connection)){
                        waitingPlayers.remove(player);
                        return;
                    }
                }
            }
        });
        server.addListener(new ImageChunkHandler());
    }

    private void handleMsgPacket(Connection connection, Object object){
        MsgPacket msgPacket = (MsgPacket)object;
        if(msgPacket.msg.equals("/cancel-find-game")){
            for(WaitingPlayer player : waitingPlayers){
                if(player.connection.equals(connection)){
                    waitingPlayers.remove(player);
                    return;
                }
            }
        }
    }


    public void sendImage(Connection connection, File file, String name) {
        final int CHUNK_SIZE = 8000;
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int chunkIndex = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                ImageChunk chunk = new ImageChunk();
                chunk.fileName = name;
                chunk.chunkIndex = chunkIndex++;
                chunk.totalChunks = (int) Math.ceil((double) file.length() / CHUNK_SIZE);
                chunk.imageData = bytesRead == CHUNK_SIZE ? buffer : java.util.Arrays.copyOf(buffer, bytesRead);

                connection.sendTCP(chunk);
                System.out.println("Sent chunk " + chunk.chunkIndex + "/" + chunk.totalChunks + " of file " + name);
            }

            System.out.println("Image " + name + " sent to server in chunks.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to read or send file: " + file.getAbsolutePath());
        }
    }

    public void sendAvatar(Connection connection, String name){
        String fileName = name + ".jpg";
        File file = new File("uploaded-images/" + fileName);
        if (!file.exists()) {
            file = new File("uploaded-images/avatar-holder.jpg");
        }
        sendImage(connection, file, fileName);
    }

    private void handleLogin(Connection connection, Object object) {
        LoginRequest request = (LoginRequest) object;
        new Thread(() -> {
            try {
                LoginResponse response = DatabaseConnection.loginAuthentication(request);
                String fileName = response.userName + ".jpg";
                File file = new File("uploaded-images/" + fileName);
                if (!file.exists()) {
                    file = new File("uploaded-images/avatar-holder.jpg");
                }
                synchronized (connection.getEndPoint()) {
                    sendImage(connection, file, fileName);
                    connection.sendTCP(response);
                }
            } catch (Exception error) {
                LoginResponse response = new LoginResponse();
                response.isSuccess = false;
                response.message = error.getMessage();
                connection.sendTCP(response);
            }
        }).start();
    }
    


    private void handleRegister(Connection connection, Object object){
        try{
            RegisterRequest request = (RegisterRequest)object;
            RegisterResponse response = DatabaseConnection.registerNewUser(request);
            connection.sendTCP(response);
        }catch(Exception ex){
            RegisterResponse response = new RegisterResponse();
            response.isSuccess = false;
            response.message = ex.getMessage();
            connection.sendTCP(response);
            System.out.println(ex.getMessage());
        }
    }
    
    private void handleGetRankingList(Connection connection, Object object){
        try{
            RankingListRequest request = (RankingListRequest)object;
            RankingListResponse response = DatabaseConnection.getRankingList(request);
            connection.sendTCP(response);
        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }

    private void handleViewProfile(Connection connection, Object object){
        try{
            ProfileViewRequest request = (ProfileViewRequest)object;
            ProfileViewResponse response = DatabaseConnection.getProfile(request);
            connection.sendTCP(response);
        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }
    

    private void handleHistoryGame(Connection connection, Object object){
        try {   
            HistoryGameRequest request = (HistoryGameRequest)object;
            HistoryGameResponse response = DatabaseConnection.getUserHistoryGame(request);
            connection.sendTCP(response);
        } catch (Exception ex) {
            // TODO: handle exception
        }
    }
    
    private void handleWatingPlayer(Connection connection, Object object){
        FindGameRequest request = (FindGameRequest)object;
        boolean isFoundNewGame = false;
        System.out.println(request.userId+ " " + request.elo);
        for(WaitingPlayer waitingPlayer: waitingPlayers){
            if(Math.abs(waitingPlayer.elo - request.elo) <= 400){
                //*! create new game server here
                //*!
                //*!
                GameServer gameServer = new GameServer(waitingPlayer.playerId, request.userId);
                int[] newServerPort = gameServer.getServerPorts();
                FindGameResponse response = new FindGameResponse();
                response.tcpPort = newServerPort[0];
                response.udpPort = newServerPort[1];    
                gameServer.run();

                response.side = "w";
                waitingPlayer.connection.sendTCP(response);
                sendAvatar(waitingPlayer.connection, request.name);
                sendAvatar(waitingPlayer.connection, waitingPlayer.name);

                response.side = "b";
                connection.sendTCP(response);
                sendAvatar(connection, waitingPlayer.name);
                sendAvatar(connection, request.name);

                waitingPlayers.remove(waitingPlayer);
                isFoundNewGame = true;
                break;
            }
        }
        if(!isFoundNewGame){
            waitingPlayers.add(new WaitingPlayer(connection, request.userId, request.name, request.elo));
        }
    }
}
