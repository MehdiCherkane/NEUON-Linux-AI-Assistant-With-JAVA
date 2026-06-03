package com.neuon.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern SUBSHELL_PATTERN = Pattern.compile(
        "\\$\\(([^)]*)\\)|`([^`]*)`"
    );

    private static final Pattern PIPE_PATTERN = Pattern.compile(
        "\\|\\s*(\\S+)"
    );

    private static final Path SAFETY_LOG = Path.of(
        System.getProperty("user.home", "."), ".neuon", "safety.log"
    );

    private String firstToken(String command) {
        return command.trim().split("\\s+")[0].replaceAll("^/", "");
    }

    public boolean isSafe(String command) {
        if (command == null || command.isBlank()) return false;
        String first = firstToken(command);
        if (containsHarmfulCommand(first)) {
            logCommand(command, false, "blocked: " + first);
            return false;
        }
        if (containsSubshellHarmful(command)) {
            return false;
        }
        return true;
    }

    public boolean isInteractive(String command) {
        if (command == null || command.isBlank()) return false;
        String first = firstToken(command);
        if (INTERACTIVE_ROOT_COMMANDS.contains(first)) return true;
        if (first.endsWith("sh") || first.endsWith("bash") || first.endsWith("zsh")) return true;
        if ((first.equals("python") || first.equals("python3")) && !command.contains("-c")) return true;
        // Also check subshells and pipes for interactive commands
        String inner = extractSubshellContents(command);
        if (inner != null) {
            String innerFirst = firstToken(inner);
            if (INTERACTIVE_ROOT_COMMANDS.contains(innerFirst)) return true;
            if (innerFirst.endsWith("sh") || innerFirst.endsWith("bash") || innerFirst.endsWith("zsh")) return true;
        }
        return false;
    }

    public boolean needsConfirmation(String command) {
        if (command == null || command.isBlank()) return false;
        String first = firstToken(command);
        if (HARMFUL_COMMANDS.contains(first)) return true;
        if (INSTALL_COMMANDS.contains(first)) return true;
        if (first.endsWith("rm") || first.contains("chmod") || first.contains("chown")) return true;
        // Also check subshells and commands after pipes
        if (containsSubshellHarmful(command)) return true;
        String inner = extractSubshellContents(command);
        if (inner != null) {
            String innerFirst = firstToken(inner);
            if (INSTALL_COMMANDS.contains(innerFirst)) return true;
        }
        Matcher pipeMatcher = PIPE_PATTERN.matcher(command);
        while (pipeMatcher.find()) {
            String piped = pipeMatcher.group(1);
            if (HARMFUL_COMMANDS.contains(piped) || INSTALL_COMMANDS.contains(piped)) return true;
        }
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

    private boolean containsHarmfulCommand(String token) {
        if (token == null || token.isEmpty()) return false;
        String clean = token.replaceAll("^/", "");
        for (String harmful : HARMFUL_COMMANDS) {
            if (clean.equals(harmful) || clean.endsWith("/" + harmful)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSubshellHarmful(String command) {
        Matcher m = SUBSHELL_PATTERN.matcher(command);
        while (m.find()) {
            String inner = m.group(1) != null ? m.group(1) : m.group(2);
            if (inner == null) continue;
            String[] innerTokens = inner.trim().split("\\s+");
            if (innerTokens.length > 0) {
                String innerFirst = innerTokens[0].replaceAll("^/", "");
                if (containsHarmfulCommand(innerFirst)) {
                    logCommand(command, false, "blocked (subshell): " + innerFirst);
                    return true;
                }
            }
        }
        // Also check commands after pipes
        Matcher pipeMatcher = PIPE_PATTERN.matcher(command);
        while (pipeMatcher.find()) {
            String piped = pipeMatcher.group(1).replaceAll("^/", "");
            if (containsHarmfulCommand(piped)) {
                logCommand(command, false, "blocked (pipe): " + piped);
                return true;
            }
        }
        return false;
    }

    private String extractSubshellContents(String command) {
        Matcher m = SUBSHELL_PATTERN.matcher(command);
        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2);
        }
        return null;
    }
}
