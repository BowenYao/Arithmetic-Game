package com.company;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class Validator {
    private final static String ENCODING = "ISO-8859-1";

    public static boolean initialize(Socket socket) throws IOException {
        socket.setSoTimeout(1000);
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, ENCODING));
        out = new BufferedOutputStream(out);
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out),true);
        Random random = new Random(new Date().getTime());
        int a = random.nextInt(), b = random.nextInt();
        writer.println(a + "|" + b);
        try {
            int response = Integer.parseInt(reader.readLine());
            if (response != a * b) {
                System.out.println("CONNECTION REQUEST DENIED: INVALID RESPONSE");
                writer.println("BAD REQUEST");
                writer.close();
                socket.close();
                return false;
            }
            socket.setSoTimeout(0);
            return true;
        }catch(NumberFormatException nfe){
            System.out.println("CONNECTION REQUEST DENIED: INVALID RESPONSE");
            return false;
        }
        catch(SocketTimeoutException ste){
            System.out.println("CONNECTION REQUEST DENIED: CONNECTION TIMED OUT");
            return false;
        }
    }
}

