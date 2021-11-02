package com.company;

import com.company.Evaluator;
import com.company.Fraction;
import com.company.Generator;
import com.company.Solver;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class Game implements Runnable {
    private final static String ENCODING = "ISO-8859-1";
    private boolean running;
    private Fraction target;
    private Generator generator;
    private Evaluator evaluator;
    private Socket socket;
    int[] currNumset;
    public Game(){
        generator = new Generator();
        evaluator = new Evaluator();
        target = evaluator.getTarget();
    }
    public Game(Socket socket){
        this.socket = socket;
        generator = new Generator();
        evaluator = new Evaluator();
        target = evaluator.getTarget();
    }
    public void run(){
        InputStream in = null;
        try {
            in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, ENCODING));
            out = new BufferedOutputStream(out);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out),true);
            running = true;
            System.out.println("Connection established");
            writer.println("READY");
            while(running){
                String inputString = reader.readLine();
                System.out.println("Response received: " + inputString);
                if(inputString.startsWith("GENERATE")){
                    System.out.println("Generate command received");
                    String[] parameters = inputString.replace("GENERATE","").trim().split(" ");
                    System.out.println(Arrays.toString(parameters) + " " + parameters.length);
                    if(parameters.length>0 && !parameters[0].equals("")){
                       int low = Integer.parseInt(parameters[0]);
                       int high = Integer.parseInt(parameters[1]);
                       int size = generator.getSize();
                       if(parameters.length>2)
                           size = Integer.parseInt(parameters[2]);
                       generator = new Generator(low,high,size);
                    }
                    currNumset = generator.generate();
                    Arrays.sort(currNumset);
                    evaluator.setNumset(currNumset);
                    writer.println("NUMSET " + Arrays.toString(currNumset));
                    System.out.println("NUMSET command sent");
                }else if(inputString.startsWith("ANSWER")){
                    String[] parameters = inputString.replaceFirst("ANSWER","").trim().split("\\|");
                    String solutionString = parameters[0].trim();
                    long startTime = Long.parseLong(parameters[1].trim());
                    long endTime = Long.parseLong(parameters[2].trim());
                    long timeSpent = endTime-startTime;
                    boolean solvable = Solver.solve(currNumset,target);
                    String outString;
                    System.out.println(solutionString);
                    if(solutionString.equals("NO ANSWER")){
                        if(solvable)
                            outString = Arrays.toString(currNumset) + " is solvable";
                        else
                            outString = "Correct! " + Arrays.toString(currNumset) + " is unsolvable";

                    }else {
                        Fraction solution = evaluator.evaluate(solutionString);
                        outString = solutionString + "=" + solution;
                        if (evaluator.isOnTarget()) {
                            outString += " is the right answer";
                            if (!evaluator.isNumSetCompliant()) {
                                outString += " but you didn't use the right numbers!";
                            }
                        } else
                            outString += " is the wrong answer";
                    }
                    outString+="\tSolution found in " + timeSpent + " milliseconds";
                    writer.println(outString);
                   // writer.println("Solution found in " + timeSpent + " milliseconds");
                }else if(inputString.equals("STOP")){
                    writer.close();
                    socket.close();
                    running = false;
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
            System.out.println("Game ended unexpectedly");
        }
    }
    public void setGenerator(int low, int high){
        generator = new Generator(low,high);
    }
    public void setGenerator(int low, int high, int size){
        generator = new Generator(low,high,size);
    }
}
