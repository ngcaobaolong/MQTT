package BTL.client;

import java.io.*;
import java.net.*;
import java.util.*;
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

    public void run() {
        try {
            BufferedReader bufferRead = new BufferedReader(new InputStreamReader(inpStream));

            String recvMsg;
            while (true) {
                if (Global.closed) {
                    break;
                }
                recvMsg = bufferRead.readLine();
                notiRecv(recvMsg);
                if (recvMsg.equals("200 READY")) {
                    solveRecvFile(bufferRead);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void solveRecvFile(BufferedReader buffRead) throws IOException {
        int num_frag = 0;
        while (true) {
            recvACKFrag(buffRead);
            num_frag++;
            if (Global.err_frag) {
                return;
            }
            if (num_frag == Global.sending) {
                break;
            }
        }
    }

    private void recvACKFrag(BufferedReader buffRead) throws IOException {
        String msg = buffRead.readLine();
        if (!msg.equals("ACK FRAG")) {
            System.out.println("Error when transferring data");
            Global.err_frag = true;
        } else {
            Global.acked = true;
        }
    }

    private static void notiRecv(String recvMsg) {
        System.out.println("Message received from server:  " + recvMsg);
    }
}

class SendThread extends Thread {
    OutputStream outStream;

    SendThread(OutputStream outStream) {
        this.outStream = outStream;
    }

    public void run() {
        try {
            Scanner scanner = new Scanner(System.in);
            DataOutputStream datOutStream = new DataOutputStream(outStream);

            String sendMsg;
            while (true) {
                sendMsg = scanner.nextLine();
                sendToServ(datOutStream, sendMsg);

                if (sendMsg.equals("@") || sendMsg.equals("QUIT")) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        Global.closed = true;
    }

    public JSONObject initData(String action) {
        JSONObject data = new JSONObject();
        JSONObject payload = new JSONObject();
        data.put("action", action);
        payload.put("username", Global.username);
        data.put("payload", payload);
        return data;
    }

    private void send_file(DataOutputStream datOutStream, String msg) throws IOException {
        String topic = "topicname";
        String filename = "file name";

        int filesize = 12;

        Global.sending = (int) Math.ceil((double) filesize / Global.BUFFER_SIZE);

        InputStream inp = new FileInputStream(new File(Global.FILEPATH + filename));
        byte[] data = new byte[Global.BUFFER_SIZE];
        int sent = 0;
        while (sent < filesize) {
            int need = Math.min(filesize - sent, Global.BUFFER_SIZE);
            data = inp.readNBytes(need);
            sent += need;
            Global.acked = false;
            datOutStream.write(data);

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

    private void recv_file(OutputStream outStream, String msg) throws IOException {

    }

    private void sendToServ(OutputStream outStream, String msg) throws IOException {
        DataOutputStream datOutStream = new DataOutputStream(outStream);
        datOutStream.writeBytes(msg + '\n');
    }
}

public class Client {
    static int PORT_NUMBER = 9000;

    public static void main(String[] args) throws IOException {
        Socket servAddr = new Socket("127.0.0.1", PORT_NUMBER);

        System.out.println("Connected to the msServer ....");

        Thread recvThread = new RecvThread(servAddr.getInputStream());
        Thread sendThread = new SendThread(servAddr.getOutputStream());

        recvThread.start();
        sendThread.start();

        while (true) {
            if (Global.closed) {
                break;
            }
        }

        servAddr.close();
    }
}