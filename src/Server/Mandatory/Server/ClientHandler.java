package Server.Mandatory.Server;

import Server.Mandatory.Stuff.Command;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

public class ClientHandler implements Runnable
{
    //Variables
    private final Socket clientSocket;
    private final ServerSocket serverSocket;
    private OutputStream ostream;
    private InputStream istream;
    private BufferedReader recieveRead;
    private String name;
    private HashMap<String, Command> serverCommands = new HashMap<>();
    private boolean shouldRun;
    private boolean isAlive = false;

    public void setShouldRun(boolean shouldRun) {
        this.shouldRun = shouldRun;
    }
    public boolean getShouldRun()
    {
        return this.shouldRun;
    }

    public String getName() {
        return name;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public boolean getAlive()
    {
        return this.isAlive;
    }

    public void setAlive(boolean alive) {
        this.isAlive = alive;
    }

    public ClientHandler(Socket clientSocket, ServerSocket serverSocket, String name) throws IOException {
        this.clientSocket = clientSocket;
        this.serverSocket = serverSocket;

        //initializes server command
        serverCommands = initializeServerCommandList();

        //Send to client
        this.ostream = clientSocket.getOutputStream();

        //recieving from client
        this.istream = clientSocket.getInputStream();
        this.recieveRead = new BufferedReader(new InputStreamReader(istream));

        this.name = name;
    }

    @Override
    public void run()
    {
        shouldRun = true;
        isAlive = true;
        while (shouldRun)
        {
            Thread heartbeatCheck = new Thread(new heartbeatCheckable());
            heartbeatCheck.start();
            isAlive = true;
            String message = null;

            //Recieves message from this client
            try {
                System.out.println("ready to read");
                System.out.println(clientSocket.isConnected());

                    message = this.recieveRead.readLine();

                System.out.println("message: " + message);
            } catch (IOException e) {

            } catch (Exception e)
            {
                System.out.println("hello");
            }


            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isServerCommand(String commandMsg)
    {
        if (commandMsg != null)
        {

            if (commandMsg.split(" ").length > 0) {
                if (serverCommands.containsKey(commandMsg.split(" ")[0])) {
                    if (serverCommands.get(commandMsg.split(" ")[0]).execute(commandMsg)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isShouldRun() {
        return shouldRun;
    }

    private HashMap<String,Command> initializeServerCommandList()
    {
        serverCommands.put("DATA", new Command() {
            @Override
            public boolean execute(String commandMessage) {
                if (commandMessage.length() > 2)
                {
                    try {
                        for (Socket socket : Server.clients.values())
                        {
                            PrintWriter pwrite = new PrintWriter(socket.getOutputStream(), true);
                            pwrite.println(name + ": " + commandMessage.split(" ")[2]);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });

        serverCommands.put("QUIT", new Command() {
            @Override
            public boolean execute(String commandMessage)
            {
                Server.clients.remove(name);

                for (Socket socket : Server.clients.values())
                {
                    try {
                        PrintWriter pwrite = new PrintWriter(socket.getOutputStream(), true);
                        pwrite.println(name + " has left the server");
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                shouldRun = false;
                return true;
            }
        });

        serverCommands.put("LIST", new Command() {
            @Override
            public boolean execute(String commandMessage)
            {
                PrintWriter pwrite = null;
                try {
                    pwrite = new PrintWriter(clientSocket.getOutputStream(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (String name : Server.clients.keySet())
                {
                    pwrite.println(name);
                }
                return true;
            }
        });

        serverCommands.put("IMALIVE", new Command() {
            @Override
            public boolean execute(String commandMessage) {
                System.out.println(name + " is alive");
                setAlive(true);
                return true;
            }
        });

        return serverCommands;
    }

    private class heartbeatCheckable implements Runnable
    {



        @Override
        public void run()
        {
            while (getShouldRun())
            {
                System.out.println("Should run is true");

                if (getAlive()) {
                    System.out.println("Get alive is true");
                    setAlive(false);
                    System.out.println(name + " is alive: " + isAlive);
                } else {
                    serverCommands.get("QUIT");
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
