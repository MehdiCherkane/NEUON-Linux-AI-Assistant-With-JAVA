package com.neuon.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

public class Runner {
    private Process activeProcess;
    private OutputStream activeStdin;

    // Non‑interactive (original)

    public ProcessResult execute(String command) {
        return execute(command, null);
    }
    public ProcessResult execute(String command, String workingDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            if (workingDir != null){
                File dir = new File(workingDir);
                if (!dir.exists() || !dir.isDirectory()) {
                    return new ProcessResult(-1, "", "Working directory does not exist: " + workingDir);
                }
                pb.directory(dir);
            } 
            Process process = pb.start();
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, stdout, stderr);
        } catch (Exception e) {
            e.printStackTrace();
            return new ProcessResult(-1, "", e.getMessage());
        }
    }

    // Interactive execution – non‑blocking


    public void executeInteractive(String command, ProcessHandler handler) {

        new Thread(() -> {
            try {
                // Use PTY instead of ProcessBuilder
                String[] cmdarray = { "bash", "-c", command };
                Process process = new PtyProcessBuilder(cmdarray)
                                        .setEnvironment(System.getenv())
                                        .setDirectory(null)
                                        .start();
                
                // Optional: set terminal size (80x24 is fine)
                if (process instanceof PtyProcess) {
                    ((PtyProcess) process).setWinSize(new WinSize(80, 24));
                }

                synchronized (this) {
                    activeProcess = process;
                    activeStdin = process.getOutputStream();
                }

    
                // read stdout, stderr, waitFor, etc.
                Thread outThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            handler.onOutput(line);
                        }
                    } catch (IOException ignored) {
                        handler.onOutput("Error occured while reading process I/O");
                    }
                });
                outThread.setDaemon(true);
                outThread.start();

                Thread errThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            handler.onError(line);
                        }
                    } catch (IOException ignored) {
                        handler.onError("Error occured while reading process I/O");
                    }
                });
                errThread.setDaemon(true);
                errThread.start();

                int exitCode = process.waitFor();
                handler.onExit(exitCode);
                synchronized (this) {
                    activeProcess = null;
                    activeStdin = null;
                }
            } catch (Exception e) {
                handler.onError(e.getMessage());
                handler.onExit(-1);
            }
        }).start();
    }

    public void sendInput(String input) {
        OutputStream out = activeStdin;
        if (out != null) {
            try {
                out.write((input + "\n").getBytes());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void killActiveProcess() {
        Process p = activeProcess;
        if (p != null && p.isAlive()) {
            p.destroyForcibly();
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            return output.toString().trim();
        }
    }

    // I use it if you don't want bash.
    private List<String> tokenize(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        boolean escaped = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (inQuotes) {
                if (c == quoteChar) {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"' || c == '\'') {
                inQuotes = true;
                quoteChar = c;
            } else if (Character.isWhitespace(c)) {
                if (current.length() > 0 || (i > 0 && (command.charAt(i-1) == '"' || command.charAt(i-1) == '\''))) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0 || (command.length() > 0 && (command.endsWith("\"") || command.endsWith("'")))) {
            tokens.add(current.toString());
        }
        tokens.replaceAll(token -> {
            if (token.startsWith("~/")) {
                return System.getProperty("user.home") + token.substring(1);
            }
            return token;
        });
        return tokens;
    }
}
