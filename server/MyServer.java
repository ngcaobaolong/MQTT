import java.net.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.io.*; 


public class MyServer implements Runnable{
    Socket connection;
    static Set<Integer> onlineUsers = new HashSet<Integer>();
    static Map<Integer, DataOutputStream> map = new HashMap<Integer, DataOutputStream>();
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

    public static String getUserList() {
        String res = "";
        for (Integer user : onlineUsers) {
            res += String.valueOf(user) + " ";
        }
        return res;
    }

    public static void login(int user_id, DataOutputStream dout) {
        System.out.println("New user login: " + String.valueOf(user_id));
        sendToClient(dout, "200 OK, " + "ONLINE USERS: " + getUserList());
        onlineUsers.add(user_id);
        map.put(user_id, dout);
    }

    public static void logout(int user_id, DataOutputStream dout) {
        System.out.println("New user logout: " + String.valueOf(user_id));
        onlineUsers.remove(user_id);
        map.remove(user_id);
        for (Integer user : onlineUsers) {
            sendToClient(map.get(user), "ONLINE USERS: " + getUserList());
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
                if (recvMess.equals("QUIT")) {
                    break;
                }
            }  
            din.close();  
            connection.close();  
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}  