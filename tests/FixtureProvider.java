package com.personal.cliptomp3;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Bundle;
import java.io.*;
public final class FixtureProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    public String getType(Uri uri){return "video/mp4";}
    public Bundle call(String method,String arg,Bundle extras){
        Bundle result=new Bundle();
        if("readAudio".equals(method))try(InputStream in=getContext().getContentResolver().openInputStream(Uri.parse(arg))){result.putBoolean("read",in!=null&&in.read()!=-1);}catch(Exception e){result.putString("error",e.toString());}
        return result;
    }
    public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException {
        String name=uri.getLastPathSegment();
        if(name==null||!name.matches("[a-zA-Z0-9]+\\.mp4"))throw new FileNotFoundException();
        try {
            ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createPipe();
            if(name.equals("stall.mp4")){
                new Thread(()->{try{Thread.sleep(5000);pipe[1].close();}catch(Exception ignored){}}).start();return pipe[0];
            }
            new Thread(()->{try(InputStream in=getContext().getAssets().open(name);OutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])){
                byte[] data=new byte[8192];int n;while((n=in.read(data))!=-1)out.write(data,0,n);
            }catch(IOException ignored){}}).start();return pipe[0];
        }catch(IOException e){throw new FileNotFoundException(e.toString());}
    }
    public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}
    public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
