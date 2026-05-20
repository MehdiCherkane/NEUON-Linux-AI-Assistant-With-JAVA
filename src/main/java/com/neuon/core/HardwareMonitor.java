package com.neuon.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class HardwareMonitor {
    
    private long lastCpuUser, lastCpuSys, lastCpuIdle, lastCpuTotal;
    private long lastNetRx, lastNetTx, lastNetTime;
    private long lastDiskRead, lastDiskWrite, lastDiskTime;
    
    // method for debugging
    public void start() {
        System.out.println("Mini Task Manager (Linux)\n");
        
        while (true) {
            System.out.printf("\rCPU: %s | RAM: %s | Disk: %s | Net: %s    ",
                getCpu(), getRam(), getDisk(), getNet());
            
            try { Thread.sleep(1000); } 
            catch (InterruptedException e) { break; }
        }
    }
    
    public String getCpu() {
        try {
            String[] parts = Files.lines(Paths.get("/proc/stat"))
                .filter(l -> l.startsWith("cpu "))
                .findFirst().get().trim().split("\\s+");
            
            long user = Long.parseLong(parts[1]) + Long.parseLong(parts[2]);
            long sys  = Long.parseLong(parts[3]);
            long idle = Long.parseLong(parts[4]);
            long total = user + sys + idle + Long.parseLong(parts[5]);
            
            long dTotal = total - lastCpuTotal;
            long dIdle  = idle - lastCpuIdle;
            
            lastCpuUser = user; lastCpuSys = sys; lastCpuIdle = idle; lastCpuTotal = total;
            
            return dTotal > 0 ? String.format("%d%%", (int)((dTotal - dIdle) * 100 / dTotal)) : "0%";
        } catch (Exception e) { return "?%"; }
    }
    
    public String getRam() {
        try {
            Map<String, Long> m = new HashMap<>();
            Files.lines(Paths.get("/proc/meminfo")).forEach(l -> {
                String[] p = l.split(":");
                if (p.length == 2) m.put(p[0].trim(), 
                    Long.parseLong(p[1].replaceAll("[^0-9]", "")) * 1024);
            });
            long total = m.get("MemTotal");
            long avail = m.getOrDefault("MemAvailable", m.get("MemFree"));
            return String.format("%d%%", (int)((total - avail) * 100 / total));
        } catch (Exception e) { return "?%"; }
    }
    
    public String getDisk() {
        try {
            // Find busiest disk from /proc/diskstats
            long totalRead = 0, totalWrite = 0;
            for (String line : Files.readAllLines(Paths.get("/proc/diskstats"))) {
                String[] p = line.trim().split("\\s+");
                if (p.length > 13 && !p[2].startsWith("loop") && !p[2].matches(".*\\d+")) {
                    totalRead += Long.parseLong(p[5]);   // read sectors
                    totalWrite += Long.parseLong(p[9]);  // write sectors
                }
            }
            
            long now = System.currentTimeMillis();
            long dt = now - lastDiskTime;
            String result = "0%";
            
            if (dt > 0 && lastDiskTime > 0) {
                // sectors are 512 bytes, convert to approximate % (simplified)
                double readMB = (totalRead - lastDiskRead) * 512.0 / 1024 / 1024;
                double writeMB = (totalWrite - lastDiskWrite) * 512.0 / 1024 / 1024;
                double mbps = (readMB + writeMB) / (dt / 1000.0);
                // Rough heuristic: 100MB/s = 100% for SATA SSD
                result = String.format("%d%%", Math.min((int)(mbps), 100));
            }
            
            lastDiskRead = totalRead; lastDiskWrite = totalWrite; lastDiskTime = now;
            return result;
            
        } catch (Exception e) { return "?%"; }
    }
    
    public String getNet() {
        try {
            long rx = 0, tx = 0;
            for (String line : Files.readAllLines(Paths.get("/proc/net/dev"))) {
                if (!line.contains(":") || line.trim().startsWith("lo:")) continue;
                String[] data = line.split(":")[1].trim().split("\\s+");
                rx += Long.parseLong(data[0]);
                tx += Long.parseLong(data[8]);
            }
            
            long now = System.currentTimeMillis();
            long dt = now - lastNetTime;
            String result = "0 Mbps";
            
            if (dt > 0 && lastNetTime > 0) {
                double speed = ((rx - lastNetRx) + (tx - lastNetTx)) * 8.0 / 1024 / 1024 / (dt / 1000.0);
                if (speed >= 1) result = String.format("%.1f Mbps", speed);
                else result = String.format("%.0f Kbps", speed * 1024);
            }
            
            lastNetRx = rx; lastNetTx = tx; lastNetTime = now;
            return result;
            
        } catch (Exception e) { return "? Mbps"; }
    }
    
}
