package fr.kylian.photofilter.compressor;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;

import java.io.File;

public class MediaCompressor {

    public void compressVideo(File source, File target) throws EncoderException {
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("aac");
        audio.setBitRate(128000);

        VideoAttributes video = new VideoAttributes();
        video.setCodec("h264");
        video.setBitRate(1200000);
        video.setFrameRate(30);

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("mp4");
        attrs.setAudioAttributes(audio);
        attrs.setVideoAttributes(video);

        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(source), target, attrs);
    }

    public void compressImage(File source, File target) throws Exception {

    }

}
