package com.neuon.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Set;

public class SafetyCheck {

    private static final Set<String> HARMFUL_COMMANDS = Set.of(
        "rm", "mv", "chmod", "chown", "dd", "mkfs", "shutdown", "reboot",
        "init", "poweroff", "halt", "kill", "killall", "pkill", "sudo",
        "su", "passwd", "chattr", "swapoff", "swapon", "modprobe", "insmod",
        "rmmod", "iwconfig", "ifconfig", "route", "iptables",
        "wget", "curl", "pkexec", "dpkg", "apt", "apt-get", "yum", "dnf",
        "systemctl", "journalctl", "crontab", "fdisk", "parted"
    );

    private static final Set<String> INSTALL_COMMANDS = Set.of(
        "apt", "apt-get", "yum", "dnf", "dpkg", "pip", "pip3", "npm", "gem", "cargo"
    );

    private static final Set<String> INTERACTIVE_ROOT_COMMANDS = Set.of(
        "sudo", "ssh", "passwd", "mysql", "psql", "vi", "vim", "nano",
        "ftp", "telnet", "su", "less", "more", "htop", "top"
    );

    private static final Path SAFETY_LOG = Path.of(
        System.getProperty("user.home", "."), ".neuon", "safety.log"
    );

    public boolean isSafe(String command) {
        if (command == null || command.isBlank()) return false;
        String firstToken = command.trim().split("\\s+")[0].replaceAll("^/", "");
        for (String harmful : HARMFUL_COMMANDS) {
            if (firstToken.equals(harmful) || firstToken.endsWith("/" + harmful)) {
                logCommand(command, false, "blocked: " + harmful);
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

    public boolean needsConfirmation(String command) {
        if (command == null || command.isBlank()) return false;
        String firstToken = command.trim().split("\\s+")[0].replaceAll("^/", "");
        if (HARMFUL_COMMANDS.contains(firstToken)) return true;
        if (INSTALL_COMMANDS.contains(firstToken)) return true;
        if (firstToken.endsWith("rm") || firstToken.contains("chmod") || firstToken.contains("chown")) return true;
        return false;
    }

    public boolean hasPathTraversal(String command) {
        if (command == null || command.isBlank()) return false;
        String[] tokens = command.trim().split("\\s+");
        for (String token : tokens) {
            if (token.contains("../") || token.contains("..\\")) {
                logCommand(command, false, "path traversal detected: " + token);
                return true;
            }
        }
        return false;
    }

    public void logCommand(String command, boolean approved, String detail) {
        try {
            Files.createDirectories(SAFETY_LOG.getParent());
            String line = String.format("[%s] approved=%b | cmd=%s | detail=%s%n",
                LocalDateTime.now().toString(), approved, command, detail);
            Files.writeString(SAFETY_LOG, line,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
