package ArithmeticGame.Server;

import java.io.*;
import java.util.*;

class Game implements Runnable {
    //Game list is a static synchronized arraylist of the current games. Should work and be thread safe but could possibly
    //be improved for a larger number of games and clients.
    private static final List<Game> gameList =  Collections.synchronizedList(new ArrayList<>());
    // FIXME: game id is currently statically determined at runtime as a random integer and then incremented for each new game.
    //  should probably use a hash function or something to better create unique ids
    private static int currId = Math.abs(new Random(new Date().getTime()).nextInt());

    private boolean running;
    private int maxPlayers = 2,id;
    private Fraction target; //FIXME: target might be unnecessary as it is stored in the evaluator object. I'll leave it for now
    private String name;

    private Generator generator;
    private final Evaluator evaluator;
    private int[] currNumSet; //FIXME: Similarly currNumSet can also be stored in just the evaluator object

    private ArrayList<ClientConnection> players = new ArrayList<>(); //list of current players
    private final List<ClientConnection> playerQueue = Collections.synchronizedList(new ArrayList<>());
    //Players who join the game are placed in a queue and added at the end of the round. This helps prevent weird issues with joining mid game


    //Default constructor which is never used
    public Game(){
        generator = new Generator();
        evaluator = new Evaluator();
        target = evaluator.getTarget();
    }
    //The more general "Default Constructor" Assumes a target of 24, a range of 1-13, and 4 numbers
    //Might be used more if game settings can be toggled during the game
    public Game(ClientConnection hostPlayer, String name, int maxPlayers){
        this.name=name;
        this.maxPlayers= maxPlayers;

        players.add(hostPlayer);
        id = currId;
        currId++;

        generator = new Generator();
        evaluator = new Evaluator();
        target = evaluator.getTarget();

        running = true;

        gameList.add(this);
    }
    //Current main constructor
    //Will be used more if game settings are toggled before a game starts
    public Game(ClientConnection hostPlayer, String name, int maxPlayers, int rangeLow, int rangeHigh, int numNumbers, int target){
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.target = new Fraction(target);

        players.add(hostPlayer);
        id = currId;
        id++;

        generator = new Generator(rangeLow,rangeHigh,numNumbers);
        evaluator = new Evaluator();
        evaluator.setTarget(this.target);

        running = true;

        gameList.add(this);
    }
    //A struct basically, a private class used to store relevant info from a player's solution
    private static class PlayerSolution{
        String solution;
        long startTime, endTime;
        PlayerSolution(String solution, long startTime, long endTime){
            this.solution=solution;
            this.startTime=startTime;
            this.endTime=endTime;
        }
    }
    /*This function iterates through all connected players and checks if they have submitted an input
    If they have this input is evaluated and a response is sent
    Otherwise the function continues
    If the player has already submitted a correct value they are not polled again*/
    //TODO: Need to create a forfeit option for players to give up or some sort of timer
    private void pollSolutions(HashMap<ClientConnection,PlayerSolution> playerSolutions,boolean solvable){
        //playerSolutions maps players to their submitted solutions
        for (ClientConnection player : players) {
            if (!playerSolutions.containsKey(player)) {
                //Only polls player if their solutions has not already been submitted
                try {
                    String inputString = player.read(10);
                    //only checks for an input 10 milliseconds at a time so as not to block the thread and
                    // to fairly accurately get the first player
                    if (inputString.startsWith("ANSWER")) {
                        //Input format ANSWER {SOLUTION_STRING}
                        String[] parameters = inputString.replaceFirst("ANSWER", "").trim().split("\\|");
                        String solutionString = parameters[0].trim();
                        long startTime = Long.parseLong(parameters[1].trim());
                        long endTime = Long.parseLong(parameters[2].trim());

                        String outString;
                        //Output string is built to either start with CORRECT or INCORRECT as well as an output message
                        boolean correct = false;

                        if (solutionString.trim().toUpperCase().equals("NO ANSWER")) {
                            //Handles NO ANSWER case
                            if (solvable)
                                outString = Arrays.toString(currNumSet) + " is solvable";
                            else {
                                outString = "Correct! " + Arrays.toString(currNumSet) + " is unsolvable";
                                correct = true;
                            }
                        } else {
                            //Otherwise string is passed to the evaluator and checked for correctness
                            try {
                                Fraction solution = evaluator.evaluate(solutionString);
                                outString = solutionString + "=" + solution;
                                if (evaluator.isOnTarget()) {
                                    correct = evaluator.isNumSetCompliant();
                                    outString += " is the right answer";
                                    if (!correct) {
                                        outString += " but you didn't use the right numbers! -> " + Arrays.toString(evaluator.numSet);
                                    }
                                } else
                                    outString += " is the wrong answer";
                            }catch(IllegalArgumentException e){
                                //Evaluator throws illegal argument exception if there is an error during parsing which is considered incorrect
                                outString = e.getMessage();
                                correct = false;
                            }
                        }
                        //Informs the player if they were correct or incorrect
                        if (correct) {
                            outString = "CORRECT " + outString;
                            playerSolutions.put(player, new PlayerSolution(solutionString, startTime, endTime));
                            System.out.println("Player " + players.indexOf(player) + " found the correct solution at " + endTime);
                        }
                        else
                            outString = "INCORRECT " + outString;
                        player.write(outString);
                        }

                } catch (IOException e) {
                    //e.printStackTrace();
                    //This exception is thrown whenever the read fails due to timeout so no need to print anything.
                    //Not sure if anything else should be done though
                }
            }
        }
    }

