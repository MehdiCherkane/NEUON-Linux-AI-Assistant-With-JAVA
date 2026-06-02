package com.neuon.tools;

import java.util.Set;

public class SafetyCheck {

    private static final Set<String> HARMFUL_COMMANDS = Set.of(
        "rm", "mv", "chmod", "chown", "dd", "mkfs", "shutdown", "reboot",
        "init", "poweroff", "halt", "kill", "killall", "pkill", "sudo",
        "su", "passwd", "chattr", "swapoff", "swapon", "modprobe", "insmod",
        "rmmod", "iwconfig", "ifconfig", "route", "iptables"
    );

    private static final Set<String> INTERACTIVE_ROOT_COMMANDS = Set.of(
        "sudo", "ssh", "passwd", "mysql", "psql", "vi", "vim", "nano",
        "ftp", "telnet", "su", "less", "more", "htop", "top"
    );

    public boolean isSafe(String command) {
        if (command == null || command.isBlank()) return false;
        String firstToken = command.trim().split("\\s+")[0].replaceAll("^/", "");
        for (String harmful : HARMFUL_COMMANDS) {
            if (firstToken.equals(harmful) || firstToken.endsWith("/" + harmful)) {
                return false;
            }
        }
        return true;
    }

    public boolean isInteractive(String command) {
        if (command == null || command.isBlank()) return false;
        String firstToken = command.trim().split("\\s+")[0].replaceAll("^/", "");
        if (INTERACTIVE_ROOT_COMMANDS.contains(firstToken)) return true;
        if (firstToken.endsWith("sh") || firstToken.endsWith("bash") || firstToken.endsWith("zsh")) return true;
        if ((firstToken.equals("python") || firstToken.equals("python3")) && !command.contains("-c")) return true;
        return false;
    }
}