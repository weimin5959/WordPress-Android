package org.wordpress.android.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class VideoUtils {

    // where to put the output file (note: /sdcard requires WRITE_EXTERNAL_STORAGE permission)
    private static final File OUTPUT_DIR = Environment.getExternalStorageDirectory();

    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 5;           // 5 seconds between I-frames
    private static final long DURATION_SEC = 8;             // 8 seconds of video

    private static final int OUTPUT_WIDTH = 1024;
    private static final int OUTPUT_HEIGHT = 768;
    private static final int OUTPUT_FRAMERATE_BITS_PER_SEC = 10;

    // movie length, in frames
    private static final int NUM_FRAMES = 30;               // two seconds of video
    private static final int TEST_Y = 120;                  // YUV values for colored rect
    private static final int TEST_U = 160;
    private static final int TEST_V = 200;


    private static final int MAX_SAMPLE_SIZE = 256 * 1024;
    private static final float LATITUDE = 0.0000f;
    private static final float LONGITUDE  = -180.0f;
    private static final float BAD_LATITUDE = 91.0f;
    private static final float BAD_LONGITUDE = -181.0f;
    private static final float TOLERANCE = 0.0002f;


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static boolean copyVideo(Context context, Uri videoUri) {
        if(context == null || videoUri == null) {
            AppLog.e(AppLog.T.MEDIA, "context and videoUri can't be null.");
            return false;
        }

        String outputFile = "/sdcard/videoAudio.mp4";

        /*File file = new File(outputFile);*/

        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(context, videoUri, null);
        } catch (IOException e) {
            return false;
        }

        MediaMuxer muxer = null;
        try {
            // Set up MediaMuxer for the destination.
            muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            return false;
        }


        int trackCount = extractor.getTrackCount();
        // Set up the tracks.
        HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(trackCount);
        for (int i = 0; i < trackCount; i++) {
            extractor.selectTrack(i);
            MediaFormat format = extractor.getTrackFormat(i);
            int dstIndex = muxer.addTrack(format);
            indexMap.put(i, dstIndex);
        }

        // Copy the samples from MediaExtractor to MediaMuxer.
        boolean sawEOS = false;
        int bufferSize = MAX_SAMPLE_SIZE;
        int frameCount = 0;
        int offset = 100;
        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        int degrees = getVideoRotationDegrees(context, videoUri);
        if (degrees >= 0) {
            muxer.setOrientationHint(degrees);
        }

        muxer.start();
        while (!sawEOS) {
            bufferInfo.offset = offset;
            bufferInfo.size = extractor.readSampleData(dstBuf, offset);
            if (bufferInfo.size < 0) {
                AppLog.d(AppLog.T.MEDIA, "saw input EOS.");
                sawEOS = true;
                bufferInfo.size = 0;
            } else {
                bufferInfo.presentationTimeUs = extractor.getSampleTime();
                bufferInfo.flags = extractor.getSampleFlags();
                int trackIndex = extractor.getSampleTrackIndex();
                muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
                extractor.advance();
                frameCount++;
                AppLog.v(AppLog.T.MEDIA, "Frame (" + frameCount + ") " +
                        "PresentationTimeUs:" + bufferInfo.presentationTimeUs +
                        " Flags:" + bufferInfo.flags +
                        " TrackIndex:" + trackIndex +
                        " Size(KB) " + bufferInfo.size / 1024);
            }
        }
        muxer.stop();
        muxer.release();

        return true;
    }


    public static long getVideoDurationMS(Context context, File file) {
        if(context == null || file == null) {
            AppLog.e(AppLog.T.MEDIA, "context and file can't be null.");
            return 0L;
        }
        return getVideoDurationMS(context, Uri.fromFile(file));
    }

    public static long getVideoDurationMS(Context context, Uri videoUri) {
        if(context == null || videoUri == null) {
            AppLog.e(AppLog.T.MEDIA, "context and videoUri can't be null.");
            return 0L;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, videoUri);
        } catch (IllegalArgumentException | SecurityException e) {
            AppLog.e(AppLog.T.MEDIA, "Can't read duration of the video.", e);
            return 0L;
        } catch (RuntimeException e) {
            // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5431
            AppLog.e(AppLog.T.MEDIA, "Can't read duration of the video due to a Runtime Exception happened setting the datasource", e);
            return 0L;
        }

        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (time == null) {
            return 0L;
        }
        return Long.parseLong(time);
    }


    public static int getVideoRotationDegrees(Context context, Uri videoUri) {
        if(context == null || videoUri == null) {
            AppLog.e(AppLog.T.MEDIA, "context and videoUri can't be null.");
            return -1;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, videoUri);
        } catch (IllegalArgumentException | SecurityException e) {
            AppLog.e(AppLog.T.MEDIA, "Can't read rotation of the video.", e);
            return -1;
        } catch (RuntimeException e) {
            // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5431
            AppLog.e(AppLog.T.MEDIA, "Can't read rotation of the video due to a Runtime Exception happened setting the datasource", e);
            return -1;
        }

        String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (rotation == null) {
            return -1;
        }
        return Integer.parseInt(rotation);
    }
}
