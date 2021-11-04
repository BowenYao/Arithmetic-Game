package ArithmeticGame.Client;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        //Creates main frame that game will be run on
        JFrame frame = new JFrame();
        frame.setLayout(null);
        frame.setTitle("Arithmetic Game");
        frame.setMinimumSize(new Dimension(600,400));
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        GUI gui = new GUI(frame); //Creates gui which runs the game
    }
}

