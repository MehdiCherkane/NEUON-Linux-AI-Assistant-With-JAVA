package com.neuon.tools;

import com.neuon.UI.*;
import com.google.gson.JsonObject;

public class ShellTool implements ToolHandler {

        private SafetyCheck safetyCheck = new SafetyCheck();
        private Runner runner = new Runner(); 
        private Interface userInterface = new FXInterface();

        @Override
        public String execute(JsonObject parameters) {
            if (parameters == null || !parameters.has("command") || parameters.get("command").isJsonNull()) {
                return "[ERROR] Missing 'command' parameter";
            }
            String command = parameters.get("command").getAsString().trim();
            if (command.isEmpty()) {
                return "[ERROR] Empty command";
            }

            if (safetyCheck.isInteractive(command)) return interactive(command);
            else return nonInteractive(command);
            
        }


        private String interactive(String command){

            userInterface.startInteractive(command, runner);
                runner.executeInteractive(command, new ProcessHandler() {
                    @Override
                    public void onOutput(String line) {
                        userInterface.sendOutput("[stdout] " + line);
                    }
                    @Override
                    public void onError(String line) {
                        userInterface.sendOutput("[stderr] " + line);
                    }

                    @Override
                    public void onExit(int exitCode) {
                        userInterface.sendOutput("Interactive process finished with exit code " + exitCode);
                        userInterface.endInteractive();
                    }
                });
            return "Interactive process launched. Output is being streamed to the user.";
        }

        private String nonInteractive(String command){
            if (safetyCheck.isSafe(command)) {
                userInterface.sendOutput("Command to execute: " + command);
                ProcessResult result = runner.execute(command, WorkspacePaths.root().toString());
                if (!result.getStdout().isBlank()) return result.getStdout();
                if (!result.getStderr().isBlank()) return result.getStderr();
                return "Exit code: " + result.getExitCode();

            } else {
                
                userInterface.sendOutput("Command to execute: " + command);
                boolean userConfirmation = userInterface.validateCommand(command);
                if (userConfirmation) {
                    ProcessResult result = runner.execute(command, WorkspacePaths.root().toString());
                    if (!result.getStdout().isBlank()) return result.getStdout();
                    if (!result.getStderr().isBlank()) return result.getStderr();
                    return "Exit code: " + result.getExitCode();

                } else {
                   return "Command execution cancelled by the user.";
                }
            }
        }
}
