package com.fidesmo.gradle.plugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import jline.Terminal;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import com.fidesmo.sec.utils.Hex;
import nordpol.IsoCard;

/** Simple command line to communicate with card.
 *
 */
public class Console {

    public static void run() throws IOException {
        Terminal terminal = TerminalFactory.create();
        terminal.setEchoEnabled(true);
        ConsoleReader reader = new ConsoleReader("Fidesmo console",
                                                 System.in,
                                                 System.out,
                                                 terminal);
        reader.setHandleUserInterrupt(true);
        PrintWriter out = new PrintWriter(reader.getOutput());
        IsoCard card = (new SmartcardioTransceiver(out))
            .getCard()
            .toBlocking()
            .first();

        String line;
        try {
            while((line = reader.readLine("> ")) != null) {
                String[] tokens = line.split(" ");
                if (tokens[0].equals("send")) {
                    if (tokens.length < 2) {
                        out.println("To few arguments for send command");
                    }

                    byte[] apdu = Hex.decodeHex(tokens[1].toUpperCase());
                    card.transceive(apdu); // output displayed through log
                } else if (line.equals("exit")) {
                    card.close();
                    break;
                } else if (line.equals("help")) {
                    out.println("send <hex string>  -- send apdu to card");
                    out.println("help               -- display this message");
                    out.println("exit               -- exit this shell");
                } else {
                    out.println("Unknown command. Type 'exit' to leave.");
                }
            }
        } catch(UserInterruptException uie) {
            try {
                card.close();
            } catch(IOException ioe) {
                /* Ignore */
            }
        }
    }
}
