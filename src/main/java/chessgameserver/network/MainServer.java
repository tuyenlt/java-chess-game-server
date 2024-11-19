package chessgameserver.network;
import java.io.IOException;
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

        server = new Server();
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

            }
        });
    }

    private void handleLogin(Connection connection, Object object){
        LoginRequest request =  (LoginRequest)object;
        try{
            LoginResponse response = DatabaseConnection.loginAuthentication(request);
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
            HistoryGameResponse response = DatabaseConnection.getHistoryGame(request);
            connection.sendTCP(response);
        } catch (Exception ex) {
            // TODO: handle exception
        }
    }
    
    private void handleWatingPlayer(Connection connection, Object object){
        FindGameRequest request = (FindGameRequest)object;
        boolean isFoundNewGame = false;
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
                waitingPlayer.connection.sendTCP(response);
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
