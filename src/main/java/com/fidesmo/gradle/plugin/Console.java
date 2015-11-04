package com.fidesmo.gradle.plugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import jline.Terminal;
import jline.TerminalFactory;
import jline.console.ConsoleReader;

import com.fidesmo.sec.utils.Hex;
import nordpol.IsoCard;

/** Simple command line to communicate with card.
 *
 */
public class Console {

    public void run() throws IOException {
        Terminal terminal = TerminalFactory.create();
        terminal.setEchoEnabled(true);
        ConsoleReader reader = new ConsoleReader("Fidesmo console",
                                                 System.in,
                                                 System.out,
                                                 terminal);

        IsoCard card = (new SmartcardioTransceiver())
            .getCard()
            .toBlocking()
            .first();

        String line;
        PrintWriter out = new PrintWriter(reader.getOutput());

        while((line = reader.readLine("> ")) != null) {
            String[] tokens = line.split(" ");
            if (tokens[0].equals("send")) {
                if (tokens.length < 2) {
                    out.println("To few arguments for send command");
                }

                byte[] apdu = Hex.decodeHex(tokens[1].toUpperCase());
                out.println(Hex.encodeHex(card.transceive(apdu)));
            } else if (line.equals("exit")) {
                break;
            } else if (line.equals("help")) {
                out.println("send <hex string>  -- send apdu to card");
                out.println("help               -- display this message");
                out.println("exit               -- exit this shell");
            } else {
                out.println("Unknown command. Type 'exit' to leave.");
            }
        }
    }
}
