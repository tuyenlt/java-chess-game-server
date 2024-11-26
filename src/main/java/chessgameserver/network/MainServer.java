package chessgameserver.network;
import java.io.File;
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

@SuppressWarnings("unused")
public class MainServer {
    private Server server;
    private int tcpPort;
    private int udpPort;
    private ArrayList<WaitingPlayer> waitingPlayers = new ArrayList<>();
    
    class WaitingPlayer{
        public Connection connection;
        public int elo;
        public int playerId;

        public WaitingPlayer(Connection connection,int playerId,int elo) {
            this.connection = connection;
            this.playerId = playerId;
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

                if(object instanceof ImageUpload){
                    handleImageUpload(connection, object);
                }
            }
        });
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

    private void handleLogin(Connection connection, Object object){
        LoginRequest request =  (LoginRequest)object;
        try{
            LoginResponse response = DatabaseConnection.loginAuthentication(request);
            File file = new File("uploaded-images/" + response.userId + ".png");
            if(!file.exists()){
                file = new File("uploaded-images/avatar-holder.jpg");
            }
            ImageUpload imageUpload = new ImageUpload();
            imageUpload.fileName = String.valueOf(response.userId);
            imageUpload.imageData = Files.readAllBytes(file.toPath());

            connection.sendTCP(imageUpload);
            connection.sendTCP(response);
        }catch(Exception error){ 
            LoginResponse response = new LoginResponse();
            response.isSuccess = false;
            response.message = error.getMessage();
            connection.sendTCP(response);
        }    
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

    public void handleImageUpload(Connection connection, Object object){
        ImageUpload upload = (ImageUpload)object;
        try {
            Path folderPath = Paths.get("uploaded-images");
            Files.createDirectories(folderPath);

            Path filePath = folderPath.resolve(upload.fileName);
            Files.write(filePath, upload.imageData);


            System.out.println("Image " + upload.fileName + " uploaded and saved at " + filePath);
            connection.sendTCP("Image uploaded successfully");
        } catch (IOException e) {
            e.printStackTrace();
            connection.sendTCP("Failed to save the image");
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
            HistoryGameResponse response = DatabaseConnection.getHistoryGame(request);
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
            if(Math.abs(waitingPlayer.elo - request.elo) <= 200){
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
                response.side = "b";
                connection.sendTCP(response);
                waitingPlayers.remove(waitingPlayer);
                isFoundNewGame = true;
                break;
            }
        }
        if(!isFoundNewGame){
            waitingPlayers.add(new WaitingPlayer(connection, request.userId, request.elo));
        }
    }
}
