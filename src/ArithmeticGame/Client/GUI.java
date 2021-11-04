package ArithmeticGame.Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;


public class GUI {

    private final JFrame continueFrame;
    JPanel introPanel,gamePanel;
    JTextField inputBox;
    JLabel numberLabel, inputText,outputLabel;
    JList<String> gameList;

    Socket socket;
    BufferedReader reader;
    PrintWriter writer;
    private static final String ip = "your_ip_here";
    private static final int port = 4444;
    private final static String ENCODING = "ISO-8859-1";
    private int[] gameIds; //maintains ids of games on the game list

    boolean inputActivated = false; //boolean flag to prevent inputs being sent to the server at the wrong time
    long startTime,endTime;

    Runnable runnable = this::runGame; //Runnable to pass to a thread for running a game

    public GUI(JFrame frame) throws IOException {
        //Tries to connect to server and perform intial handshake
        try{
            socket = new Socket(ip,port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),ENCODING));
            writer = new PrintWriter(socket.getOutputStream(),true);
            handshake(socket,reader,writer);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //Following code deals with intro panel/home page
        //Intro panel covers entire frame and swaps with game panel
        {

            introPanel = new JPanel();
            introPanel.setLocation(0,0);
            introPanel.setLayout(null);
            introPanel.setSize(frame.getWidth(), frame.getHeight());
            frame.add(introPanel);

            //gameList keeps track of all running games so that players can join
            gameList = new JList<>();
            gameList.setSize(300,200);
            gameList.setLocation(introPanel.getWidth()/2-gameList.getWidth()/2,introPanel.getHeight()/2-gameList.getHeight()/2-50);
            gameList.setFixedCellWidth(300);
            refreshGamesList();
            introPanel.add(gameList);

            //joinButton attempts to join selected game in gameList
            JButton joinButton = new JButton("Join Game");
            joinButton.setSize(joinButton.getPreferredSize());
            joinButton.setLocation(introPanel.getWidth()/2-joinButton.getWidth()/2-50,275);
            class JoinButtonListener implements ActionListener{
                @Override
                public void actionPerformed(ActionEvent e) {
                    //function triggered whenever joinButton is pressed
                    if(gameList.isSelectionEmpty()){
                        //Creates error dialog if user has no game selected
                        JOptionPane optionPane = new JOptionPane("No game is selected",JOptionPane.WARNING_MESSAGE);
                        JDialog dialog = optionPane.createDialog("Error");
                        dialog.setVisible(true);
                    }else{
                        //Attempts to join selected game
                        int gameId = gameIds[gameList.getSelectedIndex()];
                        writer.println("JOIN GAME " + gameId); //Sends join game request to server
                        try {
                            String response = reader.readLine();
                            if(response.startsWith("GAME ERROR:")){
                                //Error format is GAME ERROR:{ERROR_MESSAGE}
                                //Opens error dialog box showing why game could not be connected to
                                JOptionPane optionPane = new JOptionPane("There was an error connecting to " +gameList.getSelectedValue() + " because:\n"
                                        +response.replace("GAME ERROR:","").trim(),JOptionPane.WARNING_MESSAGE);
                                JDialog dialog = optionPane.createDialog("Error");
                                dialog.setVisible(true);
                                refreshGamesList();
                            }
                            else {
                                //FIXME: Success message is JOIN SUCCESS should handle that specifically and throw error if something else is received
                                //Joins game
                                introPanel.setVisible(false);
                                gamePanel.setVisible(true);
                                changeLabelText(outputLabel, gamePanel, "Waiting for the next round to start...");

                                //Game logic is handed off to a separate thread
                                // FIXME: Might result in issues if client somehow opens multiple games at once
                                Thread thread = new Thread(runnable);
                                thread.start();
                            }
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                }
            }
            joinButton.addActionListener(new JoinButtonListener());
            introPanel.add(joinButton);

            //hostButton creates new game
            JButton hostButton = new JButton("Host Game");
            hostButton.setSize(hostButton.getPreferredSize());
            hostButton.setLocation(introPanel.getWidth()/2-joinButton.getWidth()/2+50,275);
            class HostButtonListener implements ActionListener{
                @Override
                public void actionPerformed(ActionEvent e) {
                    //function triggered when hostButton is pushed
                    writer.println("HOST GAME Bowen's Game|4|1|13|2|24");
                    //TODO: Currently all games have default settings. Expand to maybe add a settings menu
                    introPanel.setVisible(false);
                    gamePanel.setVisible(true);

                    //Again game logic is handed off to a separate thread
                    Thread thread = new Thread(runnable);
                    thread.start();
                }
            }
            hostButton.addActionListener(new HostButtonListener());
            introPanel.add(hostButton);
        }
        //Following code deals with the game panel only
        //Game panel covers entire frame and switches with the intro panel
        {
            gamePanel = new JPanel();
            gamePanel.setLocation(0, 0);
            gamePanel.setLayout(null);
            gamePanel.setSize(frame.getWidth(), frame.getHeight());
            gamePanel.setVisible(false);
            frame.add(gamePanel);

            //numberLabel displays the number set
            numberLabel = new JLabel();
            gamePanel.add(numberLabel);
            numberLabel.setLocation(0, 20);

            //inputText displays the words Input your solution only for now
            inputText = new JLabel("Input your solution");
            gamePanel.add(inputText);
            inputText.setSize(inputText.getPreferredSize());
            inputText.setLocation(gamePanel.getWidth() / 2 - inputText.getWidth() / 2, 80);

            //Input box is the text box field which takes user input
            inputBox = new JTextField();
            gamePanel.add(inputBox);
            inputBox.setSize(150, 20);
            inputBox.setLocation(gamePanel.getWidth() / 2 - inputBox.getWidth() / 2, 100);
            inputBox.setHorizontalAlignment(JTextField.CENTER);
            class InputBoxListener implements ActionListener{
                @Override
                public void actionPerformed(ActionEvent e) {
                    //Function triggers whenever enter is pressed with the input box selected
                    String inputString = e.getActionCommand();
                    if(inputActivated){
                        //deactivates and resets input box
                        inputActivated = false;
                        inputBox.setText("");

                        //calculates end time and sends solution string to server
                        endTime = new Date().getTime();
                        writer.println("ANSWER "+ inputString.trim() + " | " + startTime + " | " + endTime);
                    }
                }
            }
            inputBox.addActionListener(new InputBoxListener());

            //outputLabel displays various instructions and whether or not user solution was correct
            outputLabel = new JLabel();
            outputLabel.setSize(outputLabel.getPreferredSize());
            outputLabel.setLocation(gamePanel.getWidth() / 2 - outputLabel.getWidth() / 2, 140);
            gamePanel.add(outputLabel);
        }
        //Following code deals with continue pop-up
        //Continue frame pops up at the end of each round to ask user if they want to keep playing
        {

            continueFrame = new JFrame();
            continueFrame.setLayout(null);
            continueFrame.setTitle("Continue?");
            continueFrame.setMinimumSize(new Dimension(400,200));
            continueFrame.setResizable(false);
            continueFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            //Closing the window prematurely would cause errors FIXME: Maybe look into some sort of default behavior
            continueFrame.setLocationRelativeTo(null);

            //continueYes button continues game
            JButton continueYes = new JButton("Yes");
            continueYes.setSize(70,25);
            continueYes.setLocation(continueFrame.getWidth()/2-continueYes.getWidth()/2-50,continueFrame.getHeight()/2-20);
            continueFrame.add(continueYes);
            class ContinueYesListener implements ActionListener{
                @Override
                public void actionPerformed(ActionEvent e) {
                    //function triggered when continueYes is pushed
                    //closes continueFrame and tells server that the client wants to continue
                    continueFrame.setVisible(false);
                    writer.println("CONTINUE YES");
                    changeLabelText(outputLabel,gamePanel,"Waiting for next round to start");
                }
            }
            continueYes.addActionListener(new ContinueYesListener());

            //continueNo ends game
            JButton continueNo = new JButton("No");
            continueNo.setSize(70,25);
            continueNo.setLocation(continueFrame.getWidth()/2-continueYes.getWidth()/2+50,continueFrame.getHeight()/2-20);

            class ContinueNoListener implements ActionListener{
                @Override
                public void actionPerformed(ActionEvent e) {
                    //function triggered when continueNo button is pushed
                    //close continueFrame and tells server that the client wants to leave
                    continueFrame.setVisible(false);
                    writer.println("CONTINUE NO");
                    changeLabelText(outputLabel,gamePanel,"Exiting game...");
                }
            }
            continueNo.addActionListener(new ContinueNoListener());
            continueFrame.add(continueNo);
            continueFrame.setVisible(false);
        }
        frame.setVisible(true);
    }
    private static void handshake(Socket socket,BufferedReader reader, PrintWriter writer) throws IOException {
        //Simple handshake function to tell the server we aren't some random connection
        do {
            String[] connectionResponse = reader.readLine().split("\\|");
            //Server response is formatted {a}|{b}
            int a = Integer.parseInt(connectionResponse[0]);
            int b = Integer.parseInt(connectionResponse[1]);
            writer.println(a * b);
        }while(!reader.readLine().equals("READY"));
    }
    private void changeLabelText(JLabel label,JPanel panel, String string){
        //Helper function to change our label texts while keeping them centered.
        label.setText(string);
        Dimension labelSize = label.getPreferredSize();
        label.setBounds(panel.getWidth()/2-labelSize.width/2, label.getY(), labelSize.width, labelSize.height);
    }

    //refreshGamesList pings server to get the currently running games and updates the list
    private void refreshGamesList() throws IOException {
        //TODO: Add refresh button
        writer.println("GAMES LIST");
        String response = reader.readLine();
        if(response.startsWith("GAMES")){
            //Server response is formatted GAMES: game1Id:game1Name    (game1Players/game1MaxPlayers)|game2Id:game2Name    game2Players/game2MaxPlayers)|...
            String[] parameters = response.replace("GAMES","").trim().split("\\|");
            if(parameters[0].equals(""))//when just GAMES is sent the response is split into an array with just the element ""
                parameters = new String[0];
            String[] listData = new String[parameters.length];
            gameIds = new int[parameters.length];
            for(int i = 0; i < parameters.length; i++){
                String parameter = parameters[i];
                String[] gameParams = parameter.trim().split(":");
                gameIds[i] = Integer.parseInt(gameParams[0]);
                listData[i]= gameParams[1];
            }
            gameList.setListData(listData);
        }
        else
            throw new IOException("Unexpected Response");
    }

    public void runGame(){
        //Main game loop
        try{
            boolean running = true;
            do{
                String response = reader.readLine(); //gets server response. Blocks indefinitely
                if(response.equals("KEEP ALIVE")){
                    writer.println("KEEP ALIVE");
                } else if(response.startsWith("NUMSET")){
                    //outputs number set server responded with
                    String labelString = response.replace("NUMSET","").trim();
                    changeLabelText(numberLabel, gamePanel,labelString);
                    inputActivated = true;
                    //records start time to calculate user time spent on problem
                    startTime = new Date().getTime();
                }else if(response.equals("END ROUND")){
                    //ends round and outputs winner
                    inputActivated = false;
                    int winner = Integer.parseInt(reader.readLine().replace("WINNER","").trim());
                    changeLabelText(outputLabel,gamePanel,"Round over: player " + (winner+1) + " won");
                }else if(response.equals("CONTINUE?")){
                    //Shows continue pop up and waits for server to see if the game continues
                    continueFrame.setVisible(true);
                    String continueResponse = reader.readLine();

                    if(continueResponse.equals("GAME CONTINUE")){
                        changeLabelText(outputLabel,gamePanel,"");
                    }else if(continueResponse.equals("GAME END")){
                        //If the game ends we can set running to false to let this thread terminate,
                        //return to the intro panel and refresht the games list
                        running = false;
                        continueFrame.setVisible(false);
                        gamePanel.setVisible(false);
                        introPanel.setVisible(true);
                        refreshGamesList();
                    }
                }else if(response.startsWith("CORRECT")){
                    //Tells user when they get they get the correct response and deactivates input box
                    String labelText = response.replace("CORRECT","").trim() +
                            " Waiting for round to end...";
                    inputActivated= false;
                    changeLabelText(outputLabel,gamePanel,labelText);
                }else if(response.startsWith("INCORRECT")){
                    //Tells user when they input an incorrect response and prompts them to retry
                    String labelText = response.replace("INCORRECT","").trim()+
                            " Try again...";
                    inputActivated=true;
                    changeLabelText(outputLabel,gamePanel,labelText);
                }
                //TODO: Handle unexpected response
            }while(running);
            changeLabelText(outputLabel,gamePanel,"");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

