import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private String wordleWord;
    private final List<String> wordleList = new ArrayList<>();
    private String userWord1,userWord2;
    private String message;
    private ServerSocket ss;
    private int numConnections;
    private ServerSideConnection player1;
    private ServerSideConnection player2;
    private final ServerSideConnection[] observers;
    private int numOfObservers = 0;

    public GameServer() throws FileNotFoundException {
        System.out.println("Game Server"); // to check execution
        numConnections = 0;
        observers = new ServerSideConnection[5];
        textImporter(); // read text file and assign a wordle Word
        System.out.println(wordleWord);

        try {
            ss = new ServerSocket(51734);
        }catch (IOException ex){
            System.out.println("IOException from GameServer Constructor");
        }
    }
    public void acceptConnections(){
        try{
            System.out.println("Waiting for connections...");
            if(numConnections < 7){
                while(numConnections < 2){
                    Socket s = ss.accept();
                    numConnections++;
                    System.out.println("Player #"+ numConnections +" has connected.");
                    ServerSideConnection ssc=new ServerSideConnection(s, numConnections);
                    if(numConnections ==1) {
                        player1=ssc;
                    }
                    else {
                        player2=ssc;
                    }
                    Thread t=new Thread(ssc);
                    t.start();
                }
                System.out.println("This game is a 2 player game. Two players already connected!!");
                while (numConnections < 7){
                    Socket s = ss.accept();
                    numConnections++;
                    System.out.println("Observer #"+ (numConnections-2) +" has connected.");
                    ServerSideConnection ssc=new ServerSideConnection(s, numConnections);
                    observers[numOfObservers] = ssc;
                    numOfObservers++;
                    Thread t=new Thread(ssc);
                    t.start();
                }
            }
        }catch (IOException ex){
            System.out.println("IOException from acceptConnections()");
        }
    }

    public class ServerSideConnection implements Runnable{
        private final Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private final int connectionID;

        public ServerSideConnection(Socket s, int id){
            socket = s;
            connectionID = id;
            try{
                dataIn = new DataInputStream(socket.getInputStream()); // to receive a data
                dataOut = new DataOutputStream((socket.getOutputStream()));// to send a data

            }catch (IOException ex){
                System.out.println("IOException from SSC constructor.");
            }
        }

        @Override
        public void run() {
            try{
                dataOut.writeInt(connectionID);
                dataOut.writeUTF(wordleWord);
                dataOut.flush();
                while(true) {
                    if(connectionID == 1){
                        String tempstr;
                        tempstr = dataIn.readUTF();
                        System.out.println(tempstr);
                        List<String> tempList = Arrays.asList(tempstr.split(""));
                        if (Objects.equals(tempList.get(0), "*")){
                            message = tempstr.substring(1);
                            player2.send("*Player1: " + message);
                            if (numConnections > 2){
                                for (int i = 0; i < numOfObservers; i++) {
                                    observers[i].send("*Player1: " + message);
                                }
                            }
                        }
                        else{
                            userWord1 = tempstr;
                            System.out.println(userWord1);
                            player2.send(userWord1);
                            if (numConnections > 2){
                                for (int i = 0; i< numOfObservers; i++){
                                    observers[i].send(userWord1);
                                }
                            }
                        }
                    }
                    else if(connectionID == 2){
                        String tempstr;
                        tempstr = dataIn.readUTF();
                        List<String> tempList = Arrays.asList(tempstr.split(""));
                        if (Objects.equals(tempList.get(0), "*")){
                            message = tempstr.substring(1);
                            player1.send("*Player2: " + message);
                            if (numConnections > 2){
                                for (int i = 0; i < numOfObservers; i++) {
                                    observers[i].send("*Player2: " + message);
                                }
                            }
                        }
                        else{
                            userWord2 = tempstr;
                            System.out.println(userWord2);
                            player1.send(userWord2);
                            if (numConnections > 2){
                                for (int i = 0; i< numOfObservers; i++){
                                    observers[i].send(userWord2);
                                }
                            }
                        }
                    }
                    else{
                        message = dataIn.readUTF();
                        message = message.substring(1);
                        player1.send("*Observer"+ (connectionID-2)+": " + message);
                        player2.send("*Observer"+ (connectionID-2)+": " + message);
                        for (int i = 0; i < numOfObservers; i++) {
                            if (connectionID == i+3){

                            }
                            else{
                                observers[i].send("*Observer"+ (connectionID-2)+": " + message);
                            }
                        }
                    }
                }
            }
            catch(IOException ex) {
                System.out.println("IOException from run SSC");
            }
        }
        public void send(String userWord){ // to send user word or messages
            try{
                dataOut.writeUTF(userWord);
                dataOut.flush();
            }
            catch (IOException ex){

            }
        }
    }


    public void textImporter() throws FileNotFoundException {
        Scanner sc = new Scanner(new File("wordlewords.txt"));

        while (sc.hasNextLine()) {
            wordleList.add(sc.nextLine().toUpperCase());
        }
        sc.close();

        Random rand = new Random();
        wordleWord = wordleList.get(rand.nextInt(wordleList.size()));

    }

    public static void main(String[] args) throws FileNotFoundException {
        GameServer gs = new GameServer();
        gs.acceptConnections();
    }
}
