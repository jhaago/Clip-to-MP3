package com.personal.cliptomp3;
import android.app.Fragment;
import android.os.*;
import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConversionController extends Fragment {
    interface Observer {void changed();}
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private AtomicBoolean cancel=new AtomicBoolean();
    Observer observer;Uri input;String inputName="";File result;boolean running=false,exporting=false;
    int progress=-1;String status="Choose a video to get started.";
    @Override public void onCreate(Bundle state){super.onCreate(state);setRetainInstance(true);}
    void notifyChange(){if(observer!=null)observer.changed();}
    void choose(Uri uri,String name){input=uri;inputName=name;result=null;progress=-1;status="Ready to convert.";notifyChange();}
    void start(Context context){
        if(running||exporting||input==null)return;
        final Context app=context.getApplicationContext();final Uri source=input;
        cancel=new AtomicBoolean();final AtomicBoolean token=cancel;running=true;progress=-1;result=null;status="Preparing video…";notifyChange();
        worker.execute(()->{
            File dir=new File(app.getCacheDir(),"completed");dir.mkdirs();
            File part=new File(dir,UUID.randomUUID()+".part");
            try {
                AudioConverter.convert(app,source,part,p->{main.post(()->{if(running){progress=p;status=p<0?"Preparing audio…":"Converting… "+p+"%";notifyChange();}});},token);
                AudioConverter.check(token);
                File completed=new File(dir,part.getName().replace(".part",".mp3"));
                if(!part.renameTo(completed))throw new IOException("Couldn't finish saving the MP3. Check free storage.");
                main.post(()->{result=completed;running=false;progress=100;status="Your MP3 is ready.";notifyChange();});
            } catch(Exception e){part.delete();main.post(()->{running=false;progress=-1;status=token.get()?"Conversion cancelled.":message(e);notifyChange();});}
        });
    }
    void cancel(){AudioConverter.cancel(cancel);status="Cancelling…";notifyChange();}
    void save(Context context,Uri destination){
        if(result==null||exporting||running)return;final File source=result;final Context app=context.getApplicationContext();
        exporting=true;status="Saving MP3…";notifyChange();
        worker.execute(()->{
            String message="MP3 saved.";
            try{OutputFiles.copyTo(app,source,destination);}catch(Exception e){message="Couldn't save there. Your MP3 is still ready — try another location.";}
            final String finalMessage=message;main.post(()->{exporting=false;status=finalMessage;notifyChange();});
        });
    }
    private static String message(Exception e){
        if(e instanceof SecurityException)return "Couldn't access that video. Please select it again.";
        String m=e.getMessage();return m==null?"Conversion failed. Try another clip and check free storage.":m;
    }
    @Override public void onDestroy(){AudioConverter.cancel(cancel);observer=null;worker.shutdownNow();super.onDestroy();}
}
