#!/usr/bin/env python3
import pathlib,subprocess
out=pathlib.Path(__file__).resolve().parents[1]/'build/test-assets';out.mkdir(parents=True,exist_ok=True)
for name,rate,channels in [('mono44100',44100,1),('stereo48000',48000,2)]:
 subprocess.run(['ffmpeg','-v','error','-f','lavfi','-i','color=c=blue:s=160x120:d=2','-f','lavfi','-i',f'sine=frequency=440:sample_rate={rate}:duration=2','-c:v','mpeg4','-c:a','aac','-ac',str(channels),'-shortest','-y',str(out/(name+'.mp4'))],check=True)
subprocess.run(['ffmpeg','-v','error','-f','lavfi','-i','color=c=blue:s=160x120:d=2','-c:v','mpeg4','-y',str(out/'silent.mp4')],check=True)
(out/'damaged.mp4').write_bytes(b'not a video')
