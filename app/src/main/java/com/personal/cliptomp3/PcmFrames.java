package com.personal.cliptomp3;
import java.nio.*;
final class PcmFrames {
    private final int channels, encoding, frameBytes;
    private byte[] pending=new byte[0];
    PcmFrames(int channels,int encoding){
        if((channels!=1 && channels!=2)||(encoding!=2 && encoding!=4)) throw new IllegalArgumentException("Unsupported audio format.");
        this.channels=channels;this.encoding=encoding;frameBytes=channels*(encoding==4?4:2);
    }
    short[] accept(ByteBuffer input){
        if(input.remaining()>4*1024*1024) throw new IllegalArgumentException("Audio buffer is too large.");
        byte[] bytes=new byte[pending.length+input.remaining()];
        System.arraycopy(pending,0,bytes,0,pending.length);input.get(bytes,pending.length,input.remaining());
        int complete=bytes.length/frameBytes*frameBytes;
        short[] out=new short[complete/(encoding==4?4:2)];
        ByteBuffer b=ByteBuffer.wrap(bytes,0,complete).order(ByteOrder.LITTLE_ENDIAN);
        for(int i=0;i<out.length;i++){
            if(encoding==2) out[i]=b.getShort();
            else {float f=b.getFloat();if(Float.isNaN(f)) f=0;out[i]=(short)Math.max(-32768,Math.min(32767,Math.round(f*32768)));}
        }
        pending=java.util.Arrays.copyOfRange(bytes,complete,bytes.length);return out;
    }
    void finish(){if(pending.length!=0) throw new IllegalStateException("The audio ended with an incomplete frame.");}
}
