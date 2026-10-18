package com.personal.cliptomp3;
import android.content.Context;
import android.net.Uri;
import java.io.*;
final class OutputFiles {
    static void copyTo(Context context,File source,Uri destination) throws IOException {
        try(InputStream in=new FileInputStream(source);OutputStream out=context.getContentResolver().openOutputStream(destination,"wt")){
            if(out==null)throw new IOException("Destination couldn't be opened.");
            byte[] bytes=new byte[65536];int n;
            while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
            out.flush();
        }
    }
}
