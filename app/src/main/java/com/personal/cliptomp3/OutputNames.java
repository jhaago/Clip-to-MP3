package com.personal.cliptomp3;
final class OutputNames {
    static String suggestName(String input){
        String name=input==null?"":input.trim().replaceAll("[/\\\\\\p{Cntrl}]","_");
        int dot=name.lastIndexOf('.');if(dot>0) name=name.substring(0,dot);
        if(name.isEmpty()||name.equals("."))name="audio";
        if(name.length()>100)name=name.substring(0,100);
        return name+".mp3";
    }
}
