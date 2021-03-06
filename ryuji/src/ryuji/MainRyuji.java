package ryuji;

//import org.trypticon.megahal.engine.Ryuji;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Main class primarily for testing.
 *
 * @author Trejkaz
 */
public class MainRyuji {

    /**
     * Main method.
     *
     * @param args command-line arguments (ignored.)
     */
    
    public final String host = "localhost";
    public final String portNumber = "1500";    
    public Client client;
    public Socket echoSocket;
    public PrintWriter outPrint;
    public BufferedReader inPrint;
    public BufferedReader stdIn;
    
    public static void main(String[] args) throws IOException {
        MainRyuji mainRyuji = new MainRyuji();        
       
        try {
            mainRyuji.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String checkSocket() throws IOException{
        String message;
        do {
            message = client.getMessage();
        } while (message == null);        
        return message;
    }

    public  MainRyuji() throws IOException{                
        echoSocket  = new Socket(host, Integer.parseInt(portNumber));
        outPrint    = new PrintWriter(echoSocket.getOutputStream(), true);
        inPrint     = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
        stdIn       = new BufferedReader(new InputStreamReader(System.in));
        
        client = new Client(outPrint, inPrint);    
    }
    
    /**
     * Runs the main program.
     *
     * @param name
     * @throws IOException primarily on errors initialising the system.
     */
    public void run() throws IOException {
        String state = "socket";
        String name = null;
        String gender = null;
        String line = null;
        
        Ryuji ryuji = new Ryuji();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out, true);

        while (true) {
            if ((state.equals("socket")) && (name == null)){                
                name = checkSocket().replaceAll("\\.", " ").toUpperCase();                                
                state = "greeting";
            }
            
            else if (state.equals("greeting")){
                out.print("RYUJI> ");
                if (!name.contains("USER NOT FOUND")){                    
                    //call t2s
                    out.println(ryuji.greetings(name));
                } else {
                    //call t2s                    
                    out.println(ryuji.greetings(name));
                    name = "USER";
                }
                out.print("RYUJI> ");
                //call t2s
                out.println(ryuji.getIntro(gender));                
                
                state = "conversation";
            }
            
            else if (state.equals("conversation")){
                out.print(name+"> ");
                out.flush();

                //call s2t
                //line = "find lintang".toUpperCase();
                line = in.readLine().toUpperCase();
                
                if (ryuji.isCommand(line)){
                    state = "command";
                } else if (line.equals("NO THANKS")){
                    state = "terminate";
                } else{
                    state = "knowledge";
                }
            }
            
            else if (state.equals("command")){
                String [] command = ryuji.command(line);
                //send message to socket
                client.WriteMessage(command[1].replaceAll(" ", "\\."));
                
                name = null;
                state = "socket";
            }
            
            else if (state.equals("terminate")){
                //call t2s
                out.println("OK SEE YOU LATER");
                client.WriteMessage("EXIT");
                state = "idle";
            }
            
            else if (state.equals("idle")){
                
            }
            
            else if (state.equals("knowledge")){
                String replyLine = ryuji.formulateReply(line);
                if (replyLine == null) {
                    replyLine = "I don't have enough information for answering it.";
                }
                
                //call t2s
                out.println("RYUJI> " + replyLine);
                
                state = "conversation";
            }
        }
        
    }

}