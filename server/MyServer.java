import java.net.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.io.*; 


public class MyServer implements Runnable{
    Socket connection;
    MyServer(Socket connection) {
        this.connection = connection;
    }

    public static void sendToClient(DataOutputStream dout, String sendMess) {
        try {
            dout.writeUTF(sendMess);  
            dout.flush(); 
            System.out.println("Sent to client: " + sendMess);
        }
        catch (Exception e) {
            System.out.println("Unknown Error.");
        }
    }






    

    public static void main(String args[]) throws Exception {
        System.out.println("Waiting for client ...");
        ServerSocket server = new ServerSocket(5000); 
        
        while (true) {
            Socket connection = server.accept();
            System.out.println("New client connected.");
            new Thread(new MyServer(connection)).start();
        }
    }


    public void run() {   
        try {
            DataInputStream din = new DataInputStream(connection.getInputStream());  
            DataOutputStream dout = new DataOutputStream(connection.getOutputStream());
            
            String recvMess="";
            int user_id;
            
            while(true) {
                recvMess = din.readUTF();
                System.out.println("Received from client: " + recvMess);

                if (recvMess.equals("login")) {

                } else
                if (recvMess.equals("logout")) {

                } else
                if (recvMess.equals("subscribe")) {

                } else
                if (recvMess.equals("unsubscribe")) {

                } else
                if (recvMess.equals("chat")) {

                } else
                if (recvMess.equals("file")) {

                }
            }  
            din.close();  
            connection.close();  
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}  