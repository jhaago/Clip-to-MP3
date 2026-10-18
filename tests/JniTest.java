package com.personal.cliptomp3;
import java.io.*;
public final class JniTest {
    public static void main(String[] args) throws Exception {
        try(Mp3Encoder encoder=new Mp3Encoder(44100,2);OutputStream out=new FileOutputStream(args[0])){
            short[] pcm=new short[44100*2];
            for(int i=0;i<44100;i++){short v=(short)(10000*Math.sin(2*Math.PI*440*i/44100));pcm[i*2]=v;pcm[i*2+1]=v;}
            out.write(encoder.encode(pcm));out.write(encoder.finish());
            try{encoder.encode(pcm);throw new AssertionError("Encoding after finish must fail");}catch(IOException expected){}
        }
        Mp3Encoder closed=new Mp3Encoder(48000,1);closed.close();closed.close();
        try{closed.finish();throw new AssertionError("Flushing after close must fail");}catch(IOException expected){}
        try{new Mp3Encoder(44100,3);throw new AssertionError("Invalid channel count must fail");}catch(IOException expected){}
        System.out.println("JNI encoding, finish/close and validation passed");
    }
}
