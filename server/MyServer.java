import java.net.*;
import java.io.*;
import org.json.*;

public class MyServer implements Runnable {
    Socket connection;
    InputStream inpStream;
    OutputStream outStream;
    int BUFFER_SIZE = 10240;

    MyServer(Socket connection) throws IOException {
        this.connection = connection;
        this.inpStream = connection.getInputStream();
        this.outStream = connection.getOutputStream();
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

        byte[] data = new byte[BUFFER_SIZE];
        int sent = 0;
        while (sent < filesize) {
            int need = Math.min(filesize - sent, BUFFER_SIZE);
            data = inp.readNBytes(need);
            sent += need;
            dout.write(data);
        }
        inp.close();
    }

    public void run() {
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

                System.out.println("Received from client: " + recvMess);

                JSONObject obj = new JSONObject(recvMess);
                JSONObject payload = (JSONObject) obj.getJSONObject("payload");

                String action = String.valueOf(obj.get("action")).toUpperCase();
                String sender = String.valueOf(payload.get("username"));

                //System.out.println(obj);


                if (action.equals("LOGIN")) {
                    /*
                        check for not duplicate username
                        login
                    */
                } else
                if (action.equals("LOGOUT")) {
                    /*
                        logout
                    */
                } else
                if (action.equals("SUBSCRIBE")) {
                    /*
                        check for valid username & topic
                        subscribe
                    */
                } else
                if (action.equals("UNSUBSCRIBE")) {
                    /*
                        check for valid username & topic
                        unsubscribe
                    */
                } else
                if (action.equals("CHAT")) {
                    String topic = String.valueOf(payload.get("topic"));
                    String message = String.valueOf(payload.get("message"));
                    /*
                        check for valid username & topic
                        sends message to topic
                    */
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