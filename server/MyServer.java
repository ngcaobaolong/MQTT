import java.net.*;
import java.io.*;

import org.json.*;

import java.util.*;

//Khai bao bien toan cuc, cho ca parent thread va ca child thread co the access
class Global {
    static int BUFFER_SIZE = 10240;
    volatile boolean busy = false;
    volatile static List<userStruct> userStructList = new ArrayList<userStruct>();
}

//Struct-like class
class userStruct {
    public String username = "";
    public Socket connection;
    public InputStream inpStream;
    public OutputStream outStream;
    public List<String> topic = new ArrayList<String>();
}

//Multi threading
public class MyServer implements Runnable {
    userStruct user = new userStruct();
    List<String> topic = new ArrayList<String>();

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
            System.out.println("New client connected. Socker.no " + connection);
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

    public void sendFile(String topic, String sender, String filename, OutputStream outStream) throws IOException {
        File fileToClient = new File(filename);
        int filesize = (int) fileToClient.length();
        sendToClient("NEW FILE " + topic + " " + sender + " " + filename + " " + filesize, outStream);
        InputStream inp = new FileInputStream(fileToClient);
        DataOutputStream dout = new DataOutputStream(outStream);
        byte[] data = new byte[Global.BUFFER_SIZE];
        int sent = filesize;
        while (sent > 0) {
            int need = Math.min(sent, Global.BUFFER_SIZE);
            data = inp.readNBytes(need);
            sent -= need;
            dout.write(data);
        }
        sendToClient("NEW MESSAGE SERVER 200 FILE-SENT", outStream);
        inp.close();
    }

    public void run() {
        String tmp = "";
        for (int i = 0; i < Global.userStructList.size(); i++)
            System.out.println(Global.userStructList.get(i).username);
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(this.user.inpStream));
        try {
            String recvMess;
            while (true) {
                recvMess = bufferRead.readLine();
                if (recvMess.equals("QUIT")) {
                    System.out.println("A client offline.");
                    break;
                }

                if (this.user.username.equals(""))
                    System.out.println("Received from guess client: " + recvMess);
                else System.out.println("Received from " + this.user.username + " :" + recvMess);

                JSONObject obj = new JSONObject(recvMess);
                JSONObject payload = (JSONObject) obj.getJSONObject("payload");
                String action = String.valueOf(obj.get("action")).toUpperCase();
                String sender = String.valueOf(payload.get("username"));
                if (action.equals("LOGIN")) {
                    if (!checkClient(sender) && user.username.equals("")) {
                        this.user.username = sender;
                        Global.userStructList.add(user);
                        sendToClient("NEW MESSAGE SERVER 200 LOGGED-IN", this.user.outStream);
                    } else sendToClient("NEW MESSAGE SERVER 400 USERNAME-INVALID", this.user.outStream);
                } else if (action.equals("LOGOUT")) {
                    //remove username out of user list
                    for (int i = 0; i < Global.userStructList.size(); i++)
                        if (Global.userStructList.get(i).username.equals(this.user.username)) {
                            sendToClient("NEW MESSAGE SERVER 200 LOGGED-OUT", this.user.outStream);
                            Global.userStructList.remove(i);
                            this.user.username = "";
                            this.user.topic.clear();
                            break;
                        }
                } else if (action.equals("SUBSCRIBE")) {
                    //add topic into topic list
                    for (int i = 0; i < Global.userStructList.size(); i++) {
                        System.out.println(Global.userStructList.get(i).username + " " + Global.userStructList.get(i).topic.toString());
                        if (Global.userStructList.get(i).username.equals(this.user.username)) {
                            Global.userStructList.get(i).topic.add(String.valueOf(payload.get("topic")));
//                            this.user.topic.add(String.valueOf(payload.get("topic")));
                            sendToClient("NEW MESSAGE SERVER 200 SUBSCRIBED ", Global.userStructList.get(i).outStream);
                        }
                    }

                } else if (action.equals("UNSUBSCRIBE")) {
                    boolean found = false;
                    for (int i = 0; i < Global.userStructList.size(); i++) {
                        System.out.println(Global.userStructList.get(i).username + " " + Global.userStructList.get(i).topic.toString());
                        if (Global.userStructList.get(i).username.equals(this.user.username) && Global.userStructList.get(i).topic.contains(String.valueOf(payload.get("topic")))) {
                            Global.userStructList.get(i).topic.remove(String.valueOf(payload.get("topic")));
//                            this.user.topic.remove(String.valueOf(payload.get("topic")));
                            sendToClient("NEW MESSAGE SERVER 200 UNSUBSCRIBED", Global.userStructList.get(i).outStream);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("TOPIC NOT FOUND");
                        sendToClient("NEW MESSAGE SERVER 400 TOPIC-ERROR", this.user.outStream);
                    }
                } else if (action.equals("CHAT")) {
                    boolean send = false;
                    String topic = String.valueOf(payload.get("topic"));
                    String message = String.valueOf(payload.get("message"));
                    if (!this.user.topic.contains(topic))
                        sendToClient("NEW MESSAGE SERVER 400 TOPIC-ERROR", this.user.outStream);
                    else {
                        for (int i = 0; i < Global.userStructList.size(); i++) {
                            if (Global.userStructList.get(i).topic.contains(topic)) {
                                send = true;
                                tmp = "NEW MESSAGE " + topic + " " + this.user.username + " " + message;
                                if (!Global.userStructList.get(i).username.equals(this.user.username)) sendToClient(tmp, Global.userStructList.get(i).outStream);
                            }
                        }
                    if (send) sendToClient("NEW MESSAGE SERVER 200 MESSAGE-SENT", this.user.outStream); else
                        sendToClient("NEW MESSAGE SERVER 400 MESSAGE-FAILED", this.user.outStream);
                    }
                } else if (action.equals("FILE")) {
                    //File not done yet
                    String topic = String.valueOf(payload.get("topic"));
                    String filename = String.valueOf(payload.get("filename"));
                    int filesize = (Integer) payload.get("filesize");
                    receiveFile(filename, filesize);
                    if (!this.user.topic.contains(topic))
                        sendToClient("NEW MESSAGE SERVER 400 TOPIC-ERROR", this.user.outStream);
                    else
                        for (int i = 0; i < Global.userStructList.size(); i++)
                            if (Global.userStructList.get(i).topic.contains(topic) && !Global.userStructList.get(i).username.equals(this.user.username)) {
                                sendFile(topic, Global.userStructList.get(i).username, filename, Global.userStructList.get(i).outStream);
                            }
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