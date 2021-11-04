package ArithmeticGame.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException {
        //Main function which accepts all connections, verifies they're 'legitimate' and assigns a thread to handle the connection
        //TODO: Threadpool limited to 10 for now but will be expanded/changed
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        try (ServerSocket serverSocket = new ServerSocket(4444)) {
            for (;;) {
                try {
                    //This accepts all connections so it's important to verify connections
                    Socket socket = serverSocket.accept();
                    System.out.println("Connection request from " + socket.getRemoteSocketAddress());
                    //the client connection constructor handles verification
                    ClientConnection client = new ClientConnection(socket);
                    //a thread is fired off to handle the verified connection
                    //TODO: Haven't tested what happens if a thread is not available.
                    // Might result in indefinite waiting which will need to be addressed
                    threadPool.execute(client);
                } catch (ClientConnectionException cce) {
                    //A client connection exception is thrown when a bad request is made either before or during the game's operation
                    System.out.println(cce.getMessage());
                    cce.printStackTrace();
                } catch (SocketException se) {
                    System.out.println("Error connecting with socket");
                    se.printStackTrace();
                }
            }
        } finally {
            threadPool.shutdown();
        }
    }
}
