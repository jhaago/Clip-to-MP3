package com.personal.cliptomp3;
import java.io.IOException;
final class Mp3Encoder implements AutoCloseable {
    static {System.loadLibrary("clipmp3");}
    private long handle;
    private final int channels;
    private boolean finished;
    Mp3Encoder(int rate,int channels) throws IOException {
        if(channels!=1 && channels!=2) throw new IOException("Only mono and stereo audio are supported.");
        this.channels=channels;handle=create(rate,channels);
        if(handle==0) throw new IOException("Couldn't start the MP3 encoder for this audio format.");
    }
    byte[] encode(short[] pcm) throws IOException {
        if(pcm.length%channels!=0) throw new IOException("Incomplete audio frame.");
        return checked(pcm,false);
    }
    byte[] finish() throws IOException {byte[] out=checked(null,true);finished=true;return out;}
    private byte[] checked(short[] pcm,boolean flush) throws IOException {
        if(handle==0 || finished) throw new IOException("Encoder is already closed.");
        byte[] result=encodeNative(handle,pcm,flush);
        if(result==null) throw new IOException("MP3 encoding failed.");
        return result;
    }
    public void close(){if(handle!=0){destroy(handle);handle=0;}}
    private static native long create(int rate,int channels);
    private static native byte[] encodeNative(long handle,short[] pcm,boolean flush);
    private static native void destroy(long handle);
}
