package desktop.app.desktopassistant.command;

import java.util.Arrays;

public class CommandParser {
    public static void main(String[] args){
        if(args.length == 0) {
            System.out.println("No command provided.");
            return;
        }

        String command = args[0].toLowerCase();
        System.out.println(Arrays.toString(args));
    }
}
