package com.neuon.UI;

import javazoom.jl.player.Player;  // from jl1.0.1.jar
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class VoiceHandler {

    // ── AssemblyAI (for Speech-to-Text) ──────────────────────────────────────
    static final String ASSEMBLYAI_API_KEY = System.getenv("ASSEMBLYAI_API_KEY");
    static final float  SAMPLE_RATE        = 16000f;
    static final double ENERGY_THRESHOLD   = 100000000;
    static final int    SILENCE_LIMIT_MS   = 2000;
    static final int    MAX_RECORD_SECS    = 60;
    static final int    CONNECT_TIMEOUT_MS = 15000;
    static final int    READ_TIMEOUT_MS    = 30000;
    private static final Gson gson = new Gson();

    // ── eidosSpeech (for Text-to-Speech) ────────────────────────────────────
    static final String EIDOS_API_KEY = System.getenv("EIDOS_API_KEY");
    static final String EIDOS_VOICE   = "en-US-AndrewNeural"; 
    static final String EIDOS_ENDPOINT = "https://eidosspeech.xyz/api/v1/tts";

    // Speech-to-text

    public String recordAndTranscribe() {
        if (ASSEMBLYAI_API_KEY == null || ASSEMBLYAI_API_KEY.isEmpty()) {
            System.err.println("AssemblyAI API key not set.");
            return null;
        }

        TargetDataLine mic = null;
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            mic = (TargetDataLine) AudioSystem.getLine(info);
            mic.open(format);
            mic.start();


            byte[] buffer = new byte[4096];
            ByteArrayOutputStream audioData = new ByteArrayOutputStream();
            long silenceSince = -1;
            boolean speechDetected = false;
            long recordingStart = System.currentTimeMillis();
            long safetyDeadline = recordingStart + (MAX_RECORD_SECS * 1000L);

            while (System.currentTimeMillis() < safetyDeadline) {
                int n = mic.read(buffer, 0, buffer.length);
                if (n <= 0) break;
                audioData.write(buffer, 0, n);

                double energy = 0;
                int samplesProcessed = n / 2;
                for (int i = 0; i < samplesProcessed * 2; i += 2) {
                    short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                    energy += (double) sample * sample;
                }
                if (samplesProcessed > 0) energy /= samplesProcessed;

                if (energy > ENERGY_THRESHOLD) {
                    speechDetected = true;
                    silenceSince = -1;
                } else {
                    if (speechDetected) {
                        if (silenceSince == -1) silenceSince = System.currentTimeMillis();
                        long silenceDuration = System.currentTimeMillis() - silenceSince;
                        if (silenceDuration >= SILENCE_LIMIT_MS) break;
                    }
                }
            }

            mic.stop();
            mic.close();
            mic = null;

            if (!speechDetected) {
                return null;
            }

            byte[] rawPcm = audioData.toByteArray();
            byte[] wavBytes = toWav(rawPcm, (int) SAMPLE_RATE, 1, 16);

            String audioUploadUrl = uploadAudio(wavBytes);
            if (audioUploadUrl == null || audioUploadUrl.isEmpty()) {
                System.err.println("Upload failed.");
                return null;
            }

            String transcriptId = requestTranscription(audioUploadUrl);
            if (transcriptId == null || transcriptId.isEmpty()) {
                System.err.println("Transcription request failed.");
                return null;
            }

            return pollForResult(transcriptId);

        } catch (Exception e) {
            System.err.println("Error during recording/transcription: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (mic != null) {
                mic.stop();
                mic.close();
            }
        }
    }

    // ── Upload audio to AssemblyAI ───────────────────────────────────────────

    private String uploadAudio(byte[] wavBytes) throws Exception {
        URL uploadUrl = new URL("https://api.assemblyai.com/v2/upload");
        HttpURLConnection uploadConn = (HttpURLConnection) uploadUrl.openConnection();
        uploadConn.setRequestMethod("POST");
        uploadConn.setRequestProperty("Authorization", ASSEMBLYAI_API_KEY);
        uploadConn.setRequestProperty("Content-Type", "application/octet-stream");
        uploadConn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        uploadConn.setReadTimeout(READ_TIMEOUT_MS);
        uploadConn.setDoOutput(true);

        try (OutputStream os = uploadConn.getOutputStream()) {
            os.write(wavBytes);
        }

        String uploadResponse = readResponse(uploadConn);
        uploadConn.disconnect();
        JsonObject uploadJson = parseJson(uploadResponse);
        return getAsString(uploadJson, "upload_url");
    }

    // ── Request transcription ────────────────────────────────────────────────

    private String requestTranscription(String audioUploadUrl) throws Exception {
        URL transcriptUrl = new URL("https://api.assemblyai.com/v2/transcript");
        HttpURLConnection transcriptConn = (HttpURLConnection) transcriptUrl.openConnection();
        transcriptConn.setRequestMethod("POST");
        transcriptConn.setRequestProperty("Authorization", ASSEMBLYAI_API_KEY);
        transcriptConn.setRequestProperty("Content-Type", "application/json");
        transcriptConn.setRequestProperty("Accept", "application/json");
        transcriptConn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        transcriptConn.setReadTimeout(READ_TIMEOUT_MS);
        transcriptConn.setDoOutput(true);

        String transcriptBody = "{\"audio_url\": \"" + escapeJson(audioUploadUrl) + "\", \"speech_models\": [\"universal-2\"]}";
        try (OutputStream os = transcriptConn.getOutputStream()) {
            os.write(transcriptBody.getBytes(StandardCharsets.UTF_8));
        }

        String transcriptResponse = readResponse(transcriptConn);
        transcriptConn.disconnect();
        JsonObject transcriptJson = parseJson(transcriptResponse);
        return getAsString(transcriptJson, "id");
    }

    // ── Poll for transcription result ────────────────────────────────────────

    private String pollForResult(String transcriptId) throws Exception {
        String status = "processing";
        String result = null;

        while (status.equals("processing") || status.equals("queued")) {
            Thread.sleep(1500);

            URL pollUrl = new URL("https://api.assemblyai.com/v2/transcript/" + transcriptId);
            HttpURLConnection pollConn = (HttpURLConnection) pollUrl.openConnection();
            pollConn.setRequestMethod("GET");
            pollConn.setRequestProperty("Authorization", ASSEMBLYAI_API_KEY);
            pollConn.setRequestProperty("Accept", "application/json");
            pollConn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            pollConn.setReadTimeout(READ_TIMEOUT_MS);

            String pollResponse = readResponse(pollConn);
            pollConn.disconnect();
            JsonObject pollJson = parseJson(pollResponse);
            status = getAsString(pollJson, "status");

            if (status == null) {
                System.err.println("Unexpected response: " + pollResponse);
                return null;
            }

            if (status.equals("completed")) {
                result = getAsString(pollJson, "text");
            } else if (status.equals("error")) {
                String errorMsg = getAsString(pollJson, "error");
                System.err.println("AssemblyAI error: " + errorMsg);
                return null;
            }
        }

        if (result == null || result.isEmpty()) return null;
        return result;
    }

    // ── Build a proper WAV file from raw PCM bytes ───────────────────────────

    byte[] toWav(byte[] pcm, int sampleRate, int channels, int bitDepth) throws Exception {
        int byteRate   = sampleRate * channels * bitDepth / 8;
        int blockAlign = channels * bitDepth / 8;
        int dataSize   = pcm.length;
        int paddedDataSize = dataSize;
        if (bitDepth == 16 && (dataSize % 2) != 0) {
            paddedDataSize = dataSize + 1;
        }
        int chunkSize  = 36 + paddedDataSize;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(chunkSize));
        dos.writeBytes("WAVE");
        dos.writeBytes("fmt ");
        dos.writeInt(Integer.reverseBytes(16));
        dos.writeShort(Short.reverseBytes((short) 1));
        dos.writeShort(Short.reverseBytes((short) channels));
        dos.writeInt(Integer.reverseBytes(sampleRate));
        dos.writeInt(Integer.reverseBytes(byteRate));
        dos.writeShort(Short.reverseBytes((short) blockAlign));
        dos.writeShort(Short.reverseBytes((short) bitDepth));
        dos.writeBytes("data");
        dos.writeInt(Integer.reverseBytes(paddedDataSize));
        dos.write(pcm);
        if (paddedDataSize > dataSize) {
            dos.writeByte(0);
        }
        dos.flush();
        return out.toByteArray();
    }

    // ── Read HTTP response ───────────────────────────────────────────────────

    String readResponse(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();
        InputStream is;
        if (status >= 200 && status < 300) {
            is = conn.getInputStream();
        } else {
            is = conn.getErrorStream();
        }
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    // ── Parse JSON ───────────────────────────────────────────────────────────
    JsonObject parseJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return gson.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            System.err.println("Failed to parse JSON response: " + e.getMessage());
            return null;
        }
    }

    static String getAsString(JsonObject obj, String memberName) {
        if (obj == null || !obj.has(memberName) || obj.get(memberName).isJsonNull()) return null;
        try {
            return obj.get(memberName).getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    // ── Escape a string for safe JSON inclusion ──────────────────────────────\

     private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // TEXT-TO-SPEECH (TTS) - eidosSpeech (Free, Microsoft Edge TTS)
    // Endpoint: POST https://eidosspeech.xyz/api/v1/tts
    // Returns: MP3 audio directly


    public boolean speak(String text) {
        if (EIDOS_API_KEY == null || EIDOS_API_KEY.isEmpty()) {
            System.err.println("eidosSpeech API key not set.");
            return false;
        }
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        try {
            // Step 1: Request TTS from eidosSpeech (returns MP3)
            byte[] mp3Bytes = requestEidosTTS(text);
            if (mp3Bytes == null || mp3Bytes.length == 0) {
                System.err.println("TTS returned empty audio.");
                return false;
            }

            System.out.println("Received " + mp3Bytes.length + " bytes from eidosSpeech");

            // step 2: save to temps and play using javazoom
            playMp3Stream(mp3Bytes);
            return true;

        } catch (Exception e) {
            System.err.println("TTS failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private byte[] requestEidosTTS(String text) throws Exception {
        URL url = new URL(EIDOS_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        //eidosSpeech uses X-API-Key header, NOT Authorization: Bearer
        conn.setRequestProperty("X-API-Key", EIDOS_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "audio/mpeg");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setDoOutput(true);

        // eidosSpeech request body
        String jsonBody = "{" +
            "\"text\": \"" + escapeJson(text) + "\"," +
            "\"voice\": \"" + EIDOS_VOICE + "\"," +
            "\"format\": \"mp3\"," +
            "\"stream_format\": \"sse\"" + 
        "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();

        // Check for errors first
        if (status >= 400) {
            String errorBody = readResponse(conn);
            throw new IOException("eidosSpeech API error (" + status + "): " + errorBody);
        }

        InputStream is = conn.getInputStream();
        if (is == null) {
            throw new IOException("No response stream, status: " + status);
        }

        // Read all audio bytes (complete MP3 file from eidosSpeech)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = is.read(buffer)) != -1) {
            baos.write(buffer, 0, n);
        }
        is.close();
        conn.disconnect();

        byte[] result = baos.toByteArray();
        baos.close();
        return result;
    }

    // no player needed
    private void playMp3Stream(byte[] mp3Bytes) throws Exception {
        // Decode and play MP3 directly in Java — no temp file, no external player
        ByteArrayInputStream bais = new ByteArrayInputStream(mp3Bytes);
        Player player = new Player(bais);
        player.play();  // Blocks until audio finishes
        player.close();
    }

    
     //Plays MP3 audio bytes through the default speakers using the system player.
     
    private void playMp3(byte[] mp3Bytes) throws Exception {
        File tempFile = File.createTempFile("tts_", ".mp3");
        tempFile.deleteOnExit();

        // Ensure file is fully written and flushed before playing
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(mp3Bytes);
            fos.flush();
        }

        // Verify file was written correctly
        if (tempFile.length() != mp3Bytes.length) {
            throw new IOException("File write incomplete: expected " + mp3Bytes.length +
                                  " bytes but wrote " + tempFile.length());
        }

        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;

        if (os.contains("linux")) {
            String[] players = {"ffplay", "mpv","cvlc", "mplayer"};
            String foundPlayer = null;
            for (String player : players) {
                if (isCommandAvailable(player)) {
                    foundPlayer = player;
                    break;
                }
            }
            if (foundPlayer == null) {
                throw new IOException("No audio player found. Install vlc, mpv, or ffplay.");
            }
            if (foundPlayer.equals("ffplay")) {
                pb = new ProcessBuilder(foundPlayer, "-nodisp", "-autoexit", tempFile.getAbsolutePath());
            } else if (foundPlayer.equals("cvlc")) {
                pb = new ProcessBuilder(foundPlayer, "--play-and-exit", "--quiet", tempFile.getAbsolutePath());
            } else {
                pb = new ProcessBuilder(foundPlayer, tempFile.getAbsolutePath());
            }
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder("afplay", tempFile.getAbsolutePath());
        } else if (os.contains("win")) {
            pb = new ProcessBuilder("cmd", "/c", "start", tempFile.getAbsolutePath());
        } else {
            throw new IOException("Unsupported OS: " + os);
        }

        pb.inheritIO();
        Process process = pb.start();
        process.waitFor();

        // Clean up temp file after playback
        tempFile.delete();
    }

    private boolean isCommandAvailable(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", cmd);
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}