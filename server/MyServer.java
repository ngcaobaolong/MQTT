import java.net.*;
import java.io.*;
import org.json.*;

public class MyServer implements Runnable {
    Socket connection;
    InputStream inpStream;
    static int BUFFER_SIZE = 10240;
    MyServer(Socket connection) throws IOException {
        this.connection = connection;
        this.inpStream = connection.getInputStream();
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

    public void notifyEvent(String topic, String sender, String msg) {
        System.out.println("[" + topic + "] [" + sender + "]:  " + msg);
    }

    public void solveRecvFile(String filename, int filesize) throws IOException {
        OutputStream out = new FileOutputStream(new File(filename));
        byte[] data = new byte[BUFFER_SIZE];
        int recv = 0;
        while (recv < filesize){
            int need = Math.min(filesize - recv, BUFFER_SIZE);
            data = inpStream.readNBytes(need);
            recv += need;
            out.write(data);
        }
        out.close();
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

                JSONObject obj = new JSONObject(recvMess);
                JSONObject payload = (JSONObject) obj.getJSONObject("payload");

                String action = String.valueOf(obj.get("action")).toUpperCase();
                String sender = String.valueOf(payload.get("username"));

                System.out.println(obj);
                solveRecvFile("Ocean.gif", 2601345);
                int a = 5;
                if (a < 3) break;
                continue;


//                if (action.equals("LOGIN")) {
//
//                } else
//                if (action.equals("LOGOUT")) {
//
//                } else
//                if (action.equals("SUBSCRIBE")) {
//
//                } else
//                if (action.equals("UNSUBSCRIBE")) {
//
//                } else
//                if (action.equals("CHAT")) {
//                    String topic = String.valueOf(payload.get("topic"));
//                    String message = String.valueOf(payload.get("message"));
//                    // Broker select clients to send
//                    notifyEvent(topic, sender, message);
//                } else
//                if (action.equals("FILE")) {
//                    String topic = String.valueOf(payload.get("topic"));
//                    String message = String.valueOf(payload.get("message"));
//                    String filename = String.valueOf(payload.get("filename"));
//                    int filesize = (Integer) payload.get("filesize");
//
//                } else
//                if (action.equals("QUIT")) {
//                    break;
//                }
            }  
            din.close();  
            connection.close();  
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}  