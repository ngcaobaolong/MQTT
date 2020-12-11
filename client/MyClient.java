import java.net.*;
import java.util.Scanner;
import java.io.*;
import org.json.*;


class Global {
    public static String FILEPATH = "";
    public static int BUFFER_SIZE = 10240;
    volatile public static boolean closed = false;

    volatile public static int sending = 0;
    volatile public static boolean err_frag = false;
    volatile public static boolean acked = false;

    volatile public static String username;
}

class RecvThread extends Thread {
    InputStream inpStream;

    RecvThread(InputStream inpStream) {
        this.inpStream = inpStream;
    }

    private static void solveRecvFile(InputStream inpStream, String filename, int filesize) throws IOException {
        // downloaded file name: "<filename>-<username>"
        OutputStream out = new FileOutputStream(new File(FILEPATH + filename + "-" + Global.username));
        byte[] data = new byte[Global.BUFFER_SIZE];
        int recv = 0;
        while (recv < filesize){
            int need = Math.min(filesize - recv, Global.BUFFER_SIZE);
            data = inpStream.readNBytes(need);
            recv += need;
            //sendToServ(outStream,"OK frag");
            out.write(data);
        }
        out.close();

        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(inpStream));
        String recvMsg = bufferRead.readLine();
        //notiRecv(recvMsg);
    }

    public void run() {
        try {
            BufferedReader bufferRead = new BufferedReader(new InputStreamReader(inpStream));

            String recvMess;
            while (true) {
                if (Global.closed) {
                    break;
                }
                recvMess = bufferRead.readLine();
                System.out.println("Message received from server:  " + recvMess);
                if (recvMess.equals("")) {
                    solveRecvFile(bufferRead);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

class SendThread extends Thread {
    DataOutputStream dout;

    SendThread(OutputStream outStream) {
        this.dout = new DataOutputStream(outStream);
    }

    public JSONObject initData(String action) {
        JSONObject data = new JSONObject();
        JSONObject payload = new JSONObject();
        data.put("action", action);
        payload.put("username", Global.username);
        data.put("payload", payload);
        return data;
    }

    public void sendToServer(String command) {
        try {
            dout.writeUTF(command);
            dout.flush();
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    public void subscribe(String topic) {
        JSONObject data = initData("subscribe");
        data.getJSONObject("payload").put("topic", topic);
        sendToServer(data.toString());
    }

    public void unsubscribe(String topic) {
        JSONObject data = initData("unsubscribe");
        data.getJSONObject("payload").put("topic", topic);
        sendToServer(data.toString());
    }

    public void chat(String topic, String message) {
        JSONObject data = initData("chat");
        data.getJSONObject("payload").put("topic", topic);
        data.getJSONObject("payload").put("message", message);
        sendToServer(data.toString());
    }

    public void send_file(String topic, String filename) throws IOException {
        int filesize = (int) new File(filename).length();
        Global.sending = (int) Math.ceil((double) filesize / Global.BUFFER_SIZE);

        InputStream inp = new FileInputStream(new File(Global.FILEPATH + filename));

        byte[] data = new byte[Global.BUFFER_SIZE];
        int sent = 0;
        while (sent < filesize) {
            int need = Math.min(filesize - sent, Global.BUFFER_SIZE);
            data = inp.readNBytes(need);
            sent += need;
            Global.acked = false;
            //datOutStream.write(data);     // fixed
            dout.write(data);

            while (true) {
                if (Global.err_frag) {
                    return;
                }
                if (Global.acked) {
                    break;
                }
            }
        }
        inp.close();
        Global.sending = 0;
    }

    public void run() {
        try {
            Scanner scanner = new Scanner(System.in);

            while (true) {
                String inp = scanner.nextLine();

                if (Global.sending > 0) {
                    System.out.println("Server is currently busy");
                    continue;
                }
                String[] inputs = inp.split(" ", 3);

                try {
                    String action = inputs[0].toUpperCase();
                    if (action.equals("SUBSCRIBE")) {
                        // subscribe <topic>
                        subscribe(inputs[1]);
                    } else if (action.equals("UNSUBSCRIBE")) {
                        // unsubscribe <topic>
                        unsubscribe(inputs[1]);
                    } else if (action.equals("CHAT")) {
                        // chat <topic> <message>
                        chat(inputs[1], inputs[2]);
                    } else if (action.equals("FILE")) {
                        // file <topic> <filename>
                        send_file(inputs[1], inputs[2]);
                    } else {
                        System.out.println("Invalid input");
                    }
                }
                catch (ArrayIndexOutOfBoundsException | IOException e) {
                    System.out.println("Invalid input");
                }

            }
        } catch (Exception e) {
            System.out.println(e);
        }

        Global.closed = true;
    }
}

public class MyClient {
    public final static int DEFAULT_PORT = 5000;
    public static void main(String args[]) throws Exception {  
        Socket connection = new Socket("localhost", DEFAULT_PORT);  
        Thread recvThread = new RecvThread(connection.getInputStream());
        Thread sendThread = new SendThread(connection.getOutputStream());

        recvThread.start();
        sendThread.start();

        while (true) {
            if (Global.closed) {
                break;
            }
        }

        connection.close();  
    }
} 