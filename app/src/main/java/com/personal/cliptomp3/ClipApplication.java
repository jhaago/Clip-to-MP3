package com.personal.cliptomp3;
import android.app.Application;
import java.io.File;
public final class ClipApplication extends Application {
    @Override public void onCreate(){super.onCreate();clean(getCacheDir());clean(new File(getCacheDir(),"completed"));}
    private void clean(File dir){
        File[] files=dir.listFiles();if(files==null)return;
        for(File f:files)if(f.isFile() && (f.getName().startsWith("stage-")||f.getName().endsWith(".part")||System.currentTimeMillis()-f.lastModified()>86400000L))f.delete();
    }
}
