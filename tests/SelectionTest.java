package com.personal.cliptomp3;
import java.io.File;
public final class SelectionTest {
    public static void main(String[] args){
        ConversionController controller=new ConversionController();
        controller.result=new File("previous.mp3");
        controller.choose(null,"new.mp4");
        if(controller.result!=null)throw new AssertionError("New input exposes stale MP3 with the wrong name");
        if(!controller.inputName.equals("new.mp4"))throw new AssertionError("New filename wasn't retained");
        controller.onDestroy();System.out.println("Selection clears stale output: passed");
    }
}
