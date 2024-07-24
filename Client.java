import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    boolean receive=true;
    DataOutputStream writeData;
    DataInputStream readData;

    Client(Socket s) throws Exception{
        readData=new DataInputStream(s.getInputStream());
        writeData = new DataOutputStream(s.getOutputStream());
    }

    void listenClient(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        char flag = 'n';
                        if (receive) {
                            while (flag == 'n') {
                                flag = readData.readChar();
                            }
                            receive = false;
                        }
                        if (flag == 's') {
                            StringBuilder filePath = new StringBuilder();
                            StringBuilder fileName = new StringBuilder();

                            int pathSize = readData.readInt();
                            for (int j = 0; j < pathSize; j++) {
                                filePath.append(readData.readChar());
                            }

                            int nameSize = readData.readInt();
                            for (int j = 0; j < nameSize; j++) {
                                fileName.append(readData.readChar());
                            }

                            receiveContents(filePath, fileName);
                        }
                    } catch (Exception e) {
                        System.out.println("File handling problem!");
                    }
                }
            }
        }).start();
    }

    void sendFile(Socket s, String fileLocation) {
        try {
            File file = new File(fileLocation);
            FileInputStream fileRead = new FileInputStream(file);

            writeData.writeChar('s');
            writeData.flush();

            // parse the file name and type
            String[] locationParts = fileLocation.split("\\\\");
            String fileName = locationParts[locationParts.length - 1];
            writeData.writeInt(fileName.length());
            writeData.writeChars(fileName);


            writeData.writeLong(file.length());

            byte[] buffer = new byte[4 * 1024];
            int bytes = 0;
            while ((bytes = fileRead.read(buffer)) != -1) {
                writeData.write(buffer, 0, bytes);
                writeData.flush();
            }
            fileRead.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void receiveContents(StringBuilder filePath, StringBuilder fileName) {
        try {
            File folder = new File(filePath.toString());
            if (!folder.exists()) {
                folder.mkdir();
            }
            FileOutputStream writeFile = new FileOutputStream(filePath.toString() + fileName.toString());

            long fileSize = readData.readLong();

            int bytes = 0;
            byte[] buffer = new byte[4 * 1024];
            while (fileSize>0 && (bytes = readData.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                writeFile.write(buffer, 0, bytes);
                writeFile.flush();
                fileSize -= bytes;
            }
            receive = true;
            writeFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter the IP Address: ");
        String ip=sc.nextLine();
        System.out.println("Enter the files to send: ");
        Socket s = new Socket(ip, 5001);
        Client c = new Client(s);
        c.listenClient();
        while (s.isConnected()) {
            String location = sc.nextLine();
            c.sendFile(s, location);
        }
        s.close();
        c.writeData.close();
        c.readData.close();
    }
}
