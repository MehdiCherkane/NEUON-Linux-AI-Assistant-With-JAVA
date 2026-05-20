package com.neuon.tools;

public interface ProcessHandler {

    void onOutput(String line);
    void onError(String line);
    void onExit(int exitCode);

}
