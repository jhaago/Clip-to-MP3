package com.personal.cliptomp3;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;

public final class MainActivity extends Activity {
    private static final int PICK=10,SAVE=11;
    private final int ink=Color.rgb(20,35,55),blue=Color.rgb(39,95,218),muted=Color.rgb(93,108,128);
    private ConversionController controller;
    private TextView filename,status,detail;
    private Button choose,convert,cancel,save,share;
    private ProgressBar progress;
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        controller=(ConversionController)getFragmentManager().findFragmentByTag("conversion");
        if(controller==null){controller=new ConversionController();getFragmentManager().beginTransaction().add(controller,"conversion").commitNow();}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(246,248,252));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(24),dp(24),dp(24),dp(24));
        scroll.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
        scroll.addView(root);setContentView(scroll);
        TextView label=text("PERSONAL AUDIO TOOL",12,blue);label.setTypeface(null,Typeface.BOLD);root.addView(label);space(root,18);
        TextView title=text("Clip to MP3",34,ink);title.setTypeface(null,Typeface.BOLD);root.addView(title);space(root,8);
        root.addView(text("Keep the sound. Leave the video.",17,muted));space(root,26);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(20),dp(22),dp(20),dp(22));
        card.setBackground(shape(Color.WHITE,20));root.addView(card);
        TextView mark=text("♫",40,blue);card.addView(mark);space(card,14);
        TextView step=text("1   Choose your clip",19,ink);step.setTypeface(null,Typeface.BOLD);card.addView(step);space(card,8);
        filename=text("Select an MP4 video from your phone.",15,muted);filename.setMaxLines(3);card.addView(filename);space(card,18);
        choose=button("Choose video",blue,Color.WHITE);card.addView(choose);space(card,24);
        TextView two=text("2   Make it an MP3",19,ink);two.setTypeface(null,Typeface.BOLD);card.addView(two);space(card,8);
        detail=text("192 kbps audio · One clip at a time",14,muted);card.addView(detail);space(card,18);
        convert=button("Convert to MP3",ink,Color.WHITE);card.addView(convert);space(card,16);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgressTintList(ColorStateList.valueOf(blue));
        card.addView(progress,new LinearLayout.LayoutParams(-1,dp(8)));space(card,12);
        status=text("",15,muted);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);card.addView(status);space(card,12);
        cancel=button("Cancel",Color.rgb(234,239,248),ink);card.addView(cancel);
        LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.HORIZONTAL);card.addView(results);
        save=button("Save MP3",blue,Color.WHITE);share=button("Share",Color.rgb(234,239,248),ink);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1);left.setMarginEnd(dp(8));results.addView(save,left);results.addView(share,new LinearLayout.LayoutParams(0,-2,1));
        space(root,22);root.addView(text("ON YOUR PHONE, OFFLINE",12,blue));space(root,6);
        root.addView(text("Your videos stay on your device. Keep this app open while converting.",14,muted));space(root,12);
        TextView about=text("About & open-source notices",13,muted);about.setPadding(0,dp(12),0,dp(12));about.setOnClickListener(v->about());root.addView(about);
        choose.setOnClickListener(v->{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("video/mp4");startActivityForResult(intent,PICK);});
        convert.setOnClickListener(v->controller.start(this));cancel.setOnClickListener(v->controller.cancel());
        save.setOnClickListener(v->{Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/mpeg").putExtra(Intent.EXTRA_TITLE,OutputNames.suggestName(controller.inputName));startActivityForResult(intent,SAVE);});
        share.setOnClickListener(v->shareAudio());render();
    }
    @Override protected void onStart(){super.onStart();controller.observer=this::render;render();}
    @Override protected void onStop(){controller.observer=null;super.onStop();}
    private void render(){
        boolean busy=controller.running||controller.exporting,ready=controller.result!=null&&!busy;
        filename.setText(controller.input==null?"Select an MP4 video from your phone.":controller.inputName);
        choose.setEnabled(!busy);convert.setEnabled(controller.input!=null&&!busy);convert.setAlpha(convert.isEnabled()?1:0.4f);
        choose.setAlpha(choose.isEnabled()?1:0.5f);status.setText(controller.status);
        progress.setVisibility(controller.running?View.VISIBLE:View.GONE);progress.setIndeterminate(controller.progress<0);
        if(controller.progress>=0)progress.setProgress(controller.progress);
        cancel.setVisibility(controller.running?View.VISIBLE:View.GONE);cancel.setEnabled(!controller.status.equals("Cancelling…"));
        save.setVisibility(ready?View.VISIBLE:View.GONE);share.setVisibility(ready?View.VISIBLE:View.GONE);
        if(controller.running)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void shareAudio(){
        if(controller.result==null)return;
        Uri uri=new Uri.Builder().scheme("content").authority(getPackageName()+".audio").appendPath(controller.result.getName()).appendQueryParameter("name",controller.inputName).build();
        Intent intent=new Intent(Intent.ACTION_SEND).setType("audio/mpeg").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("MP3 audio",uri));
        try{startActivity(Intent.createChooser(intent,"Share MP3"));}catch(ActivityNotFoundException e){controller.status="No app is available to share audio. Use Save MP3 instead.";render();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(request==PICK){
            String name="video.mp4";
            try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))name=cursor.getString(0);
            }catch(Exception ignored){}
            try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}
            controller.choose(uri,name);
        }else if(request==SAVE)controller.save(this,uri);
    }
    private void about(){
        String notices="Clip to MP3 1.0\nPersonal, offline MP4 audio converter.\n\nMP3 encoder: LAME 3.100\nLicensed under GNU LGPL 2.0 or later.\nhttps://lame.sourceforge.io\n\nComplete license and corresponding source are included in the accompanying source archive.\n\n";
        try(java.io.InputStream in=getAssets().open("LAME-COPYING.txt");java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);
            notices+=new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
        }catch(Exception ignored){}
        TextView body=text(notices,14,ink);body.setPadding(dp(20),dp(16),dp(20),dp(16));ScrollView sc=new ScrollView(this);sc.addView(body);
        new AlertDialog.Builder(this).setTitle("About & licenses").setView(sc).setPositiveButton("Close",null).show();
    }
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);return t;}
    private Button button(String value,int background,int color){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(color);b.setTypeface(null,Typeface.BOLD);b.setBackground(shape(background,13));b.setMinHeight(dp(52));b.setPadding(dp(12),dp(10),dp(12),dp(10));return b;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private void space(LinearLayout layout,int height){View v=new View(this);layout.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
}
