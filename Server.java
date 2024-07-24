import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    ArrayList<ClientHandler> clientHandlers=new ArrayList<>();;
    Server(ServerSocket ss) {
        try {
            while(!ss.isClosed()){
                Socket s = ss.accept();
                System.out.println("A client is connected!");
                ClientHandler clientHandler = new ClientHandler(s, this);
                clientHandlers.add(clientHandler);
                Thread client = new Thread(clientHandler);
                client.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket(5001);
        new Server(ss);
    }
}

class ClientHandler implements Runnable{
    Server server;
    ClientHandler currentClientHandler;
    boolean receive = true;
    private Socket s;
    DataInputStream readData;
    DataOutputStream sendClient;


    ClientHandler(Socket s, Server server) throws Exception{
        this.s=s;
        readData=new DataInputStream(s.getInputStream());
        sendClient = new DataOutputStream(s.getOutputStream());
        this.server=server;
        currentClientHandler=this;
    }

    public void run(){
        try {
            while (true) {
                // This flag is used to avoid exceptions.
                char flag = 'n';
                if (receive) {
                    while (flag == 'n') {
                        flag = readData.readChar();
                    }
                    receive = false;
                }
                if (flag == 's') {
                    // reading file name;
                    int nameLength = readData.readInt();
                    StringBuilder fileName = new StringBuilder();
                    for (int j = 0; j < nameLength; j++) {
                        fileName.append(readData.readChar());
                    }

                    String file = "C:\\Share Stream\\";
                    sendOtherClients(file, fileName);
                }
            }
        } catch(Exception e){
            System.out.println("File handling problems!");
        }
    }

    void sendOtherClients(String filePath, StringBuilder fileName){
//        System.out.println(server.clientHandlers.size());
        for(ClientHandler client: server.clientHandlers){
            if(client==currentClientHandler) continue;
            sendContents(filePath,fileName,client);
        }
    }

    void sendContents(String filePath, StringBuilder fileName, ClientHandler client){
        try {
            client.sendClient.writeChar('s');
            // Send file name and path
            client.sendClient.writeInt(filePath.length());
            client.sendClient.flush();
            client.sendClient.writeChars(filePath);
            client.sendClient.flush();
            client.sendClient.writeInt(fileName.length());
            client.sendClient.flush();
            client.sendClient.writeChars(fileName.toString());
            client.sendClient.flush();

            // Send content of the file
            long contentSize=readData.readLong();
            client.sendClient.writeLong(contentSize);

            int bytes=0;
            byte[] buffer=new byte[4*1024];
            while(contentSize>0 && (bytes=readData.read(buffer,0,(int)Math.min(buffer.length, contentSize)))!=-1){
                client.sendClient.write(buffer,0,bytes);
                client.sendClient.flush();
                contentSize-=bytes;
            }
            receive=true;
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}