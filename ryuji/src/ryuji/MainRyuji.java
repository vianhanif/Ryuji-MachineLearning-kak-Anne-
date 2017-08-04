package ryuji;

//import org.trypticon.megahal.engine.Ryuji;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
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

    public String checkSocket() throws IOException {
        String message;
        do {
            message = client.getMessage();
        } while (message == null);
        return message;
    }

    public MainRyuji() throws IOException {
        echoSocket = new Socket(host, Integer.parseInt(portNumber));
        outPrint = new PrintWriter(echoSocket.getOutputStream(), true);
        inPrint = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
        stdIn = new BufferedReader(new InputStreamReader(System.in));

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
        boolean first = true;
        String name = null;
        String gender = null;
        String line = null;

        Ryuji ryuji = new Ryuji();
        Configuration configuration = new Configuration();

        // Set path to the acoustic model.
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        // Set path to the dictionary.
        configuration.setDictionaryPath("/home/jefri/ryuji-lm/2450.dic");
        // Set path to the language model.
        configuration.setLanguageModelPath("/home/jefri/ryuji-lm/2450.lm");

        //Recognizer object, Pass the Configuration object
        LiveSpeechRecognizer recognize = new LiveSpeechRecognizer(configuration);

        //Start Recognition Process (The bool parameter clears the previous cache if true)
        recognize.startRecognition(true);

        //Creating SpeechResult object
        SpeechResult result;

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out, true);
        TextToSpeechConverter ttsc = new TextToSpeechConverter();

        while (true) {
            if ((state.equals("socket")) && (name == null)) {
        if (first == true){
                    client.WriteMessage("REG;JAVA;");
                    first = false;
                    state = "wait_init";                    
                }
                name = checkSocket().toUpperCase();
                if (name.contains("MISS")){
                    name = name.replace("MISS", "MISS ");//"MISS "+name.substring(4);
                } else {
                    name = name.replace("MISTER", "MISTER ");//"MISTER "+name.substring(6);
                }
                state = "greeting";
            } else if (state.equals("greeting")) {
                out.print("RYUJI> ");
                if (!name.contains("USER NOT FOUND")) {
                    //call t2s
                    out.println(ryuji.greetings(name));
                    ttsc.speak(ryuji.greetings(name));
                } else {
                    //call t2s                    
                    out.println(ryuji.greetings(name));
                    ttsc.speak(ryuji.greetings(name));
                    name = "USER";
                }
                out.print("RYUJI> ");
                //call t2s
                out.println(ryuji.getIntro(gender));
                ttsc.speak(ryuji.getIntro(gender));

                state = "conversation";
            } else if (state.equals("conversation")) {
                if ((result = recognize.getResult()) != null) {
                    String command = result.getHypothesis();
                    
                    out.print(name + "> "+command);
                    out.flush();

                    //call s2t
                    //line = "find lintang".toUpperCase();
                    line = command.toUpperCase();
                    if(line.contains("FIND")||line.contains("VISION")||line.contains("GUNADARMA")||line.contains("RECTOR")&& line.length()>2){
                    if (ryuji.isCommand(line)) {
                        state = "command";
                    } else if (line.equals("NO THANKS")) {
                        state = "terminate";
                    } else {
                        state = "knowledge";
                    }
                    }
                }

            } else if (state.equals("command")){
                String [] command = ryuji.command(line);
                //send message to socket
                client.WriteMessage("DATA;"+command[1].replaceAll(" ", "")+";");
                
                name = null;
                state = "socket";
            }
            
            else if (state.equals("terminate")){
                //call t2s
                out.println("OK SEE YOU LATER");
                client.WriteMessage("EXIT");
                state = "socket";
            }
            
            else if (state.equals("knowledge")){
                String replyLine = ryuji.formulateReply(line);
                if (replyLine == null) {
                    replyLine = "I don't have enough information for answering it.";
                }
                
                //call t2s
                out.println("RYUJI> " + replyLine);
                ttsc.speak(replyLine);
                
                state = "conversation";
            }
        }

    }

}
