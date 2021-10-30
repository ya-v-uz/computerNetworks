import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Client {
  public static void main ( String... args ) throws IOException {

    String host;
    if (args.length != 1) {
        System.err.println("please add <port num>");
        System.exit(1);
}
        String usrname = null;
        String password= null;
        String token= null;
        int port = 9999;
        port = Integer.parseInt(args[0]); // parsing the port argument
        System.out.println("Binding to port localhost: " + port);
        host = "localhost";
        BufferedReader credentialsIn = new BufferedReader ( new InputStreamReader (System.in));
        System.out.printf("username:");
        usrname = credentialsIn.readLine();

    try (      
        Socket clientSocket = new Socket ( host, port );
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());
    ) 
    {
        BufferedReader stdIn = new BufferedReader ( new InputStreamReader (System.in));
        String fromServer, fromUser = "placeholder";
        
        
        System.out.println("-------------------------");
        System.out.println("connected to server!");
        System.out.println("-------------------------");
        out.writeUTF(usrname);
        
           
        while (in != null) {
            
            // Terminating the client in case of authentication fail
          
           char a = in.readChar();
           char b = in.readChar();

          
          if(a == '0' && b == '1') {
               System.out.println("server: enter password");
                fromUser = stdIn.readLine();
                byte[] dataInBytes = fromUser.getBytes(StandardCharsets.UTF_8);
                out.writeChar('0');
                out.writeChar('0');
                out.writeInt(dataInBytes.length);
                out.write(dataInBytes);
           }


           else if(a == '0' && b == '2') {
            int length = in.readInt();
            String fail_msg = decapsulateMessage(in, length);
            System.out.println("server: Authentication Fail! reason: " +  fail_msg);
            System.out.println("you got disconnected from server!");
            System.exit(-1);
           }


           else if(a == '0' && b == '3') {
            System.out.println("server: okay you are verified, sending you the token.");
            int length = in.readInt();
            token = decapsulateMessage(in, length);
            System.out.println("server: here's your token =" + token);
            out.writeChar('1');
            out.writeChar('9');
            out.writeInt(4);
           }

           else if (a == '1') { // Query Phase // first accept - then ask or not, for another Query // 
                System.out.println("starting query phase");
                break;
           }

           
           else{
           
           System.out.println("server: " + a + b);
            fromUser = stdIn.readLine();
            byte[] dataInBytes = fromUser.getBytes(StandardCharsets.UTF_8);
            out.writeChar(type);
            out.writeChar(type);
            out.writeInt(dataInBytes.length);
            out.write(dataInBytes);
        }
            

        
    }   

        // Query Phase

        // Initial query requst from the server 
        System.out.println("send query request please:");
        fromUser = stdIn.readLine();
        String encapsulated_query = appendToken(fromUser, token); 
        out.writeUTF(encapsulated_query);
        
        // Continious query requst from the server 
        while ((fromServer = in.readUTF()) != null) { 
            System.out.println("server:" + fromServer); // query return 
            System.out.println("send query request or exit please:");
            fromUser = stdIn.readLine();
            if(fromUser.equals("exit")){
                out.writeUTF("exit");
                break;
            }
            else {
            encapsulated_query = appendToken(fromUser, token);
            out.writeUTF(encapsulated_query);
            }
        }
        
        System.out.println("socket closed");    
    }
  }




public static String decapsulateMessage(DataInputStream into, int message_length) {
    byte[] messageByte = new byte[message_length];
    boolean end = false;
  
    StringBuilder message = new StringBuilder(message_length);
    int totalBytesRead = 0;
    try { 
        while(!end) {
        int currentBytesRead = into.read(messageByte);
        totalBytesRead = currentBytesRead + totalBytesRead;
        if(totalBytesRead <= message_length) {
            message.append(new String(messageByte, 0, currentBytesRead, StandardCharsets.UTF_8));
        } else {
            message.append(new String(messageByte, 0, message_length - totalBytesRead + currentBytesRead, StandardCharsets.UTF_8));
        }
        if(message.length() >= message_length) {
             end = true;
        }
    }
   
}
catch (IOException e) {
    e.printStackTrace();
}
return message.toString();
}


public static String appendToken(String msg, String token){
    String result = msg + "%" + token;
    return result;
}



}


