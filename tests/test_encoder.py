import json, math, pathlib, struct, subprocess, tempfile, unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]

class EncoderTests(unittest.TestCase):
    def test_real_mp3_mono_stereo(self):
        harness = ROOT / 'tmp/encoder-harness'
        self.assertTrue(harness.exists(), 'MP3 encoder harness has not been implemented')
        with tempfile.TemporaryDirectory() as folder:
            for channels in (1, 2):
                for rate in (16000, 44100, 48000):
                    pcm = pathlib.Path(folder) / 'tone.pcm'
                    mp3 = pathlib.Path(folder) / 'tone.mp3'
                    pcm.write_bytes(b''.join(struct.pack('<h', int(10000*math.sin(2*math.pi*440*i/rate))) * channels for i in range(rate)))
                    subprocess.run([str(harness),str(rate),str(channels),str(pcm),str(mp3)],check=True)
                    data=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-show_format','-of','json',str(mp3)]))
                    self.assertEqual(data['streams'][0]['codec_name'],'mp3')
                    self.assertEqual(data['streams'][0]['channels'],channels)
                    self.assertEqual(int(data['streams'][0]['bit_rate']),192000)
                    self.assertLess(abs(float(data['format']['duration'])-1),0.15)
                    decoded=subprocess.check_output(['ffmpeg','-v','error','-i',str(mp3),'-f','s16le','-'])
                    samples=struct.unpack('<'+'h'*(len(decoded)//2),decoded)
                    self.assertGreater(sum(s*s for s in samples)/len(samples),1000000)
            for channels in (0,3):
                result=subprocess.run([str(harness),'44100',str(channels),str(pcm),str(mp3)],capture_output=True)
                self.assertNotEqual(result.returncode,0)

if __name__=='__main__': unittest.main()
