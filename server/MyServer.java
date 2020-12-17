import java.net.*;
import java.io.*;
import org.json.*;
import java.util.*;

//Khai bao bien toan cuc, cho ca parent thread va ca child thread co the access
class Global {
    public static int BUFFER_SIZE = 10240;
    volatile public static boolean busy = false;
    volatile public static String[] subscribers;
    static String[] topic_init= {"security","gamer","developer"};
    public static List<String> topic = Arrays.asList(topic_init);
    volatile public static List<String> client = new ArrayList<String>();
}
//Multi threading
public class MyServer implements Runnable {
    Socket connection;
    InputStream inpStream;
    OutputStream outStream;

    private static List<String> topic = new ArrayList<String>();

    MyServer(Socket connection) throws IOException {
        this.connection = connection;
        this.inpStream = connection.getInputStream();
        this.outStream = connection.getOutputStream();
    }

    public boolean checkClient(String username) {
        for (int i = 0;i<Global.client.size();i++) {
            if (username.equals(Global.client.get(i)))
                return true;
        }
        return false;
    }
    public void sendToClient(String sendMess) {
        try {
            DataOutputStream dout = new DataOutputStream(outStream);
            dout.writeBytes(sendMess + '\n');
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

    public void notifyMessage(String topic, String sender, String msg) {
        System.out.println("[" + topic + "] [" + sender + "]:  " + msg);
    }

    public void receiveFile(String filename, int filesize) throws IOException {
        OutputStream out = new FileOutputStream(new File(filename));
        byte[] data = new byte[Global.BUFFER_SIZE];
        int recv = 0;
        while (recv < filesize){
            int need = Math.min(filesize - recv, Global.BUFFER_SIZE);
            data = inpStream.readNBytes(need);
            recv += need;
            out.write(data);
        }
        out.close();
    }

    public void sendFile(String topic, String sender, String filename) throws IOException {
        int filesize = (int) new File(filename).length();
        sendToClient("NEW FILE " + topic + " " + sender + " " + filename + " " + filesize);

        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(inpStream));
        String recvMess = bufferRead.readLine().toUpperCase();
        if (!recvMess.equals("ACCEPT")) {
            return;
        }

        InputStream inp = new FileInputStream(new File(filename));
        DataOutputStream dout = new DataOutputStream(outStream);

        byte[] data = new byte[Global.BUFFER_SIZE];
        int sent = 0;
        while (sent < filesize) {
            int need = Math.min(filesize - sent, Global.BUFFER_SIZE);
            data = inp.readNBytes(need);
            sent += need;
            dout.write(data);
        }
        inp.close();
    }

    public void run() {
        String username = "";
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(inpStream));
        try {
            String recvMess;
            while(true) {
                recvMess = bufferRead.readLine();
                System.out.println(recvMess);
                if (recvMess.equals("QUIT")) {
                    System.out.println("A client offline.");
                    break;
                }

                if (username.equals(""))
                    System.out.println("Received from guess client: " + recvMess);
                else System.out.println("Received from " + username + " :" + recvMess);

                JSONObject obj = new JSONObject(recvMess);
                JSONObject payload = (JSONObject) obj.getJSONObject("payload");

                String action = String.valueOf(obj.get("action")).toUpperCase();
                String sender = String.valueOf(payload.get("username"));
                if (action.equals("LOGIN")) {
                    if (!Global.client.contains(sender) && username.equals("")) {
                        Global.client.add(sender);
                        username = sender;
                    }
                        else System.out.println("Invalid username.");
                } else
                if (action.equals("LOGOUT")) {
                    Global.client.remove(sender);
                    username = "";
                } else
                if (action.equals("SUBSCRIBE")) {
                    this.topic.add(String.valueOf(payload.get("topic")));
                } else
                if (action.equals("UNSUBSCRIBE")) {
                    this.topic.remove(String.valueOf(payload.get("topic")));
                } else
                if (action.equals("CHAT")) {
                    String topic = String.valueOf(payload.get("topic"));
                    String message = String.valueOf(payload.get("message"));
                    notifyMessage(topic, sender, message);
                } else if (action.equals("FILE")) {
                    String topic = String.valueOf(payload.get("topic"));
                    String filename = String.valueOf(payload.get("filename"));
                    int filesize = (Integer) payload.get("filesize");
                    /*
                        check for valid username & topic
                        sends file to topic
                    */

                    //for testing purpose only
                    receiveFile(filename, filesize);
                    sendFile(topic, sender, filename);
                    //
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            connection.close();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}