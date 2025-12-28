package com.example.demo.client.util;

import javax.sound.sampled.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

/**
 * 🎤 VoiceRecorder - Utility class for recording voice messages
 * Uses javax.sound.sampled to capture audio from microphone
 */
@Slf4j
public class VoiceRecorder {

    // Audio format settings
    private static final float SAMPLE_RATE = 44100.0f;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1; // Mono
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    private static final int MAX_RECORDING_SECONDS = 60;
    private static final int MIN_RECORDING_MS = 1000; // Minimum 1 second

    private TargetDataLine targetDataLine;
    private ByteArrayOutputStream audioOutputStream;
    private Thread recordingThread;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private long recordingStartTime;

    // Callback for recording events
    private Runnable onRecordingStarted;
    private java.util.function.Consumer<Long> onRecordingProgress; // milliseconds elapsed
    private Runnable onRecordingStopped;

    public VoiceRecorder() {
    }

    /**
     * Set callback when recording starts
     */
    public void setOnRecordingStarted(Runnable callback) {
        this.onRecordingStarted = callback;
    }

    /**
     * Set callback for recording progress (called every 100ms with elapsed time)
     */
    public void setOnRecordingProgress(java.util.function.Consumer<Long> callback) {
        this.onRecordingProgress = callback;
    }

    /**
     * Set callback when recording stops
     */
    public void setOnRecordingStopped(Runnable callback) {
        this.onRecordingStopped = callback;
    }

    /**
     * Start recording audio from microphone
     * 
     * @return true if recording started successfully, false otherwise
     */
    public boolean startRecording() {
        if (isRecording.get()) {
            log.warn("🎤 Already recording!");
            return false;
        }

        try {
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            // Check if microphone is available
            if (!AudioSystem.isLineSupported(info)) {
                log.error("🎤 Microphone not supported on this system");
                return false;
            }

            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();

            audioOutputStream = new ByteArrayOutputStream();
            isRecording.set(true);
            recordingStartTime = System.currentTimeMillis();

            // Start recording in background thread
            recordingThread = new Thread(this::recordAudio, "VoiceRecorder-Thread");
            recordingThread.start();

            if (onRecordingStarted != null) {
                javafx.application.Platform.runLater(onRecordingStarted);
            }

            log.info("🎤 Started recording voice message");
            return true;

        } catch (LineUnavailableException e) {
            log.error("🎤 Failed to start recording: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Stop recording and return the audio file
     * 
     * @return File containing the recorded audio, or null if recording was too
     *         short or failed
     */
    public File stopRecording() {
        if (!isRecording.get()) {
            log.warn("🎤 Not currently recording!");
            return null;
        }

        long recordingDuration = System.currentTimeMillis() - recordingStartTime;
        isRecording.set(false);

        // Stop the data line
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }

        // Wait for recording thread to finish
        if (recordingThread != null) {
            try {
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (onRecordingStopped != null) {
            javafx.application.Platform.runLater(onRecordingStopped);
        }

        // Check minimum duration
        if (recordingDuration < MIN_RECORDING_MS) {
            log.info("🎤 Recording too short ({} ms), discarding", recordingDuration);
            return null;
        }

        // Save to WAV file
        try {
            File tempFile = File.createTempFile("voice_message_", ".wav");
            tempFile.deleteOnExit();

            byte[] audioData = audioOutputStream.toByteArray();
            AudioFormat format = getAudioFormat();

            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream ais = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());

            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempFile);

            log.info("🎤 Voice recording saved: {} ({} ms)", tempFile.getName(), recordingDuration);
            return tempFile;

        } catch (IOException e) {
            log.error("🎤 Failed to save recording: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Cancel recording without saving
     */
    public void cancelRecording() {
        if (!isRecording.get()) {
            return;
        }

        isRecording.set(false);

        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }

        if (onRecordingStopped != null) {
            javafx.application.Platform.runLater(onRecordingStopped);
        }

        log.info("🎤 Recording cancelled");
    }

    /**
     * Check if currently recording
     */
    public boolean isRecording() {
        return isRecording.get();
    }

    /**
     * Get elapsed recording time in milliseconds
     */
    public long getElapsedTime() {
        if (!isRecording.get()) {
            return 0;
        }
        return System.currentTimeMillis() - recordingStartTime;
    }

    /**
     * Background thread method to capture audio data
     */
    private void recordAudio() {
        byte[] buffer = new byte[4096];
        long lastProgressUpdate = 0;

        while (isRecording.get()) {
            int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                audioOutputStream.write(buffer, 0, bytesRead);
            }

            // Check max duration
            long elapsed = System.currentTimeMillis() - recordingStartTime;
            if (elapsed >= MAX_RECORDING_SECONDS * 1000L) {
                log.info("🎤 Maximum recording time reached ({} seconds)", MAX_RECORDING_SECONDS);
                isRecording.set(false);
                break;
            }

            // Progress callback every 100ms
            if (onRecordingProgress != null && elapsed - lastProgressUpdate >= 100) {
                lastProgressUpdate = elapsed;
                final long elapsedFinal = elapsed;
                javafx.application.Platform.runLater(() -> onRecordingProgress.accept(elapsedFinal));
            }
        }
    }

    /**
     * Get the audio format for recording
     */
    private AudioFormat getAudioFormat() {
        return new AudioFormat(
                SAMPLE_RATE,
                SAMPLE_SIZE_BITS,
                CHANNELS,
                SIGNED,
                BIG_ENDIAN);
    }

    /**
     * Format milliseconds to mm:ss string
     */
    public static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Check if microphone is available on this system
     */
    public static boolean isMicrophoneAvailable() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            return AudioSystem.isLineSupported(info);
        } catch (Exception e) {
            return false;
        }
    }
}
