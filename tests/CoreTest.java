package com.personal.cliptomp3;
import java.nio.*;
import java.util.*;
public class CoreTest {
    private static void check(boolean value) { if(!value) throw new AssertionError(); }
    public static void main(String[] args) {
        PcmFrames p = new PcmFrames(2,2);
        check(p.accept(ByteBuffer.wrap(new byte[]{1,0,2})).length==0);
        check(Arrays.equals(p.accept(ByteBuffer.wrap(new byte[]{0,3,0,4,0})),new short[]{1,2,3,4}));
        p.finish();
        PcmFrames f = new PcmFrames(1,4);
        ByteBuffer b=ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        b.putFloat(-2).putFloat(-0.5f).putFloat(0.5f).putFloat(2).flip();
        check(Arrays.equals(f.accept(b),new short[]{-32768,-16384,16384,32767}));
        try { new PcmFrames(3,2); throw new AssertionError(); } catch(IllegalArgumentException expected) {}
        PcmFrames partial=new PcmFrames(1,2); partial.accept(ByteBuffer.wrap(new byte[]{1}));
        try {partial.finish(); throw new AssertionError();} catch(IllegalStateException expected) {}
        check(OutputNames.suggestName("HOLIDAY.MP4").equals("HOLIDAY.mp3"));
        check(OutputNames.suggestName("voice").equals("voice.mp3"));
        check(OutputNames.suggestName(" ").equals("audio.mp3"));
        check(OutputNames.suggestName("日本語.mp4").equals("日本語.mp3"));
        System.out.println("PCM frame, float, validation and filename checks passed");
    }
}
