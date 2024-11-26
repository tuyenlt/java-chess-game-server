package chessgameserver.network.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import chessgameserver.network.packets.*;
import chessgameserver.network.packets.GeneralPackets.*;

public class DatabaseConnection {

    // Phương thức kết nối cơ sở dữ liệu
    private static Connection connection;

    public static void DatabaseConnectionInit() {
        connectToDatabase();
    }

    private static void connectToDatabase() {
        try {
            
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:ChessGame.db"; 
            connection = DriverManager.getConnection(url);
            System.out.println("Kết nối cơ sở dữ liệu SQLite thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi kết nối cơ sở dữ liệu SQLite: " + e.getMessage());
        }
    }

    public static LoginResponse loginAuthentication(LoginRequest loginRequest) throws Exception {
        boolean isUserExits = false;
        boolean isPasswordCorrect = false;
        
        LoginResponse user = new LoginResponse();
        // Kiểm tra input hợp lệ
        if (loginRequest.userName == null || loginRequest.userName.isEmpty()) {
            throw new Exception("Tên đăng nhập không được để trống");
        }
        if (loginRequest.passwd == null || loginRequest.passwd.isEmpty()) {
            throw new Exception("Mật khẩu không được để trống");
        }

        String query = "SELECT * FROM Users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, loginRequest.userName);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                isUserExits = true;
                String dbPassword = resultSet.getString("password");
                isPasswordCorrect = dbPassword.equals(loginRequest.passwd);

                if (isPasswordCorrect) {
                    user.userId = resultSet.getInt("id");
                    user.userName = resultSet.getString("username");
                    // user.passwd = resultSet.getString("password");
                    user.elo = resultSet.getInt("elo");
                    user.win = resultSet.getInt("win");
                    user.lose = resultSet.getInt("lose");
                    user.draw = resultSet.getInt("draw");
                }
            }
            user.isSuccess = true;
            user.message = "Login Success";
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Lỗi khi truy vấn cơ sở dữ liệu: " + e.getMessage());
        }
        
        if (!isUserExits) {   
            throw new Exception("User không tồn tại");
        }
        if (!isPasswordCorrect) {
            throw new Exception("Sai mật khẩu, vui lòng thử lại");
        }
        
        return user;
    }

    public static RegisterResponse registerNewUser(RegisterRequest registerRequest) throws Exception {
        boolean isUserNameExist = false;
        RegisterResponse response = new RegisterResponse();
        response.isSuccess = false;
        // Kiểm tra input hợp lệ
        if (registerRequest.userName == null || registerRequest.userName.isEmpty()) {
            throw new Exception("Tên đăng nhập không được để trống");
        }
        if (registerRequest.password == null || registerRequest.password.isEmpty()) {
            throw new Exception("Mật khẩu không được để trống");
        }

        String query = "SELECT * FROM Users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, registerRequest.userName);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                isUserNameExist = true;
            }
        }

        if (isUserNameExist) {
            response.message = "User name already exists";
        } else {
            String insertQuery = "INSERT INTO Users (username, password, elo) VALUES (?, ?, ?)";
            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                insertStatement.setString(1, registerRequest.userName);
                insertStatement.setString(2, registerRequest.password);
                insertStatement.setInt(3, 500);
                insertStatement.executeUpdate();
                response.message = "Create new account success, go back to login";
            }
        }
        response.isSuccess = true;
        return response;
    }

    public static RankingListResponse getRankingList(RankingListRequest rankingListRequest) throws Exception {
        RankingListResponse rankingListResponse = new RankingListResponse();
        String query = "SELECT username, elo FROM Rank ORDER BY elo DESC";
        System.out.println("pass");
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String userName = resultSet.getString("username");
                int elo = resultSet.getInt("elo");
                rankingListResponse.addUserRankToList(userName, elo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error fetching ranking list: " + e.getMessage());
        }

        return rankingListResponse;
    }

    public static synchronized HistoryGameResponse getHistoryGame(HistoryGameRequest gameRequest) throws Exception {
        HistoryGameResponse response;
        String query = "SELECT player_id, opponent_id, moves, result FROM HistoryGame WHERE matchid = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, Long.parseLong(gameRequest.gameId));
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int playerId = resultSet.getInt("player_id");
                int opponentId = resultSet.getInt("opponent_id");
                String moves = resultSet.getString("moves");
                String result = resultSet.getString("result");
                response = new HistoryGameResponse(playerId, opponentId, moves, result);

            } else {
                throw new Exception("Game not found with ID: " + gameRequest.gameId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error fetching game history: " + e.getMessage());
        }

        return response;
    }

    public static synchronized ProfileViewResponse getProfile(ProfileViewRequest request) throws Exception {
        ProfileViewResponse response = new ProfileViewResponse();
        String query = "SELECT id, username, email, elo, win, lose, draw FROM Users WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, request.userId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                response.userName = resultSet.getString("username");
                response.elo = resultSet.getInt("elo");
                response.win = resultSet.getInt("win");
                response.lose = resultSet.getInt("lose");
                response.draw = resultSet.getInt("draw");
            } else {
                throw new Exception("User not found with ID: " + request.userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error fetching profile: " + e.getMessage());
        }

        return response;
    }

    public static PlayerData getPlayerInfoById(int id) throws Exception{
        String query = "SELECT id, username, elo, win, lose, draw FROM Users WHERE id = ?";        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                PlayerData player = new PlayerData(
                    resultSet.getInt("id"),
                    resultSet.getString("username"),
                    resultSet.getInt("elo"),
                    resultSet.getInt("win"),
                    resultSet.getInt("lose"),
                    resultSet.getInt("draw")
                    );
                return player;
            } else {
                throw new Exception("User not found with ID: " + id);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error fetching profile: " + e.getMessage());
        }
    }

    public static void updatePlayerToDB(PlayerData player) throws Exception{
        String query = "UPDATE Users SET elo = ?, win = ?, lose = ?, draw = ? WHERE id = ?";
        try(PreparedStatement statement = connection.prepareStatement(query)){
            statement.setInt(1, player.elo);
            statement.setInt(2, player.win);
            statement.setInt(3, player.lose);
            statement.setInt(4, player.draw);
            statement.setInt(5, player.playerId);
            statement.executeUpdate();
        }
    }
}
