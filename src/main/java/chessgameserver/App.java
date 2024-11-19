package chessgameserver;

import java.io.IOException;

import chessgameserver.network.MainServer;
import chessgameserver.network.database.DatabaseConnection;

public class App 
{
    public static void main( String[] args ) throws IOException
    {
        DatabaseConnection.DatabaseConnectionInit();
        MainServer server = new MainServer(0, 0);
        server.run();
        System.out.println( "Hello World!" );
    }
}
