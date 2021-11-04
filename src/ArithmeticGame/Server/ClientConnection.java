package ArithmeticGame.Server;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Random;

public class ClientConnection implements Runnable {
    //TODO: Encoding can possibly be negotiated instead of hard-coded to a single encoding
    private final static String ENCODING = "ISO-8859-1";

    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    public ClientConnection(Socket socket) throws IOException, ClientConnectionException {
        //Constructor
        this.socket = socket;

        InputStream in = socket.getInputStream();
        reader = new BufferedReader(new InputStreamReader(in,ENCODING));

        OutputStream out = socket.getOutputStream();
        out = new BufferedOutputStream(out);
        writer = new PrintWriter(new OutputStreamWriter(out),true);

        {
            //HANDSHAKE FUNCTION
            //Connection is verified by sending two random integers and expecting the product within a second
            socket.setSoTimeout(1000);
            Random random = new Random(new Date().getTime());
            int a = random.nextInt(), b = random.nextInt();
            writer.println(a + "|" + b);
            try {
                int response = Integer.parseInt(reader.readLine());
                if (response != a * b) {
                    System.out.println("CONNECTION REQUEST DENIED: INVALID RESPONSE");
                    writer.println("BAD REQUEST");
                    writer.close();
                    throw new ClientConnectionException("Bad connection request");
                }
                System.out.println("Connection Established");
                //Ready response sent to help synchronize client and server
                writer.println("READY");
            } catch (NumberFormatException nfe) {
                System.out.println("CONNECTION REQUEST DENIED: INVALID RESPONSE");
                throw new ClientConnectionException("Bad connection request");
            } catch (SocketTimeoutException ste) {
                System.out.println("CONNECTION REQUEST DENIED: CONNECTION TIMED OUT");
                throw new ClientConnectionException("Bad connection request");
            } finally{
                socket.setSoTimeout(0);
            }
        }
    }
    //read and write functions exposed so that the Game class can send and receive messages from specific clients/players
    public String read() throws IOException {
        return reader.readLine();
    }
    public String read(int timeout) throws IOException {
        //this read only waits {timeout} milliseconds and throws a SocketTimeoutException if it fails
        socket.setSoTimeout(timeout);
        try {
            return reader.readLine();
        }finally {
            //it's important to reset the Socket Timeout or else future reads to this socket will be affected
            socket.setSoTimeout(0);
        }
    }
    public void write(String message){
        writer.println(message);
    }

