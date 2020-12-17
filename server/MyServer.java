import java.net.*;
import java.io.*;

import org.json.*;

import java.util.*;

//Khai bao bien toan cuc, cho ca parent thread va ca child thread co the access
class Global {
    static int BUFFER_SIZE = 10240;
    volatile static boolean busy = false;
    volatile static List<userStruct> userStructList = new ArrayList<userStruct>();
}

//Struct-like class
class userStruct {
    static String username = "";
    static Socket connection;
    static InputStream inpStream;
    static OutputStream outStream;
    static List<String> topic = new ArrayList<String>();
}

//Multi threading
public class MyServer implements Runnable {
    userStruct user = new userStruct();
    private volatile static List<String> topic = new ArrayList<String>();
    MyServer(Socket connection) throws IOException {
        this.user.connection = connection;
        this.user.inpStream = connection.getInputStream();
        this.user.outStream = connection.getOutputStream();
    }

    public boolean checkClient(String username) {
        if (Global.userStructList.size() == 0) return false;
        for (int i = 0; i < Global.userStructList.size(); i++) {
            if (username.equals(Global.userStructList.get(i).username))
                return true;
        }
        return false;
    }

    public void sendToClient(String sendMess, OutputStream outStream) {
        try {
            DataOutputStream dout = new DataOutputStream(outStream);
            dout.writeBytes(sendMess + '\n');
            System.out.println("Sent to client: " + sendMess);
        } catch (Exception e) {
            System.out.println("Unknown Error.");
        }
    }

    //MAIN
    public static void main(String args[]) throws Exception {
        System.out.println("Waiting for client ...");
        ServerSocket server = new ServerSocket(5000);

        while (true) {
            Socket connection = server.accept();
            System.out.println("New client connected. Socker.no "+connection);
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
        while (recv < filesize) {
            int need = Math.min(filesize - recv, Global.BUFFER_SIZE);
            data = this.user.inpStream.readNBytes(need);
            recv += need;
            out.write(data);
        }
        out.close();
    }

    public void sendFile(String topic, String sender, String filename) throws IOException {
        int filesize = (int) new File(filename).length();
        sendToClient("NEW FILE " + topic + " " + sender + " " + filename + " " + filesize, this.user.outStream);

        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(this.user.inpStream));
        String recvMess = bufferRead.readLine().toUpperCase();
        if (!recvMess.equals("ACCEPT")) {
            return;
        }

        InputStream inp = new FileInputStream(new File(filename));
        DataOutputStream dout = new DataOutputStream(this.user.outStream);

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
        System.out.println(this.user.username + " " + this.user.topic + " " + this.user.connection);
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(this.user.inpStream));
        try {
            String recvMess;
            while (true) {
                recvMess = bufferRead.readLine();
                System.out.println(recvMess);
                if (recvMess.equals("QUIT")) {
                    System.out.println("A client offline.");
                    break;
                }

                if (user.username.equals(""))
                    System.out.println("Received from guess client: " + recvMess);
                else System.out.println("Received from " + this.user.username + " :" + recvMess);

                JSONObject obj = new JSONObject(recvMess);
                JSONObject payload = (JSONObject) obj.getJSONObject("payload");

                String action = String.valueOf(obj.get("action")).toUpperCase();
                String sender = String.valueOf(payload.get("username"));
                if (action.equals("LOGIN")) {
                    if (!checkClient(sender) && this.user.username.equals("")) {
                        this.user.username = sender;
                        Global.userStructList.add(this.user);
                    } else System.out.println("Invalid username.");
                } else if (action.equals("LOGOUT")) {
                    //remove username out of user list
                    for (int i = 0; i < Global.userStructList.size(); i++)
                        if (Global.userStructList.get(i).username.equals(this.user.username)) {
                            Global.userStructList.remove(i);
                            break;
                        }
                    this.user.username = "";
                    this.user.topic.clear();
                } else if (action.equals("SUBSCRIBE")) {
                    //add topic into topic list
                    for (int i = 0; i < Global.userStructList.size(); i++) {
                        if (Global.userStructList.get(i).username.equals(this.user.username))
                            Global.userStructList.get(i).topic.add(String.valueOf(payload.get("topic")));
                    }

                } else if (action.equals("UNSUBSCRIBE")) {
                    for (int i = 0; i < Global.userStructList.size(); i++) {
                        if (Global.userStructList.get(i).username.equals(this.user.username))
                            Global.userStructList.get(i).topic.remove(String.valueOf(payload.get("topic")));
                    }
                } else if (action.equals("CHAT")) {
                    //Chat not done yet
                    System.out.println(Global.userStructList.toString());
                    String topic = String.valueOf(payload.get("topic"));
                    String message = String.valueOf(payload.get("message"));
                    for (int i = 0; i < Global.userStructList.size(); i++) {
                        if (Global.userStructList.get(i).topic.contains(topic))
                            sendToClient(message, Global.userStructList.get(i).outStream);
                    }
                    notifyMessage(topic, sender, message);
                } else if (action.equals("FILE")) {
                    //File not done yet
                    String topic_publish = String.valueOf(payload.get("topic"));
                    String filename = String.valueOf(payload.get("filename"));
                    int filesize = (Integer) payload.get("filesize");
                    /*
                        check for valid username & topic
                        sends file to topic
                    */

                    //for testing purpose only
                    receiveFile(filename, filesize);
                    sendFile(topic_publish, sender, filename);
                    //
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            this.user.connection.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}