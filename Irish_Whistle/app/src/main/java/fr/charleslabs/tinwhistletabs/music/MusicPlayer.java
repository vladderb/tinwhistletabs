package fr.charleslabs.tinwhistletabs.music;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.PresetReverb;
import android.os.Handler;

import java.util.List;

import fr.charleslabs.tinwhistletabs.music.synth.TinWhistleSynth;

public class MusicPlayer {
    private static final int SAMPLE_RATE = 22050;

    // Internal states
    private AudioTrack audioTrack = null;
    private final Handler handler = new Handler();

    //Singleton
    private static MusicPlayer instance;
    public static MusicPlayer getInstance(){
        if(instance == null){
            instance = new MusicPlayer();
        }
        return instance;
    }
    private MusicPlayer(){}

    public static byte[] genMusic(List<MusicNote> notes, float tempoModifier){
        // Compute length of music
        float lengthInS = 0;
        for(MusicNote note : notes) {
            lengthInS += note.getLengthInS(tempoModifier);
        }

        final float[] music = new float[(int)(lengthInS*SAMPLE_RATE)];
        int index = 0;
        for(MusicNote note : notes) {
            index += genNote(note,tempoModifier,music,index);
        }

        TinWhistleSynth.reverb(music, (int)(SAMPLE_RATE*0.1f), 0.2f);
        TinWhistleSynth.reverb(music, (int)(SAMPLE_RATE*0.2f), 0.1f);
        TinWhistleSynth.reverb(music, (int)(SAMPLE_RATE*0.3f), 0.05f);
        TinWhistleSynth.reverb(music, (int)(SAMPLE_RATE*0.4f), 0.05f);

        return toneToBytePCM(music);
    }

    private static int genNote(MusicNote note, float tempoModifier, float[] music, int offset){
        int numSamples = (int)(note.getLengthInS(tempoModifier)*SAMPLE_RATE);

        if (numSamples+offset >= music.length -1)
            numSamples = music.length - offset -1;

        if (note.isRest())
            for (int i = 0; i < numSamples; ++i)
                music[i+offset] = 0;
        else
            TinWhistleSynth.genNote(note.getFrequency(),numSamples,music,offset,SAMPLE_RATE);

        return numSamples;
    }

    // Media controls: set, play, pause, stop, clear
    public void setAudioTrack(byte[] generatedSnd){
        if(audioTrack != null){
            audioTrack.stop();
            audioTrack.flush();
            audioTrack.release();
        }
        
        // Получаем минимальный размер буфера
        int minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        
        // Используем максимум из минимального размера и размера данных
        int bufferSize = Math.max(minBufferSize, generatedSnd.length);
        
        android.util.Log.d("MusicPlayer", "Creating AudioTrack: dataSize=" + generatedSnd.length + 
                ", minBufferSize=" + minBufferSize + ", bufferSize=" + bufferSize);
        
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                AudioTrack.MODE_STATIC);

        // PRESET
        try {
            final PresetReverb reverb = new PresetReverb(0, audioTrack.getAudioSessionId());
            reverb.setPreset(PresetReverb.PRESET_SMALLROOM);
            reverb.setEnabled(true);
            audioTrack.setAuxEffectSendLevel(1.0f);
        }catch (Exception ignored){}

        int written = audioTrack.write(generatedSnd, 0, generatedSnd.length);
        android.util.Log.d("MusicPlayer", "Written " + written + " bytes to AudioTrack");
        
        if (written != generatedSnd.length) {
            android.util.Log.w("MusicPlayer", "Warning: not all data written to AudioTrack");
        }
    }

    public void play() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if (audioTrack != null) {
                            try {
                                int state = audioTrack.getState();
                                if (state == AudioTrack.STATE_INITIALIZED) {
                                    audioTrack.play();
                                } else {
                                    android.util.Log.e("MusicPlayer", "AudioTrack not initialized, state: " + state);
                                }
                            } catch (Exception e) {
                                android.util.Log.e("MusicPlayer", "Error playing audio", e);
                            }
                        }
                    }
                });
            }
        });
        thread.start();
    }

    public void pause() {
        if (audioTrack != null){
            audioTrack.pause();
        }
    }

    public void stop() {
        if (audioTrack != null)
            audioTrack.stop();
    }

    public void move(float time) {
        if (audioTrack != null)
            audioTrack.setPlaybackHeadPosition((int)(time*SAMPLE_RATE));
    }

    private static byte[] toneToBytePCM(double[] tone){
        final byte[] generatedSnd = new byte[tone.length * 2];
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (double dVal : tone) {
            short val = (short) (dVal * 32767);
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        return generatedSnd;
    }

    private static byte[] toneToBytePCM(float[] tone){
        final byte[] generatedSnd = new byte[tone.length * 2];
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (float dVal : tone) {
            short val = (short) (dVal * 32767);
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        return generatedSnd;
    }
}
