package com.personal.cliptomp3;
import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class TestInstrumentation extends Instrumentation {
    private int passed=0;
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    private void report(String message){Bundle b=new Bundle();b.putString("stream",message+"\n");sendStatus(0,b);}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            Context app=getTargetContext();File dir=app.getExternalFilesDir(null);dir.mkdirs();
            for(String name:new String[]{"mono44100","stereo48000"}){
                File out=new File(dir,name+".mp3");
                AudioConverter.convert(app,Uri.parse("content://com.personal.cliptomp3.tests.fixtures/"+name+".mp4"),out,p->{},new AtomicBoolean());
                MediaExtractor e=new MediaExtractor();e.setDataSource(out.getAbsolutePath());
                MediaFormat f=e.getTrackFormat(0);
                check("audio/mpeg".equals(f.getString(MediaFormat.KEY_MIME)),"Output must be MP3");
                check(f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)==(name.startsWith("mono")?1:2),"Channels must be retained");
                long duration=f.getLong(MediaFormat.KEY_DURATION);check(duration>1900000 && duration<2400000,"Output duration mismatch: "+duration);e.release();
                passed++;report("PASS "+name+" conversion via nonseekable content URI");
            }
            for(String name:new String[]{"silent.mp4","damaged.mp4"}){
                File out=new File(dir,"failed.part");boolean failed=false;
                try{AudioConverter.convert(app,Uri.parse("content://com.personal.cliptomp3.tests.fixtures/"+name),out,p->{},new AtomicBoolean());}catch(IOException expected){failed=true;}
                check(failed,"Invalid/no-audio input must fail");check(!out.exists(),"Partial output must be removed");passed++;report("PASS "+name+" error and cleanup");
            }
            AtomicBoolean cancel=new AtomicBoolean(true);File out=new File(dir,"cancel.part");boolean cancelled=false;
            try{AudioConverter.convert(app,Uri.parse("content://com.personal.cliptomp3.tests.fixtures/mono44100.mp4"),out,p->{},cancel);}catch(InterruptedIOException expected){cancelled=true;}
            check(cancelled&&!out.exists(),"Cancellation must remove partial output");passed++;report("PASS cancellation cleanup");
            AtomicBoolean stalledCancel=new AtomicBoolean();
            new Thread(()->{try{Thread.sleep(300);}catch(InterruptedException ignored){}AudioConverter.cancel(stalledCancel);}).start();
            long started=System.currentTimeMillis();boolean stalledStopped=false;
            try{AudioConverter.convert(app,Uri.parse("content://com.personal.cliptomp3.tests.fixtures/stall.mp4"),out,p->{},stalledCancel);}catch(InterruptedIOException expected){stalledStopped=true;}
            check(stalledStopped&&System.currentTimeMillis()-started<2500,"Cancel must stop waiting for a stalled video provider");passed++;report("PASS stalled-provider cancellation");
            File source=new File(dir,"mono44100.mp3");boolean exportFailed=false;
            try{OutputFiles.copyTo(app,source,Uri.parse("content://com.personal.cliptomp3.audio/missing.mp3"));}catch(IOException expected){exportFailed=true;}
            check(exportFailed&&source.isFile(),"Failed export must retain output");passed++;report("PASS failed export retains MP3");
            File[] cache=app.getCacheDir().listFiles();for(File f:cache)check(!f.getName().startsWith("stage-"),"Staged input leaked");
            final ConversionController[] c=new ConversionController[1];
            runOnMainSync(()->{c[0]=new ConversionController();c[0].result=source;c[0].choose(Uri.parse("content://clip/new.mp4"),"new.mp4");});
            check(c[0].result==null,"Choosing a new video must hide the old result");passed++;report("PASS selecting a new video clears previous result");
            java.util.concurrent.atomic.AtomicReference<Activity> current=new AtomicReference<>();
            Application application=(Application)app.getApplicationContext();
            Application.ActivityLifecycleCallbacks callbacks=new Application.ActivityLifecycleCallbacks(){
                public void onActivityResumed(Activity a){current.set(a);}
                public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}
            };
            application.registerActivityLifecycleCallbacks(callbacks);
            Activity activity=startActivitySync(new Intent(app,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            final ConversionController[] retained=new ConversionController[1];
            runOnMainSync(()->{
                retained[0]=(ConversionController)activity.getFragmentManager().findFragmentByTag("conversion");
                retained[0].choose(Uri.parse("content://com.personal.cliptomp3.tests.fixtures/stereo48000.mp4"),"stereo.mp4");
                retained[0].start(app);retained[0].start(app);
                check(retained[0].running,"Conversion must start once");
                activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            });
            passed++;report("PASS duplicate Convert taps are ignored");
            long until=System.currentTimeMillis()+30000;
            while((current.get()==null||current.get()==activity)&&System.currentTimeMillis()<until)Thread.sleep(100);
            check(current.get()!=null&&current.get()!=activity,"Activity did not rotate");
            runOnMainSync(()->check(current.get().getFragmentManager().findFragmentByTag("conversion")==retained[0],"Rotation must retain conversion owner"));
            while(retained[0].running&&System.currentTimeMillis()<until)Thread.sleep(100);
            check(retained[0].result!=null,"Conversion must survive rotation");passed++;report("PASS rotation retains conversion and produces output");
            File kept=retained[0].result;
            runOnMainSync(()->((MainActivity)current.get()).onActivityResult(11,Activity.RESULT_CANCELED,null));
            check(retained[0].result==kept&&kept.isFile(),"Cancelled save picker must retain completed audio");passed++;report("PASS save-picker cancellation retains output");
            Uri shared=new Uri.Builder().scheme("content").authority("com.personal.cliptomp3.audio").appendPath(kept.getName()).appendQueryParameter("name","stereo.mp4").build();
            app.grantUriPermission("com.personal.cliptomp3.tests",shared,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Bundle read=app.getContentResolver().call(Uri.parse("content://com.personal.cliptomp3.tests.fixtures"),"readAudio",shared.toString(),null);
            check(read!=null&&read.getBoolean("read"),"Another app must read shared audio after URI grant: "+read);
            app.revokeUriPermission(shared,Intent.FLAG_GRANT_READ_URI_PERMISSION);passed++;report("PASS audio sharing across application UIDs");
            runOnMainSync(()->current.get().finish());application.unregisterActivityLifecycleCallbacks(callbacks);
            result.putString("stream","PASS "+passed+" Android conversion/export checks\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL after "+passed+" checks: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
    }
}