    //Keep alive function simply sends a KEEP ALIVE message and
    // periodically checks within a certain amount for a KEEP ALIVE response
    public boolean keepAlive() throws IOException {
        System.out.println("KEEP ALIVE SENT");
        writer.println("KEEP ALIVE");
        //Currently keepAlive checks every 10th of a second for one second
        //More testing should be done to see if this is enough time for various latencies
        socket.setSoTimeout(100);
        for(int i = 0; i < 10; i++){
            try {
                String input = reader.readLine();
                System.out.println(input);
                if (input.equals("KEEP ALIVE")) {
                    System.out.println("KEEP ALIVE RECEIVED");
                    socket.setSoTimeout(0);
                    return true;
                }
            }catch(SocketTimeoutException ste){
                System.out.println("Keep alive miss");
            }
        }
        socket.setSoTimeout(0);
        return false;
    }
    @Override
    public void run() {
        //This run handles all client interactions WHEN OUTSIDE OF GAME.
        //Namely creating or joining a game and checking for what games are available
        boolean running = true;
        try {
            //Following loop receives input from the client and then acts based on that input
            while (running) {

                String inputString = reader.readLine().trim();
                System.out.println("Response received: " + inputString);
                if(inputString.equals("GAMES LIST")){
                    //Sends all currently running games for the client homepage games list
                    Game[] gamesList = Game.getGameList();

                    //Output is formatted GAMES: game1Id:game1Name    (game1Players/game1MaxPlayers)|game2Id:game2Name    game2Players/game2MaxPlayers)|...
                    StringBuilder outputString = new StringBuilder("GAMES ");
                    for(Game game:gamesList){
                        outputString.append(game.getId()).append(":").append(game.getName()).append("\t(").append(game.getNumPlayers()).append("/").append(game.getMaxPlayers()).append(")|");
                    }
                    outputString.deleteCharAt(outputString.length()-1);   //Remove trailing |
                    writer.println(outputString);
                    System.out.println("GAMES LIST SENT");
                }else if(inputString.startsWith("JOIN GAME")){
                    //Attempts to join an existing game
                    //Input string is formatted JOIN GAME {GAME_ID}
                    int gameId = Integer.parseInt(inputString.replace("JOIN GAME","").trim());
                    System.out.println("JOIN REQUEST for game " + gameId);

                    Game[] gamesList = Game.getGameList(); //Reminder that Game.getGameList() gets a copy of the gameList so it's thread-safe but not always accurate
                    /*The following goes through all the games in the games list and
                    tries to join the matching id. Due to the client list refresh being
                    rather rare and concurrency issues a game might no longer exist and the join can fail*/
                    boolean join = false;
                    for(Game game:gamesList){
                        if(game.getId()==gameId){
                            //JOIN GAME
                            join = true;
                            if(game.join(this)){
                                writer.println("JOIN SUCCESS");
                                try {
                                    /*Both join and host perform a wait on the current ClientConnection object
                                    This means the thread handling this client falls asleep and is given control
                                    to the 'host thread' of the game. When a player leaves notify is called
                                    and this thread wakes up again and resumes its previous activity */
                                    synchronized (this) {
                                        this.wait();
                                    }
                                }catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    //FIXME: Current system doesn't differentiate between a game not existing and being full.
                    // Should be an easy fix
                    if(!join) {
                        //If join fails the client is informed and regular activity resumes
                        System.out.println("Game " + gameId + " no longer exists");
                        writer.println("GAME ERROR: Game " + gameId + " no longer exists");
                    }
                }else if(inputString.startsWith("HOST GAME")){
                    //Starts a new game
                    //Input string format is HOST GAME {GAME_NAME}|{NUM_NUMBERS}|{RANGE_LOW_NUM}|{RANGE_HIGH_NUM}|{NUM_PLAYERS}|{TARGET_NUMBER}
                    String[] parameters = inputString.replace("HOST GAME","").trim().split("\\|");
                    String name = parameters[0];
                    int numNumbers = Integer.parseInt(parameters[1].trim());
                    int rangeLow = Integer.parseInt(parameters[2].trim());
                    int rangeHigh = Integer.parseInt(parameters[3].trim());
                    int numPlayers = Integer.parseInt(parameters[4].trim());
                    int target = Integer.parseInt(parameters[5].trim());
                    Game game = new Game(this,name,numPlayers,rangeLow,rangeHigh,numNumbers,target);
                    System.out.println("Running game: " + name);

                    //Since this creates a new game a new thread is spawned to run the game
                    //FIXME: I think these threads should maybe be spawned from the threadpool.
                    // I can't picture how this would be exploited but it might be
                    Thread thread = new Thread(game);
                    thread.start();
                    try{
                        /*Both join and host perform a wait on the current ClientConnection object
                        This means the thread handling this client falls asleep and is given control
                        to the 'host thread' of the game. When a player leaves notify is called
                        and this thread wakes up again and resumes its previous activity */
                        synchronized (this) {
                            this.wait();
                        }
                    }catch(InterruptedException e){
                        //FIXME: haven't done enough testing to know what else to do when this exception is called
                        e.printStackTrace();
                    }
                }else if(inputString.startsWith("EXIT")){
                    //TODO: There is currently no way to reach this statement
                    int status = Integer.parseInt(inputString.replace("EXIT","").trim());
                    System.out.println("Client connection terminated with status "+ status);
                    writer.close();
                    socket.close();
                    running = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
