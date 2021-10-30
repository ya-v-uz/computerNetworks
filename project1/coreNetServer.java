import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class coreNetServer extends Thread {
        
        // definitions for printing colored text on terminal
        public static final String ANSI_RESET = "\u001B[0m";
        public static final String ANSI_RED = "\u001B[31m";
        // main 
        public static void main(String... args) throws IOException {

        int port = 9999; //defualt port
        int clientCount = 0;
        
       
        ServerSocket server = null;

        try {
           // int randomPortNum = (int) (Math.random() * (upper - lower)) + lower;
			// server is listening on port 1234
			server = new ServerSocket(port);
			server.setReuseAddress(true);

			// running infinite loop for getting
			// client request
			while (clientCount < 10) {

				// socket object to receive incoming client
				// requests
				Socket client = server.accept();

				// Displaying that new client is connected
				// to server
				System.out.println("[" + clientCount + "]" + " New client connected from "+ client.getInetAddress().getHostAddress());

				// create a new thread object
				ClientHandler clientSock = new ClientHandler(client, clientCount);

				// This thread will handle the client
				// separately
				new Thread(clientSock).start();
                clientCount++;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}             
  }
 
 // ClientHandler class for creating Thread runnable()
 private static class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final int clientNumber;
    private boolean auth_check;
    private boolean auth_fail;
    private boolean password_check = false;

    // Constructor
    public ClientHandler(Socket socket, int clientNumber)
    {
        this.clientSocket = socket;
        this.clientNumber = clientNumber;

    }

    public void run()
    {
        DataOutputStream out = null;
        DataInputStream in = null;
        auth_check = false;
        auth_fail = false;
        int password_attempts = 2;
        String reason_of_failure = "Username doesn't exist";
       
        try {
                
            out = new DataOutputStream(clientSocket.getOutputStream());
            in = new DataInputStream(clientSocket.getInputStream());

            String clientHashToken = null;
            String clientUsrname = null;
            String fromUser, fromServer = "placeholder";
           // out.writeUTF("welcome to server! please provide your username:");
           
            // || Username Authentication || \\
            fromUser = in.readUTF(); // first read from socket
            String userName = "placeholder";
            File file=new File("./credentials.txt");
            BufferedReader br=new BufferedReader(new FileReader(file));
            String st;
            boolean usrname_found = false;
            String correct_password = null;
            // readLine from credentials.txt file, save password if a username matches
            while(!usrname_found && (st=br.readLine()) != null){
            if( st.equals(fromUser) ) { usrname_found = true; correct_password = br.readLine(); break;}
            }
            
            if(usrname_found) {
            clientUsrname = fromUser;
            auth_check = true;
            System.out.println("waiting password authentication from [" + clientNumber + "]" + clientUsrname);
            out.writeChar('0');
            out.writeChar('1');
            
        }
            else{
                out.writeChar('0');
                out.writeChar('2');
                reason_of_failure = "Username doesn't exist";
                byte[] dataInBytes = reason_of_failure.getBytes(StandardCharsets.UTF_8); 
                out.writeInt(dataInBytes.length);
                out.write(dataInBytes);
                auth_fail = true;
            }
        
            // if Username is found on the database, Server will listen to client
            // and proceed to password authentication

            if(!auth_fail) { // only happens if authentication isn't failed
            // Server/Client Communication 
            
            while (in != null) {
               
                char phase = in.readChar();
                char challenge = in.readChar();
                int length = in.readInt();
     

                if (phase == '1') {   //Query Phase
               
                    System.out.println("Query Phase is starting!");
                    out.writeChar('1');
                    out.writeChar('9');
                    break;
                           
                }
           
               else if(phase == '0' && challenge == '0') {
                String password_try = decapsulateMessage(in,length);
                System.out.println( "[" + clientNumber + "]client sent password:" + password_try );
                reason_of_failure = "Incorrect Password";
                byte[] dataInBytes = reason_of_failure.getBytes(StandardCharsets.UTF_8);   

                   // do password check, if true: 
                   if(password_try.equals(correct_password)) {
                    System.out.println("[" + clientNumber + "] has secured connection");
                    clientHashToken = generateHashToken(clientUsrname);
                    dataInBytes = clientHashToken.getBytes(StandardCharsets.UTF_8);
                    out.writeChar('0');
                    out.writeChar('3');
                    out.writeInt(dataInBytes.length);
                    out.write(dataInBytes);
                }
                  
                else if(password_attempts == 0) {  // failed password authentication 
                     out.writeChar('0');
                     out.writeChar('2');
                     dataInBytes = reason_of_failure.getBytes(StandardCharsets.UTF_8); 
                     out.writeInt(dataInBytes.length);
                     out.write(dataInBytes);
                    auth_fail = true;
                    break;
                  }
  
                  else{
                    out.writeChar('0');
                    out.writeChar('1');
                    password_attempts--;
                  }
            
                }
                else{
                System.out.println("kanka server else'de patladık veya thread'in işi bitmiş de olabilir");
                }
            }
            
            BufferedReader stdIn = new BufferedReader ( new InputStreamReader (System.in));
            String query = "title:medicine and year:2014";
            
            while ((fromUser = in.readUTF()) != null) { // Query Phase
                if(fromUser.equals("exit")) {
                System.out.println("client left the server!");
                out.writeUTF("exit");
                break;
            }
                if (!validateMessage(fromUser, clientHashToken)){
                    String err = "token is corrupted for ["+ clientNumber +"], connection is being ended";
                    System.out.println(err);
                    out.writeUTF("corrupt_token");
                    break;
                } //check wheter fromUser message appended token is identical to server stored token 
                
                fromUser = deTokenizeMessage(fromUser);
                System.out.println("client: " + fromUser);
                String[] QueryAndTypeArray = fromUser.split(" ", 2);
                char query_type = QueryAndTypeArray[0].charAt(1);
                query = QueryAndTypeArray[1];
               // add query result here 
                String newUrlString = constructURL(query, query_type);
                URL u = new URL(newUrlString);
                URLConnection conn = u.openConnection();
                InputStream is = conn.getInputStream();
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();
                String inputStr;
                while ((inputStr = streamReader.readLine()) != null) {
                // System.out.println(inputStr);
                    responseStrBuilder.append(inputStr);
                } 
                System.out.println("Query results for " + fromUser + " has been sent to client[" + clientNumber + "]" );
                if(query_type == 'a'){
                    out.writeUTF(yavuzArticleParser(responseStrBuilder.toString()));
                }
                else if(query_type == 'j') {
                    out.writeUTF(yavuzJournalParser(responseStrBuilder.toString()));
                }
                else{
                    out.writeUTF(" illegal query arguement, correct format: \n -a / -j \"query\" \n please try again:");
                }
            }
            System.out.println("bu thread[" + clientNumber + "] bitmiş");
        }
       
        // Terminate the Thread (Either Auth_fail or Client Left)
        else{
            out.writeUTF("auth_fail");
            System.out.println("Client [" + clientNumber +  "] lost connection!");
            clientSocket.close();
        }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                    clientSocket.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

public static String generateHashToken(String usrname) {
    return (usrname + "68646"); 

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

public static String deTokenizeMessage(String msg){
    String[] parts = msg.split("%");
    String decapsulated_msg = parts[0];
    return decapsulated_msg; 
}

public static Boolean validateMessage(String clientMsg, String OriginalToken) {
    String[] parts = clientMsg.split("%");
    String token = parts[1];
    return (token.equals(OriginalToken));
}

public static String constructURL(String query, char type){
    
    String newurl = query.replace(":" , "%3A");
    String newnewurl = newurl.replace(" ", "%20");
    String finalUrl = null;
   
    if(type == 'j'){
    String journalFinalEndUrl = newnewurl + ("?page=1&pageSize=10&apiKey=TKmDGqcIL1XxR3B6a8uEY2F9yCowjbgS");
    finalUrl = "https://core.ac.uk:443/api-v2/journals/search/" + journalFinalEndUrl;
   }
   else{  //else if(type == 'a') or unvalid. in which case, do an article search anyway.
    String ArticalFinalEndUrl = newnewurl + ("?page=1&pageSize=10&metadata=true&fulltext=false&citations=false&similar=false&duplicate=false&urls=false&faithfulMetadata=false&apiKey=TKmDGqcIL1XxR3B6a8uEY2F9yCowjbgS");
    finalUrl = "https://core.ac.uk:443/api-v2/articles/search/" + ArticalFinalEndUrl;
   }
    return finalUrl;

}
// parses article JSON from Core API in title:topics:year format. May not translate char's out of UTF8
public static String yavuzArticleParser(String JSON){
    String[] b = JSON.split(",\"|\":");
    StringBuilder responseFormatter = new StringBuilder();
    int size = b.length;
    String a = null;
    String dummy = null;
    int count = 1;


    for(int i = 0; i<size; i++) {
        a= b[i];
        if(a.equals("title") ){
            dummy = count + "-title: " + b[i+1] + "\n";
            responseFormatter.append(dummy);
            count++;
        }
        else if(a.equals("topics")){
            int x=2;
            dummy = "topics: " + b[i+1] + "\n";
            responseFormatter.append(dummy);
                while(!b[i+x].equals("types")){
                    dummy =  b[i+x] + "\n";
                    responseFormatter.append(dummy);
                    x++;
                }
        }
        else if(a.equals("year") ){
            dummy = "year: " + b[i+1] + "\n--------------------\n";
            responseFormatter.append(dummy);
        }
        
    }  
    
    return responseFormatter.toString();
}

// less elegant parser for journal search queries
public static String yavuzJournalParser(String JSON){
    String[] b = JSON.split(",\"|\":|\\{\"");
    StringBuilder responseFormatter = new StringBuilder();
    int size = b.length;
    String a = null;
    String dummy = null;
    int count = 1;
 
   for(int i = 0; i<size; i++) {
        a= b[i];
        
        if(a.equals("title") ){ 
            dummy = count + "-title: " + b[i+1] + "\n"; 
            responseFormatter.append(dummy);
            count++;
        }
    }   
    return responseFormatter.toString();
}



}




