package com.personal.cliptomp3;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
public final class ShareProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    private File resolve(Uri uri) throws FileNotFoundException {
        String name=uri.getLastPathSegment();
        if(name==null||!name.matches("[0-9a-fA-F-]+\\.mp3"))throw new FileNotFoundException();
        File result=new File(new File(getContext().getCacheDir(),"completed"),name);
        if(!result.isFile())throw new FileNotFoundException();return result;
    }
    public String getType(Uri uri){return "audio/mpeg";}
    public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!"r".equals(mode))throw new FileNotFoundException("Read-only audio.");
        return ParcelFileDescriptor.open(resolve(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){
        try {
            File file=resolve(uri);
            String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
            MatrixCursor cursor=new MatrixCursor(cols);Object[] row=new Object[cols.length];
            for(int i=0;i<cols.length;i++){
                if(OpenableColumns.DISPLAY_NAME.equals(cols[i]))row[i]=OutputNames.suggestName(uri.getQueryParameter("name"));
                if(OpenableColumns.SIZE.equals(cols[i]))row[i]=file.length();
            }
            cursor.addRow(row);return cursor;
        }catch(FileNotFoundException e){return null;}
    }
    public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
