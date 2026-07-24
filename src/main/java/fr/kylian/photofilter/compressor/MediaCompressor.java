package fr.kylian.photofilter.compressor;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class MediaCompressor {

    /**
     * Compresses a video using H.264 / AAC codecs.
     */
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

    /**
     * Compresses an image file.
     * Uses Java ImageIO for standard formats (JPG, PNG),
     * and falls back to FFmpeg (via Jave2) for modern/HEIC/AVIF/WebP formats.
     * 
     * @param source Input image file
     * @param target Output image file
     * @param quality Quality factor between 0.0f and 1.0f (default: 0.75f = 75%)
     */
    public void compressImage(File source, File target, float quality) throws Exception {
        BufferedImage image = null;
        try {
            image = ImageIO.read(source);
        } catch (Exception ignored) {
            // ImageIO couldn't read the format directly (e.g. HEIC, AVIF)
        }

        if (image != null) {
            compressWithImageIO(image, target, quality);
        } else {
            // Fallback to FFmpeg for formats unsupported by native ImageIO
            compressWithFFmpeg(source, target, quality);
        }
    }

    public void compressImage(File source, File target) throws Exception {
        compressImage(source, target, 0.75f);
    }

    private void compressWithImageIO(BufferedImage image, File target, float quality) throws IOException {
        String targetName = target.getName().toLowerCase();

        if (targetName.endsWith(".png")) {
            // Lossless PNG compression
            ImageIO.write(image, "png", target);
        } else {
            // JPEG Compression (lossy with custom quality factor)
            BufferedImage rgbImage;
            if (image.getType() == BufferedImage.TYPE_INT_ARGB || 
                image.getType() == BufferedImage.TYPE_4BYTE_ABGR || 
                image.getColorModel().hasAlpha()) {
                // Convert transparent background to solid white for JPEG
                rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgbImage.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, image.getWidth(), image.getHeight());
                g.drawImage(image, 0, 0, null);
                g.dispose();
            } else {
                rgbImage = image;
            }

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                throw new IllegalStateException("Aucun ImageWriter JPG disponible dans le système");
            }

            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(new FileOutputStream(target))) {
                writer.setOutput(ios);

                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(quality);
                }

                writer.write(null, new IIOImage(rgbImage, null, null), param);
            } finally {
                writer.dispose();
            }
        }
    }

    private void compressWithFFmpeg(File source, File target, float quality) throws Exception {
        String ffmpegExecutable = new DefaultFFMPEGLocator().getExecutablePath();
        
        // Convert quality float (0.0 - 1.0) to FFmpeg qscale (1 - 31, where lower is better)
        int qscale = Math.max(1, Math.round((1.0f - quality) * 15 + 2));

        ProcessBuilder pb = new ProcessBuilder(
            ffmpegExecutable,
            "-y",                     // Overwrite output file if exists
            "-i", source.getAbsolutePath(),
            "-q:v", String.valueOf(qscale),
            target.getAbsolutePath()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Erreur lors de la compression FFmpeg de l'image (code " + exitCode + ")");
        }
    }
}
