package st.project;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private String hostName = "localhost";
    private int portNumber;
    private String nickname;
    private Socket clientSocket;
    private Scanner writer;
    private String message;
    private PrintWriter out;
    private BufferedReader in;

    public void start() {
        System.out.println("Insert port number:");
        Scanner scannedPort = new Scanner(System.in);
        if (scannedPort.hasNextInt()) {
            portNumber = Integer.parseInt(scannedPort.nextLine());
            //has to connect to port 9999
            if (portNumber != 9999) {
                System.out.println("Please input a valid port number");
                start();
                return;
            }
            connect();
        } else {
            System.out.println("Please input a valid port number");
            start();
        }

    }

    public void connect() {
        try {
            clientSocket = new Socket(hostName, portNumber);
            chooseNick();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void chooseNick() {
        System.out.println("Choose your nickname:");
        writer = new Scanner(System.in);
        nickname = writer.nextLine();
        //if client inputs a space (or several) by mistake, it will ask for a valid nickname
        if (nickname.trim().length() == 0) {
            System.out.println("The nickname you chose: " + "'" + nickname + "'" + " is not valid. Please use a valid one");
            chooseNick();
            return;
        }
        if (nickname.split(" ").length > 1) {
            System.out.println("The nickname you chose: " + "'" + nickname + "'" + " is not valid. You cannot use spaces");
            chooseNick();
            return;
        }
        startChatHelper();
    }

    public void startChatHelper() {

        try {
            out = new PrintWriter(clientSocket.getOutputStream());
            out.println(nickname);
            out.flush();
            ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
            singleExecutor.submit(new ClientHelper());

            chat();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void chat() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream());
            while (clientSocket.isBound()) {
                message = writer.nextLine();
                out.println(message);
                out.flush();
                if(message.equals("/quit")){
                    quit();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void quit(){
        try {
            in.close();
            out.close();
            clientSocket.close();
            System.exit(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public class ClientHelper implements Runnable {

        @Override
        public void run() {
            synchronized (clientSocket) {
                try {
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    while ((message = in.readLine()) != null) {
                        System.out.println(message);

                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