    //Actually runs games
    public void run() {
        System.out.println("Game started");
        /*Game loop structure is as follows:
        1. Send Keep alives to each player. Remove unresponsive players and end game if no players are left
        2. Generate new number set and update evaluator accordingly
        3. Send number set to each player
        4. Poll players for solutions until at least one player finds one
        5. Wait for stragglers and poll again
        6. Inform all players that the round has ended
        7. Determine winner from submitted solutions based on time spent
        8. Poll all players to see if they want to continue or not
        9. Remove players who say no or are unresponsive and end game if no players are left
        10. Go to step 1
        FIXME: If all the players forcefully leave during step 4 the game never ends and is never properly deleted
         which results in players who try to join also getting soft locked*/
        while(running){
            {
                //Handles KEEP ALIVE. Removes all players who don't respond

                //It seemed easier to create a list of 'living players' rather than update the current arraylist as that seemed messy
                ArrayList<ClientConnection> livingPlayers = new ArrayList<>();
                System.out.println("Sending keep alives");
                for (ClientConnection player : players) {
                    try {
                        if (player.keepAlive())
                            livingPlayers.add(player);
                    } catch (IOException e) {
                        //  e.printStackTrace();
                    }
                }
                players = livingPlayers;
                //This ends game if there are no more living players
                if(players.isEmpty())
                    break;

                for(int i = 0; i < players.size(); i++){
                    System.out.println("PLAYER " + i + " alive");
                }
            }
            //Generates number set and updates evaluator
            currNumSet = generator.generate();
            evaluator.setNumSet(currNumSet);

            //Sends each player the number set
            for(ClientConnection player: players){
                player.write("NUMSET " +Arrays.toString(currNumSet));
            }
            //Determines if the number set is solvable
            boolean solvable = Solver.solve(currNumSet, target);

            //Polls players for solutions. See pollSolutions for more details
            HashMap<ClientConnection, PlayerSolution> playerSolutions = new HashMap<>();
            while (playerSolutions.isEmpty()) {
                pollSolutions(playerSolutions, solvable);
            }

            try {
                //Sleeps for 3 seconds to let other solutions come in
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("Sleep interuppted");
                //FIXME: Not really sure what to do if this ever gets reached
            }
            pollSolutions(playerSolutions, solvable); //Polls players one more time

            //Tells all players that the game is over
            for(ClientConnection player:players){
                player.write("END ROUND");
            }
            //Checks all submitted solutions for the minimal time
            long minTime = Long.MAX_VALUE;
            ClientConnection winner = null;
            for(ClientConnection player :playerSolutions.keySet()){
                PlayerSolution solution = playerSolutions.get(player);
                long solutionTime = solution.endTime-solution.startTime;
                if(minTime>solutionTime){
                    minTime = solutionTime;
                    winner = player;
                }
            }
            //Sends winner to every player
            for(ClientConnection player:players){
                player.write("WINNER " + players.indexOf(winner));
            }
            System.out.println("Player " + players.indexOf(winner) + " wins!");
            //Sends continue query to every player
            for(ClientConnection player: players){
                player.write("CONTINUE?");
            }
            boolean[] continues = new boolean[players.size()]; //Stores whether or not a player wants to continue
            //Following code gives players 20 seconds to opt into continuing to the next round
            long startTime = new Date().getTime();
            while(new Date().getTime()-startTime<20000){ //20 seconds seems to be a decent amount of time. Might need adjusting
                for (int i = 0; i < continues.length; i++) {
                    try {
                        String continueResponse = players.get(i).read(10);
                        //Response will either be CONTINUE YES or CONTINUE NO
                        if(continueResponse.replace("CONTINUE","").trim().equals("YES"))
                            continues[i]=true;
                        //TODO: Maybe add functionality for CONTINUE NO
                    } catch (IOException e) {
                        // e.printStackTrace();
                    }
                }
            }
            //Removes all players who are not continue sending GAME CONTINUE to those who are and GAME END to those who aren't
            ArrayList<ClientConnection> livingPlayers = new ArrayList<>();
            for(int i = 0;i < continues.length; i++){
                ClientConnection player = players.get(i);
                if(continues[i]) {
                    livingPlayers.add(player);
                    player.write("GAME CONTINUE");
                }else {
                    player.write("GAME END");
                    synchronized (player) {
                        player.notify();
                    }
                }

            }
            players = livingPlayers;

            //Adds all players waiting in queue into the next round
            while (!playerQueue.isEmpty()) {
                players.add(playerQueue.remove(0));
                System.out.println("Player " + players.size() + " has joined the game");
            }
            System.out.println("There are " +players.size() + " players in this lobby");
            //Ends game if there are no players
            if(players.isEmpty())
                running = false;
        }
        gameList.remove(this);
    }
    //Simple join function which adds players to the queue
    public boolean join(ClientConnection player){
        if(players.size()+playerQueue.size()<maxPlayers){
            playerQueue.add(player);
            return true;
        }
        return false;
    }
    //Get functions
    public static Game[] getGameList(){return gameList.toArray(new Game[0]);}
    public String getName(){return name;}
    public int getMaxPlayers(){return maxPlayers;}
    public int getNumPlayers(){return players.size();}
    public int getId(){return id;}

    //Set generator functions might be useless b/c all changes are done internally atm
    /*public void setGenerator(int low, int high){
        generator = new Generator(low,high);
    }
    public void setGenerator(int low, int high, int size){
        generator = new Generator(low,high,size);
    }*/
}
