package com.neuon.tools;

public class SafetyCheck {
    private String harmfulCommands[] = {"rm", "mv", "chmod", "chown", "dd", "mkfs", "shutdown", "reboot", "init", "poweroff", "halt", "kill", "killall", "pkill"};

    public boolean isSafe(String command) {
        String harmfulPattern = "(rm|mv|chmod|chown|dd|mkfs|shutdown|reboot|init|poweroff|halt|kill|killall|pkill)\\s+";
        return !command.matches(".*" + harmfulPattern + ".*");
    }

    // Determine if a command is likely to need interactive input
    
    public boolean isInteractive(String command) {
        String lower = command.toLowerCase();
        return lower.matches("^(sudo|ssh|passwd|mysql|psql|python|python3|bash|sh|zsh|cat\\s+>|tee\\s+|less|more|vi|vim|nano|ftp|telnet|su).*");
    }
}