package com.personal.cliptomp3;
import android.content.Context;
import android.net.Uri;
import android.media.*;
import android.content.res.AssetFileDescriptor;
import android.os.CancellationSignal;
import android.system.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

final class AudioConverter {
    private static final ConcurrentHashMap<AtomicBoolean,CancellationSignal> SIGNALS=new ConcurrentHashMap<>();
    interface Listener {void onProgress(int percent);}
    static void cancel(AtomicBoolean token){
        token.set(true);CancellationSignal signal=SIGNALS.get(token);
        if(signal!=null)new Thread(signal::cancel,"Cancel video provider").start();
    }
    static void check(AtomicBoolean cancelled) throws InterruptedIOException {
        if(cancelled.get()||Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Conversion cancelled.");
    }
    static void convert(Context context,Uri input,File output,Listener listener,AtomicBoolean cancelled) throws IOException {
        File staged=File.createTempFile("stage-",".mp4",context.getCacheDir());
        MediaExtractor extractor=new MediaExtractor();MediaCodec decoder=null;Mp3Encoder encoder=null;
        CancellationSignal signal=new CancellationSignal();SIGNALS.put(cancelled,signal);
        boolean success=false;
        try {
            listener.onProgress(-1);
            check(cancelled);
            try(AssetFileDescriptor asset=context.getContentResolver().openAssetFileDescriptor(input,"r",signal)){
                if(asset==null)throw new IOException("Couldn't open that video.");
                try(InputStream in=asset.createInputStream();OutputStream out=new FileOutputStream(staged)){
                byte[] copy=new byte[65536];int n;
                StructPollfd poll=new StructPollfd();poll.fd=asset.getParcelFileDescriptor().getFileDescriptor();poll.events=(short)(OsConstants.POLLIN|OsConstants.POLLHUP);
                while(true){
                    check(cancelled);
                    try{if(Os.poll(new StructPollfd[]{poll},200)==0)continue;}catch(ErrnoException e){throw new IOException("Couldn't read this video.",e);}
                    if((poll.revents&(OsConstants.POLLERR|OsConstants.POLLNVAL))!=0)throw new IOException("The video provider stopped responding.");
                    n=in.read(copy);if(n==-1)break;out.write(copy,0,n);
                }
                }
            }
            check(cancelled);extractor.setDataSource(staged.getAbsolutePath());
            MediaFormat selected=null;String decoderName=null;boolean audioFound=false;
            MediaCodecList codecs=new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            for(int i=0;i<extractor.getTrackCount();i++){
                MediaFormat f=extractor.getTrackFormat(i);String mime=f.getString(MediaFormat.KEY_MIME);
                if(mime!=null && mime.startsWith("audio/")){
                    audioFound=true;
                    if(f.containsKey(MediaFormat.KEY_CHANNEL_COUNT) && f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)>2)continue;
                    String candidate=codecs.findDecoderForFormat(f);
                    if(candidate!=null){selected=f;decoderName=candidate;extractor.selectTrack(i);break;}
                }
            }
            if(selected==null) throw new IOException(audioFound?"This video's audio format isn't supported. Try a mono or stereo clip.":"This video has no audio track.");
            long duration=selected.containsKey(MediaFormat.KEY_DURATION)?selected.getLong(MediaFormat.KEY_DURATION):0;
            decoder=MediaCodec.createByCodecName(decoderName);decoder.configure(selected,null,null,0);decoder.start();
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            boolean inputEnded=false,outputEnded=false;int rate=0,channels=0,encoding=0;long samples=0,lastOutput=System.currentTimeMillis();
            PcmFrames frames=null;listener.onProgress(duration>0?0:-1);
            try(OutputStream mp3=new BufferedOutputStream(new FileOutputStream(output))){
                while(!outputEnded){
                    check(cancelled);
                    if(System.currentTimeMillis()-lastOutput>30000)throw new IOException("The audio decoder stopped responding. Try another clip.");
                    if(!inputEnded){
                        int index=decoder.dequeueInputBuffer(10000);
                        if(index>=0){
                            ByteBuffer buffer=decoder.getInputBuffer(index);
                            if(buffer==null)throw new IOException("Couldn't read the audio decoder buffer.");
                            buffer.clear();int size=extractor.readSampleData(buffer,0);
                            if(size<0){decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnded=true;}
                            else {decoder.queueInputBuffer(index,0,size,Math.max(0,extractor.getSampleTime()),0);extractor.advance();}
                        }
                    }
                    int index=decoder.dequeueOutputBuffer(info,10000);
                    if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                        MediaFormat f=decoder.getOutputFormat();int nextRate=f.getInteger(MediaFormat.KEY_SAMPLE_RATE),nextChannels=f.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                        int nextEncoding=f.containsKey(MediaFormat.KEY_PCM_ENCODING)?f.getInteger(MediaFormat.KEY_PCM_ENCODING):2;
                        if(frames!=null && (rate!=nextRate||channels!=nextChannels||encoding!=nextEncoding))throw new IOException("The audio format changes during this clip, which isn't supported.");
                        if(frames==null){rate=nextRate;channels=nextChannels;encoding=nextEncoding;frames=new PcmFrames(channels,encoding);encoder=new Mp3Encoder(rate,channels);}
                        lastOutput=System.currentTimeMillis();
                    } else if(index>=0){
                        try {
                            if(info.size>0 && (info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){
                                if(frames==null)throw new IOException("The decoder didn't provide an audio format.");
                                ByteBuffer buffer=decoder.getOutputBuffer(index);
                                if(buffer==null)throw new IOException("Couldn't read decoded audio.");
                                buffer.position(info.offset);buffer.limit(info.offset+info.size);
                                short[] pcm=frames.accept(buffer);samples+=pcm.length;mp3.write(encoder.encode(pcm));
                                if(duration>0)listener.onProgress((int)Math.max(0,Math.min(99,info.presentationTimeUs*100/duration)));
                            }
                            outputEnded=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                            lastOutput=System.currentTimeMillis();
                        } finally {decoder.releaseOutputBuffer(index,false);}
                    }
                }
                if(encoder==null||samples==0)throw new IOException("No audio samples could be read from this video.");
                frames.finish();check(cancelled);mp3.write(encoder.finish());
            }
            check(cancelled);success=true;
        } catch(IllegalArgumentException|IllegalStateException e){throw new IOException("Couldn't decode this video's audio. It may be damaged or unsupported.",e);
        } finally {
            SIGNALS.remove(cancelled);
            if(encoder!=null)encoder.close();
            if(decoder!=null){try{decoder.stop();}catch(Exception ignored){}try{decoder.release();}catch(Exception ignored){}}
            extractor.release();staged.delete();if(!success)output.delete();
        }
    }
}
