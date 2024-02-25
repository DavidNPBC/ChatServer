package st.project;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private int portNumber;
    private Map<String, Socket> clientList = new HashMap<>();
    private int johnDoeCounter;
    private final String enter = "\n";
    private BufferedReader in;
    private PrintWriter out;
    private Socket clientSocket;
    private String[] pokemons = {"blastoise", "bulbasaur", "caterpie", "charizard", "pikachu", "venusaur"};

    public void start() {
        portNumber = 9999;
        ServerSocket serverSocket = null;
        System.out.println("Server 9999 initializing ....");

        try {
            serverSocket = new ServerSocket(portNumber);
            while (true) {
                clientSocket = serverSocket.accept();
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                String nick = in.readLine();

                //if your nickname is already in use, you enter with a default generated nickname
                if (!clientList.containsKey(nick)) {
                    distributeToServerHelper(clientSocket, nick, in);
                } else {
                    String generatedNickname = generateNewNick();
                    distributeToServerHelper(clientSocket, generatedNickname, in);
                    out.println("/ERROR -- The nickname you chose is already in use. You were assigned a default nickname: " + generatedNickname + "." + enter + "To change it type /change + newNickname" + enter);
                    out.flush();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateNewNick() {
        String generatedNick = "JohnDoe";
        if (johnDoeCounter > 0) {
            generatedNick += " " + johnDoeCounter;
        }
        johnDoeCounter++;
        return generatedNick;
    }


    //send this logged in message to everyone
    public void distributeToServerHelper(Socket clientSocket, String nick, BufferedReader in) {

        clientList.put(nick, clientSocket);

        System.out.println(nick + " connected to the server ...");
        ExecutorService cachedPool = Executors.newCachedThreadPool();
        cachedPool.submit(new ServerHelper(clientSocket, nick, in));
    }

    public class ServerHelper implements Runnable {
        private String mapKey;
        private Socket clientSocket;
        private String message;
        private BufferedReader in;
        private PrintWriter out;
        private String formerNick;
        private File file;
        private BufferedReader readAscii;
        private String asciiArt = "";
        private String quit = "";

        public ServerHelper(Socket socket, String nick, BufferedReader in) {
            this.mapKey = nick;
            this.clientSocket = socket;
            this.in = in;
        }

        public void messageManagement() {
            try {
                message = in.readLine();
                //Message is null - break run while loop and logout
                if (message == null) {
                    quit = message;
                    return;
                }
                //message has just spaces, most likely a typo
                if (message.trim().length() == 0) {
                    return;
                }

                if (message.contains("/quit")) {
                    quit();
                    return;
                }

                if (message.contains("/list")) {
                    checkList(out);
                    return;
                }

                if (message.contains("/risky")) {
                    riskyText();
                    return;
                }

                if (message.contains("/commands")) {
                    sendCommands();
                    return;
                }

                if (message.contains("/pm")) {
                    messageTarget();
                    return;
                }

                if (message.contains("/spongebob")) {
                    sendArt("spongebob");
                    return;
                } else if (message.contains("/pokemon")) {
                    sendArt("pokemon");
                    return;
                } else if (message.contains("/kakarot")) {
                    sendArt("kakarot");
                    return;
                }

                if (message.contains("/change")) {
                    changeNickname();
                    return;
                }

                if (message.startsWith("/")) {
                    out.println("Your command had an error, please send a valid command ...");
                    out.flush();
                    return;
                }

                sendAll("normal");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void changeNickname() {
            //invalid use of the command
            if (message.split(" ").length != 2) {
                out.println("Please input a valid username - your name cannot contain spaces or you did not input a new username");
                out.flush();
                return;
            }

            //if the nickname has been automatically attributed, remove that default user name so it can be reused in the future
            if (mapKey.contains("JohnDoe")) {
                johnDoeCounter--;
            }

            clientList.remove(this.mapKey, this.clientSocket);
            String[] nickArray = message.split(" ");
            formerNick = mapKey;
            this.mapKey = nickArray[1];
            clientList.put(this.mapKey, clientSocket);

            sendAll("change");
        }

        public void checkList(PrintWriter out) {
            String presences = "Present in the chat: ";

            for (String nick : clientList.keySet()) {
                presences += "\n" + nick;
                //identify your current nickname in the list
                if (nick.equals(this.mapKey)) {
                    presences += " (You)";
                }
            }
            presences += "\n" + "***";
            out.println(presences);
            out.flush();
        }

        public void quit() {
            synchronized (clientList) {
                clientList.remove(mapKey);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            sendAll("quit");
        }

        public void sendCommands() {
            String availableCommands = "/commands -- ask for available commands" + enter +
                    "/change + exampleNewNickname -- changes your nickname to exampleNewNickname" + enter
                    + "/list -- list of people in the chat" + enter
                    + "/risky -- send a text to someone randomnly" + enter
                    + "/pm + availableNickname -- send a private message to availableNickname" + enter
                    + "/spongebob or /kakarot or /pokemon -- request ASCII art" + enter
                    +"/quit -- leave the chat";
            out.println(availableCommands);
            out.flush();
        }

        public void sendAll(String call) {
            String messageAll = "[" + mapKey + "]: " + message;

            //when someone leaves, change message format
            if (call.equals("quit")) {
                messageAll = mapKey + " has left the chat ...";
            }

            if (call.equals("change")) {
                messageAll = formerNick + " has changed its nick to " + mapKey + " ...";
            }

            if (call.equals("ascii")) {
                messageAll = mapKey + " sent: " + "\n" + asciiArt;
            }

            for (Socket client : clientList.values()) {
                try {
                    PrintWriter outAll = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));

                    //not send the person's message back and sending that person an acknowledgement that the message was received
                    if (!clientSocket.equals(client)) {
                        outAll.println(messageAll);
                        outAll.flush();
                    } else {
                        //only send an acknowledgement of a message sent if it is a message
                        if (!call.equals("change")) {
                            outAll.println("Server successfully received and sent your message");
                            outAll.flush();
                            //if you are trying to change your username, then you receive the appropriate success message
                        } else if (call.equals("change")) {
                            outAll.println("Successfully change your user name to: " + this.mapKey);
                            outAll.flush();
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println(messageAll);
            asciiArt = "";
        }

        public void riskyText() {
            if (message.split(" ").length < 2) {
                out.println("Wrong use of the /risky command. Please send /risky + your message");
                out.flush();
                return;
            }
            String[] messageToBeSent = message.split(" ");
            String users = "";
            for (String nick : clientList.keySet()) {
                //just send to other people
                if (!nick.equals(mapKey)) {
                    users += nick + " ";
                }
            }
            String[] usersArray = users.split(" ");

            try {
                //choose random out channel to send the text to
                PrintWriter outRisky = new PrintWriter(clientList.get(usersArray[(int) Math.floor(Math.random() * usersArray.length)]).getOutputStream());
                String messageRisky = "";
                //append the whole message
                for (int i = 1; i < messageToBeSent.length; i++) {
                    messageRisky += messageToBeSent[i] + " ";
                }

                outRisky.println("Received risky message from: " + this.mapKey);
                outRisky.println(messageRisky);
                outRisky.flush();
                out.println("Server successfully received and sent your message");
                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        public void sendArt(String art) {
            if (art.equals("kakarot")) {
                file = new File("resources/kakarot.txt");
            }

            if (art.equals("spongebob")) {
                file = new File("resources/spongebob.txt");
            }

            if (art.equals("pokemon")) {
                String randomPokemon = pokemons[(int) Math.floor(Math.random() * pokemons.length)];
                file = new File("resources/pokemons/" + randomPokemon + ".txt");
            }

            try {
                readAscii = new BufferedReader(new FileReader(file));
                String line = "";
                while ((line = readAscii.readLine()) != null) {
                    asciiArt += line + "\n";
                }

            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            sendAll("ascii");
        }

        public void messageTarget() {
            String pmTarget = message.split(" ")[1];
            Socket targetSocket = clientList.get(pmTarget);
            //check if target client exists
            if (targetSocket != null) {
                try {
                    PrintWriter targetOut = new PrintWriter(new OutputStreamWriter(targetSocket.getOutputStream()));
                    //ignoring first two words of the message (command and nickname) and sending the rest
                    String[] targetMessageArray = message.split(" ");
                    String targetMessage = "";
                    for (int i = 2; i < targetMessageArray.length; i++) {
                        targetMessage += targetMessageArray[i] + " ";
                    }
                    //block a private message to yourself.
                    if (targetSocket.equals(this.clientSocket)) {
                        out.println("You cannot message yourself.");
                        out.flush();
                        return;
                    }

                    targetOut.println("Received a private message from: " + this.mapKey);
                    targetOut.flush();
                    targetOut.println(targetMessage);
                    targetOut.flush();
                    out.println("Your message to: " + pmTarget + ", was successfully sent.");
                    out.flush();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //intended socket is not in the HashMap, cannot message it.
            } else {
                out.println("Please input a valid nickname in chat. " + pmTarget + ", is not in the chat.");
                out.flush();
            }
        }

        @Override
        public void run() {

            synchronized (clientSocket) {
                try {
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                sendCommands();
                while (clientSocket.isBound()) {
                    //when client logs out without using the quit command
                    if (quit == null) {
                        quit();
                        break;
                    }
                    messageManagement();
                }
            }

        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
