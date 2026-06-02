package com.neuon.UI;
import com.neuon.tools.*;

import java.util.Scanner;

// every I/O should go through this class, to be easlly replaceable in the future with voice or other interfaces.
public class Interface {
    private String prompt;
    private Scanner sc = new Scanner(System.in);
    
    // those method are just for the CLI
    public String getPrompt(){
            System.out.println("How can I serve you sir?");
            System.out.print("---> ");
            this.prompt = sc.nextLine();
            return prompt;
    }
    //this mehtod is used to validate the command before executing it, it will ask the user if they are sure they want to execute the command if it is a harmful command
    public boolean validateCommand(String command){
        System.out.print("This may be a harmful command, are you sure you want to execute it? (yes/no): ");
        while (true) {
            String response = sc.nextLine();
            if (response.equalsIgnoreCase("yes")) {
                return true;
            } else if (response.equalsIgnoreCase("no")) {
                return false;
            } else {
                System.out.print("Please enter 'yes' or 'no': ");
            }
        }
    }
    // this method is used to send the output to the user (CLI)

    public void sendOutput(String output){
        System.out.println(output);
    }
    public void endInteractive(){
        System.out.println("Goodbye sir, have a nice day!");
    }
    public void startInteractive(String command, Runner runner){
        // for CLI.
        System.out.println("Interactive mode started. ");
    }

    public void appendStdout(String text) {
        System.out.println(text);
    }

    public void appendStderr(String text) {
        System.err.println(text);
    }

    public void setToolCall(String text) {
        System.out.println("[TOOL] " + text);
    }

}
