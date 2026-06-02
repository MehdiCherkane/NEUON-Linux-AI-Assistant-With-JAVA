package com.neuon.tools;

import com.neuon.UI.*;
import com.google.gson.JsonObject;
import java.util.concurrent.CountDownLatch;

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

            if (safetyCheck.hasPathTraversal(command)) {
                return "[ERROR] Command rejected because it contains path traversal.";
            }

            boolean approvedByUser = false;
            if (safetyCheck.needsConfirmation(command)) {
                approvedByUser = confirm(command);
            }

            if (safetyCheck.needsConfirmation(command) && !approvedByUser) {
                return "Command execution cancelled by the user.";
            }

            if (safetyCheck.isInteractive(command)) return interactive(command);
            else return nonInteractive(command, approvedByUser);
            
        }


        private String interactive(String command) {
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            final int[] exitCodeRef = new int[1];
            CountDownLatch done = new CountDownLatch(1);

            userInterface.startInteractive(command, runner);
            runner.executeInteractive(command, new ProcessHandler() {
                @Override
                public void onOutput(String line) {
                    if (line != null) {
                        stdout.append(line).append("\n");
                        userInterface.appendStdout(line);
                    }
                }
                @Override
                public void onError(String line) {
                    if (line != null) {
                        stderr.append(line).append("\n");
                        userInterface.appendStderr(line);
                    }
                }

                @Override
                public void onExit(int exitCode) {
                    exitCodeRef[0] = exitCode;
                    userInterface.sendOutput("Interactive process finished with exit code " + exitCode);
                    userInterface.endInteractive();
                    done.countDown();
                }
            });

            try {
                done.await();
            } catch (InterruptedException e) {
                return "[ERROR] Interactive process interrupted";
            }

            return "exit_code: " + exitCodeRef[0]
                + "\nstdout:\n" + stdout.toString().trim()
                + "\nstderr:\n" + stderr.toString().trim();
        }

        private String nonInteractive(String command, boolean approvedByUser){
            if (approvedByUser || safetyCheck.isSafe(command)) {
                userInterface.sendOutput("Command to execute: " + command);
                ProcessResult result = runner.execute(command, WorkspacePaths.root().toString());
                safetyCheck.logCommand(command, true, "exit_code=" + result.getExitCode());
                userInterface.appendStdout(result.getStdout());
                userInterface.appendStderr(result.getStderr());
                return formatResult(result);

            } else {
                
                userInterface.sendOutput("Command to execute: " + command);
                if (confirm(command)) {
                    ProcessResult result = runner.execute(command, WorkspacePaths.root().toString());
                    safetyCheck.logCommand(command, true, "exit_code=" + result.getExitCode());
                    userInterface.appendStdout(result.getStdout());
                    userInterface.appendStderr(result.getStderr());
                    return formatResult(result);

                } else {
                   return "Command execution cancelled by the user.";
                }
            }
        }

        private boolean confirm(String command) {
            userInterface.sendOutput("Command to execute: " + command);
            boolean approved = userInterface.validateCommand(command);
            safetyCheck.logCommand(command, approved, approved ? "approved by user" : "denied by user");
            return approved;
        }

        private String formatResult(ProcessResult result) {
            return "exit_code: " + result.getExitCode()
                + "\nstdout:\n" + result.getStdout()
                + "\nstderr:\n" + result.getStderr();
        }
}
