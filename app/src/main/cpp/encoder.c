#include <stdlib.h>
#include <stdint.h>
#include "lame.h"

typedef struct { lame_t lame; int channels; } encoder;
encoder *enc_new(int rate, int channels) {
    if ((channels != 1 && channels != 2) || rate < 8000 || rate > 192000) return NULL;
    encoder *e = calloc(1, sizeof(*e));
    if (!e) return NULL;
    e->lame = lame_init(); e->channels = channels;
    if (!e->lame) {free(e); return NULL;}
    lame_set_in_samplerate(e->lame, rate);
    /* MPEG-2 sample rates cannot carry 192 kbps. LAME resamples these inputs. */
    if(rate<32000) lame_set_out_samplerate(e->lame,44100);
    lame_set_num_channels(e->lame, channels);
    lame_set_brate(e->lame, 192);
    lame_set_quality(e->lame, 2);
    lame_set_bWriteVbrTag(e->lame, 0);
    if (lame_init_params(e->lame) < 0) {lame_close(e->lame); free(e); return NULL;}
    return e;
}
int enc_write(encoder *e, const short *pcm, int count, unsigned char *out, int cap) {
    if (!e || count < 0 || count % e->channels) return -1;
    if (!count) return 0;
    return e->channels == 2
      ? lame_encode_buffer_interleaved(e->lame, (short *)pcm, count/2, out, cap)
      : lame_encode_buffer(e->lame, pcm, pcm, count, out, cap);
}
int enc_finish(encoder *e, unsigned char *out, int cap) {return e ? lame_encode_flush(e->lame, out, cap) : -1;}
void enc_close(encoder *e) {if(e) {lame_close(e->lame); free(e);}}

#ifdef __ANDROID__
#include <jni.h>
JNIEXPORT jlong JNICALL Java_com_personal_cliptomp3_Mp3Encoder_create(JNIEnv *env,jclass cls,jint rate,jint channels) {
    return (jlong)(intptr_t)enc_new(rate,channels);
}
JNIEXPORT jbyteArray JNICALL Java_com_personal_cliptomp3_Mp3Encoder_encodeNative(JNIEnv *env,jclass cls,jlong handle,jshortArray input,jboolean finish) {
    encoder *e=(encoder *)(intptr_t)handle;
    int count=input ? (*env)->GetArrayLength(env,input) : 0;
    if(count>1048576) return NULL;
    int cap=(int)(count*1.25)+7200;
    unsigned char *out=malloc(cap);
    if(!out) return NULL;
    jshort *pcm=input ? (*env)->GetShortArrayElements(env,input,NULL) : NULL;
    if(input && !pcm){free(out);return NULL;}
    int n=finish ? enc_finish(e,out,cap) : enc_write(e,pcm,count,out,cap);
    if(pcm) (*env)->ReleaseShortArrayElements(env,input,pcm,JNI_ABORT);
    jbyteArray result=NULL;
    if(n>=0){result=(*env)->NewByteArray(env,n); if(result) (*env)->SetByteArrayRegion(env,result,0,n,(jbyte *)out);}
    free(out);return result;
}
JNIEXPORT void JNICALL Java_com_personal_cliptomp3_Mp3Encoder_destroy(JNIEnv *env,jclass cls,jlong handle) {enc_close((encoder *)(intptr_t)handle);}
#endif
