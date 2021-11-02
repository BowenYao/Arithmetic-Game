package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws IOException {
       // System.out.println(Solver.solve(new int[]{8,5,9,11,1,12},24));
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        ServerSocket serverSocket = new ServerSocket(80);
        try{
            for(;;){
                try {
                    Socket socket = serverSocket.accept();
                  /*  SocketAddress remoteAddress = socket.getRemoteSocketAddress();
                    String address;
                    if(remoteAddress==null)
                        address = "NULL";
                    address = remoteAddress.*/
                    System.out.println("Connection request from " + socket.getRemoteSocketAddress());
                    if (Validator.initialize(socket)) {
                        Game game = new Game(socket);
                        threadPool.execute(game);
                    }
                }catch(SocketException se){
                    System.out.println("Error connecting with socket");
                    se.printStackTrace();
                }
            }
        }finally{
            threadPool.shutdown();
            serverSocket.close();
        }
	// write your code here
        /*File file = new File("C:\\Users\\boboa\\For Fun\\24Game\\24GameJava\\src\\com\\company\\solutions.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        Stream<String> stream = br.lines();
        stream.map(line-> {String[] stringArr = line.replaceAll("[]\\[]","").split(",");
            int[] intArr = new int[stringArr.length];
            for(int i = 0; i < stringArr.length; i++){
                intArr[i] = Integer.parseInt(stringArr[i].strip());
            }
            return intArr;
        }).forEach(ints-> {
            if (!Solver.solve(ints, 24))
                System.out.println("Missing set");
        });*/
        //System.out.println(Solver.solve(new int[]{3,3,8,8},24));

    }
}
