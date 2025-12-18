package com.example.demo.util;

import javax.sound.sampled.*;
import java.io.*;

/**
 * Utility to generate simple notification sounds
 * Run this once to create sound files
 */
public class SoundGenerator {

    public static void main(String[] args) throws Exception {
        String basePath = "src/main/resources/sounds/";

        // Create directory if not exists
        new File(basePath).mkdirs();

        // Generate message notification (soft ding - 440Hz)
        generateTone(basePath + "message.wav", 440, 0.3, 0.8);
        System.out.println("Created message.wav");

        // Generate friend request (friendly chime - rising 330Hz to 550Hz)
        generateChime(basePath + "friend_request.wav", new int[] { 330, 440, 550 }, 0.15, 0.7);
        System.out.println("Created friend_request.wav");

        // Generate room invite (bell - 520Hz)
        generateTone(basePath + "room_invite.wav", 520, 0.4, 0.7);
        System.out.println("Created room_invite.wav");

        System.out.println("All notification sounds generated!");
    }

    /**
     * Generate a simple sine wave tone
     */
    private static void generateTone(String filename, int frequency, double durationSec, double volume)
            throws Exception {
        float sampleRate = 44100;
        int numSamples = (int) (durationSec * sampleRate);
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * i * frequency / sampleRate;
            double envelope = Math.min(1.0, Math.min(i / (sampleRate * 0.02), (numSamples - i) / (sampleRate * 0.1)));
            short value = (short) (Math.sin(angle) * 32767 * volume * envelope);
            buffer[i * 2] = (byte) (value & 0xFF);
            buffer[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
        }

        saveWav(filename, buffer, sampleRate);
    }

    /**
     * Generate a chime with multiple frequencies
     */
    private static void generateChime(String filename, int[] frequencies, double noteLength, double volume)
            throws Exception {
        float sampleRate = 44100;
        int samplesPerNote = (int) (noteLength * sampleRate);
        int totalSamples = samplesPerNote * frequencies.length;
        byte[] buffer = new byte[totalSamples * 2];

        for (int note = 0; note < frequencies.length; note++) {
            int frequency = frequencies[note];
            int startSample = note * samplesPerNote;

            for (int i = 0; i < samplesPerNote; i++) {
                double angle = 2.0 * Math.PI * i * frequency / sampleRate;
                double envelope = Math.min(1.0,
                        Math.min(i / (sampleRate * 0.01), (samplesPerNote - i) / (sampleRate * 0.05)));
                short value = (short) (Math.sin(angle) * 32767 * volume * envelope);
                int index = (startSample + i) * 2;
                buffer[index] = (byte) (value & 0xFF);
                buffer[index + 1] = (byte) ((value >> 8) & 0xFF);
            }
        }

        saveWav(filename, buffer, sampleRate);
    }

    /**
     * Save raw PCM data as WAV file
     */
    private static void saveWav(String filename, byte[] data, float sampleRate) throws Exception {
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        AudioInputStream ais = new AudioInputStream(bais, format, data.length / 2);

        File file = new File(filename);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
        ais.close();
    }
}
