#include <stdio.h>
#include <stdlib.h>
#include "../app/src/main/cpp/encoder.c"
int main(int argc,char **argv) {
    if(argc!=5) return 2;
    encoder *e=enc_new(atoi(argv[1]),atoi(argv[2]));
    if(!e) return 3;
    unsigned char out[20000];short pcm[8192];
    if(enc_write(e,pcm,0,out,sizeof(out))!=0) return 4;
    FILE *in=fopen(argv[3],"rb"), *dest=fopen(argv[4],"wb");
    if(!in || !dest) return 5;
    int n;
    while((n=fread(pcm,sizeof(short),8192,in))>0){
        int bytes=enc_write(e,pcm,n,out,sizeof(out));
        if(bytes<0 || fwrite(out,1,bytes,dest)!=(size_t)bytes) return 6;
    }
    n=enc_finish(e,out,sizeof(out));
    if(n<0) return 7;
    fwrite(out,1,n,dest); fclose(in);fclose(dest);enc_close(e);
    return 0;
}
