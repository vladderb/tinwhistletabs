package fr.charleslabs.tinwhistletabs.music;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;

/**
 * Simple metronome that plays click sounds at specified tempo
 */
public class Metronome {
    private static final int SAMPLE_RATE = 22050;
    private static final int CLICK_DURATION_MS = 50;
    
    private AudioTrack audioTrack;
    private Handler handler;
    private Runnable clickRunnable;
    private boolean isPlaying = false;
    private int tempo = MusicSettings.DEFAULT_TEMPO;
    
    public Metronome() {
        handler = new Handler();
    }
    
    /**
     * Start metronome at specified tempo
     */
    public void start(int tempo) {
        if (isPlaying) {
            stop();
        }
        
        this.tempo = tempo;
        isPlaying = true;
        
        // Generate click sound
        final byte[] clickSound = generateClickSound();
        
        // Calculate interval between clicks in milliseconds
        // 60000 ms per minute / BPM = ms per beat
        final long intervalMs = 60000 / tempo;
        
        clickRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    playClick(clickSound);
                    handler.postDelayed(this, intervalMs);
                }
            }
        };
        
        // Start immediately
        handler.post(clickRunnable);
    }
    
    /**
     * Stop metronome
     */
    public void stop() {
        isPlaying = false;
        if (handler != null && clickRunnable != null) {
            handler.removeCallbacks(clickRunnable);
        }
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
    
    /**
     * Check if metronome is playing
     */
    public boolean isPlaying() {
        return isPlaying;
    }
    
    /**
     * Generate a short click sound
     */
    private byte[] generateClickSound() {
        int numSamples = (CLICK_DURATION_MS * SAMPLE_RATE) / 1000;
        byte[] sound = new byte[numSamples * 2]; // 16-bit = 2 bytes per sample
        
        // Generate a short beep at 1000 Hz
        double frequency = 1000.0;
        
        for (int i = 0; i < numSamples; i++) {
            // Generate sine wave with envelope (fade out)
            double envelope = 1.0 - ((double) i / numSamples);
            double sample = Math.sin(2.0 * Math.PI * i * frequency / SAMPLE_RATE) * envelope;
            
            // Convert to 16-bit PCM
            short val = (short) (sample * 32767 * 0.3); // 30% volume
            
            // Little endian
            sound[i * 2] = (byte) (val & 0xff);
            sound[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        
        return sound;
    }
    
    /**
     * Play a single click sound
     */
    private void playClick(byte[] clickSound) {
        try {
            if (audioTrack != null) {
                audioTrack.stop();
                audioTrack.release();
            }
            
            int bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(bufferSize, clickSound.length),
                    AudioTrack.MODE_STATIC);
            
            audioTrack.write(clickSound, 0, clickSound.length);
            audioTrack.play();
        } catch (Exception e) {
            android.util.Log.e("Metronome", "Error playing click: " + e.getMessage());
        }
    }
}
