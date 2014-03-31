package com.example.flower.gles;

/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

//20131122: minor tweaks to saveFrame() I/O
//20131205: add alpha to EGLConfig (huge glReadPixels speedup); pre-allocate pixel buffers;
//          log time to run saveFrame()
//20140123: correct error checks on glGet*Location() and program creation (they don't set error)
//20140212: eliminate byte swap

/**
 * Extract frames from an MP4 using MediaExtractor, MediaCodec, and GLES. Put a
 * .mp4 file in "/sdcard/source.mp4" and look for output files named
 * "/sdcard/frame-XX.png".
 * <p>
 * This uses various features first available in Android "Jellybean" 4.1 (API
 * 16).
 * <p>
 * (This was derived from bits and pieces of CTS tests, and is packaged as such,
 * but is not currently part of CTS.)
 */

public class ExtractMpegFramesTest {
    private static final String TAG = ExtractMpegFramesTest.class.getSimpleName();
    private static final boolean VERBOSE = false; // lots of logging

    // where to find files (note: requires WRITE_EXTERNAL_STORAGE permission)
    private static final File FILES_DIR = Environment
            .getExternalStorageDirectory();
    private static final String INPUT_FILE = "source.mp4";
    private static final int MAX_FRAMES = 10; // stop extracting after this many

    /** test entry point */
    public ExtractMpegFramesTest() throws Throwable {

    }

    public void start() throws Throwable {
        ExtractMpegFramesWrapper.runTest(this);
    };

    /**
     * Wraps extractMpegFrames(). This is necessary because SurfaceTexture will
     * try to use the looper in the current thread if one exists, and the CTS
     * tests create one on the test thread.
     * 
     * The wrapper propagates exceptions thrown by the worker thread back to the
     * caller.
     */
    private static class ExtractMpegFramesWrapper implements Runnable {
        private ExtractMpegFramesTest mTest;

        private ExtractMpegFramesWrapper(ExtractMpegFramesTest test) {
            mTest = test;
        }

        @Override
        public void run() {
            try {
                mTest.extractMpegFrames();
            } catch (Throwable th) {
                Log.e(TAG, "Throwable: " + th.getMessage());
            }
        }

        /** Entry point. */
        public static void runTest(ExtractMpegFramesTest obj) throws Throwable {
            ExtractMpegFramesWrapper wrapper = new ExtractMpegFramesWrapper(obj);
            /*
             * Handler mHandler = new Handler(); mHandler.post(wrapper);
             */
            new Thread(wrapper).start();

        }
    }

    /**
     * Tests extraction from an MP4 to a series of PNG files.
     * <p>
     * We scale the video to 640x480 for the PNG just to demonstrate that we can
     * scale the video with the GPU. If the input video has a different aspect
     * ratio, we could preserve it by adjusting the GL viewport to get
     * letterboxing or pillarboxing, but generally if you're extracting frames
     * you don't want black bars.
     */
    private void extractMpegFrames() throws IOException {
        MediaCodec decoder = null;
        CodecOutputSurface outputSurface = null;
        MediaExtractor extractor = null;
        int saveWidth = 640;
        int saveHeight = 480;

        try {
            File inputFile = new File(FILES_DIR, INPUT_FILE); // must be an
                                                              // absolute path
            // The MediaExtractor error messages aren't very useful. Check to
            // see if the input
            // file exists so we can throw a better one if it's not there.
            if (!inputFile.exists()) {
                Log.e(TAG, " sorry, input file not exist !");
                return;
            }
            if (!inputFile.canRead()) {
                throw new FileNotFoundException("Unable to read " + inputFile);
            }

            extractor = new MediaExtractor();
            extractor.setDataSource(inputFile.toString());
            int trackIndex = selectTrack(extractor);
            if (trackIndex < 0) {
                throw new RuntimeException("No video track found in "
                        + inputFile);
            }
            extractor.selectTrack(trackIndex);

            MediaFormat format = extractor.getTrackFormat(trackIndex);
            if (VERBOSE) {
                Log.d(TAG,
                        "Video size is "
                                + format.getInteger(MediaFormat.KEY_WIDTH)
                                + "x"
                                + format.getInteger(MediaFormat.KEY_HEIGHT));
            }

            // Could use width/height from the MediaFormat to get full-size
            // frames.
            outputSurface = new CodecOutputSurface(saveWidth, saveHeight);

            // Create a MediaCodec decoder, and configure it with the
            // MediaFormat from the
            // extractor. It's very important to use the format from the
            // extractor because
            // it contains a copy of the CSD-0/CSD-1 codec-specific data chunks.
            String mime = format.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, outputSurface.getSurface(), null, 0);
            decoder.start();

            doExtract(extractor, trackIndex, decoder, outputSurface);
        } finally {
            // release everything we grabbed
            if (outputSurface != null) {
                outputSurface.release();
                outputSurface = null;
            }
            if (decoder != null) {
                decoder.stop();
                decoder.release();
                decoder = null;
            }
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
        }
    }

    /**
     * Selects the video track, if any.
     * 
     * @return the track index, or -1 if no video track is found.
     */
    private int selectTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                Log.d(TAG, " Extractor selected track " + i + " ("
                        + mime + "): " + format);
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime
                            + "): " + format);
                }
                return i;
            }
        }

        return -1;
    }

    void doExtract(MediaExtractor extractor, int trackIndex,
            MediaCodec decoder, CodecOutputSurface outputSurface)
            throws IOException {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        // MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inputChunk = 0;
        int decodeCount = 0;
        long frameSaveTime = 0;

        boolean sawInputEOS = false;
        long inputEOSPTS = -1;
        long lastPTS = -1;
        for (;;) {
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outputBufferIndex = -1;

            // Queue Input buffer
            for (; !sawInputEOS;) {
                long presentationTimeUs = 0;
                int inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufferIndex >= 0) {
                    ByteBuffer dstBuf = decoderInputBuffers[inputBufferIndex];

                    int sampleSize = extractor.readSampleData(dstBuf, 0);
                    presentationTimeUs = extractor.getSampleTime();
                    if (presentationTimeUs > 0)
                        lastPTS = presentationTimeUs;

                    Log.d(TAG, "Input Buffer");
                    Log.d(TAG, "InputBufIndex:" + String.valueOf(inputBufferIndex));
                    Log.d(TAG, "PresentationTimeUS"+
                            String.valueOf(presentationTimeUs));
                    lastPTS = presentationTimeUs;

                    if (!sawInputEOS) {
                        Log.d(TAG, "Extractor Advancing");
                        if (!extractor.advance()) {
                            Log.i(TAG, "Input EOS");
                            sawInputEOS = true;
                            sampleSize = 0;
                            inputEOSPTS = lastPTS;
                        }
                    }

                    if (sampleSize > 0 || sawInputEOS)
                        decoder.queueInputBuffer(
                                inputBufferIndex,
                                0, // offset
                                sampleSize,
                                presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                        : 0);
                    else {
                        break;
                    }

                    outputBufferIndex = decoder
                            .dequeueOutputBuffer(info, TIMEOUT_USEC);
                    if (outputBufferIndex >= 0) {
                        break;
                    }
                }
            }

            //
            // handle output buffer
            //
            if (outputBufferIndex < 0) {
                outputBufferIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                Log.i(TAG, "outputBufferIndex = " + outputBufferIndex);
            }

            boolean isOutputEOS = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0;
            Log.i(TAG, "info.presentationTimeUs = " + info.presentationTimeUs
                    + " inputEOSPTS = " + inputEOSPTS);
            isOutputEOS |= (inputEOSPTS == info.presentationTimeUs);

            if (outputBufferIndex >= 0) {
                /*
                 * if (mFrameListener != null) {
                 * mFrameListener.onFrameAvailable(info.presentationTimeUs,
                 * outputBufferIndex, isOutputEOS); }
                 */
                decoder.releaseOutputBuffer(outputBufferIndex, true);
                Log.v(TAG, " outputBufferIndex: " + outputBufferIndex);
                outputSurface.awaitNewImage();
                outputSurface.drawImage(true);
                Log.v(TAG, " decodeCount: " + decodeCount);
                if (decodeCount < MAX_FRAMES) {
                    File outputFile = new File(
                            FILES_DIR,
                            String.format("frame-%02d.png", decodeCount));
                    long startWhen = System.nanoTime();
                    outputSurface.saveFrame(outputFile.toString());
                    frameSaveTime += System.nanoTime() - startWhen;
                    decodeCount++;
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // No need to update output buffer, since we don't touch it
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                final MediaFormat oformat = decoder.getOutputFormat();
                getColorFormat(oformat);
            }

            if (isOutputEOS) {
                Log.i(TAG, "out EOS ");
                break;
            }
        }
    }

    private void getColorFormat(MediaFormat format) {
        int colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);

        int QOMX_COLOR_FormatYUV420PackedSemiPlanar64x32Tile2m8ka = 0x7FA30C03;

        String formatString = "";
        if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format12bitRGB444) {
            formatString = "COLOR_Format12bitRGB444";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB1555) {
            formatString = "COLOR_Format16bitARGB1555";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB4444) {
            formatString = "COLOR_Format16bitARGB4444";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format16bitBGR565) {
            formatString = "COLOR_Format16bitBGR565";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format16bitRGB565) {
            formatString = "COLOR_Format16bitRGB565";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format18bitARGB1665) {
            formatString = "COLOR_Format18bitARGB1665";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format18BitBGR666) {
            formatString = "COLOR_Format18BitBGR666";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format18bitRGB666) {
            formatString = "COLOR_Format18bitRGB666";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format19bitARGB1666) {
            formatString = "COLOR_Format19bitARGB1666";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format24BitABGR6666) {
            formatString = "COLOR_Format24BitABGR6666";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format24bitARGB1887) {
            formatString = "COLOR_Format24bitARGB1887";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format24BitARGB6666) {
            formatString = "COLOR_Format24BitARGB6666";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format24bitBGR888) {
            formatString = "COLOR_Format24bitBGR888";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format24bitRGB888) {
            formatString = "COLOR_Format24bitRGB888";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format25bitARGB1888) {
            formatString = "COLOR_Format25bitARGB1888";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888) {
            formatString = "COLOR_Format32bitARGB8888";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format32bitBGRA8888) {
            formatString = "COLOR_Format32bitBGRA8888";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format8bitRGB332) {
            formatString = "COLOR_Format8bitRGB332";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatCbYCrY) {
            formatString = "COLOR_FormatCbYCrY";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatCrYCbY) {
            formatString = "COLOR_FormatCrYCbY";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatL16) {
            formatString = "COLOR_FormatL16";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatL2) {
            formatString = "COLOR_FormatL2";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatL24) {
            formatString = "COLOR_FormatL24";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatL32) {
            formatString = "COLOR_FormatL32";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatL4) {
            formatString = "COLOR_FormatL4";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatL8) {
            formatString = "COLOR_FormatL8";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatMonochrome) {
            formatString = "COLOR_FormatMonochrome";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer10bit) {
            formatString = "COLOR_FormatRawBayer10bit";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer8bit) {
            formatString = "COLOR_FormatRawBayer8bit";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer8bitcompressed) {
            formatString = "COLOR_FormatRawBayer8bitcompressed";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYCbYCr) {
            formatString = "COLOR_FormatYCbYCr";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYCrYCb) {
            formatString = "COLOR_FormatYCrYCb";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411PackedPlanar) {
            formatString = "COLOR_FormatYUV411PackedPlanar";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411Planar) {
            formatString = "COLOR_FormatYUV411Planar";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) {
            formatString = "COLOR_FormatYUV420PackedPlanar";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar) {
            formatString = "COLOR_FormatYUV420PackedSemiPlanar";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedPlanar) {
            formatString = "COLOR_FormatYUV422PackedPlanar";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedSemiPlanar) {
            formatString = "COLOR_FormatYUV422PackedSemiPlanar";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Planar) {
            formatString = "COLOR_FormatYUV422Planar";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedSemiPlanar) {
            formatString = "COLOR_FormatYUV422PackedSemiPlanar";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Planar) {
            formatString = "COLOR_FormatYUV422Planar";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422SemiPlanar) {
            formatString = "COLOR_FormatYUV422SemiPlanar";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444Interleaved) {
            formatString = "COLOR_FormatYUV444Interleaved";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar) {
            formatString = "COLOR_QCOM_FormatYUV420SemiPlanar";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar) {
            formatString = "COLOR_TI_FormatYUV420PackedSemiPlanar";
        } else if (colorFormat == QOMX_COLOR_FormatYUV420PackedSemiPlanar64x32Tile2m8ka) {
            formatString = "QOMX_COLOR_FormatYUV420PackedSemiPlanar64x32Tile2m8ka";
        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
            formatString = "COLOR_FormatYUV420Planar";
        }

        Log.i(TAG, " formatString: " + formatString);
    }

    /**
     * Work loop.
     */
    static void doExtract1(MediaExtractor extractor, int trackIndex,
            MediaCodec decoder, CodecOutputSurface outputSurface)
            throws IOException {
        final int TIMEOUT_USEC = 100000;
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inputChunk = 0;
        int decodeCount = 0;
        long frameSaveTime = 0;

        boolean outputDone = false;
        boolean inputDone = false;
        while (!outputDone) {
            if (VERBOSE)
                Log.d(TAG, "loop");

            // Feed more data to the decoder.
            if (!inputDone) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                    // Read the sample data into the ByteBuffer. This neither
                    // respects nor
                    // updates inputBuf's position, limit, etc.
                    int chunkSize = extractor.readSampleData(inputBuf, 0);
                    if (chunkSize < 0) {
                        // End of stream -- send empty frame with EOS flag set.
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        if (VERBOSE)
                            Log.d(TAG, "sent input EOS");
                    } else {
                        if (extractor.getSampleTrackIndex() != trackIndex) {
                            Log.w(TAG, "WEIRD: got sample from track "
                                    + extractor.getSampleTrackIndex()
                                    + ", expected " + trackIndex);
                        }
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufIndex, 0, chunkSize,
                                presentationTimeUs, 0 /* flags */);
                        if (VERBOSE) {
                            Log.d(TAG, "submitted frame " + inputChunk
                                    + " to dec, size=" + chunkSize);
                        }
                        inputChunk++;
                        extractor.advance();
                    }
                } else {
                    if (VERBOSE)
                        Log.d(TAG, "input buffer not available");
                }
            }

            if (!outputDone) {
                int decoderStatus = decoder.dequeueOutputBuffer(info,
                        TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE)
                        Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not important for us, since we're using Surface
                    if (VERBOSE)
                        Log.d(TAG, "decoder output buffers changed");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = decoder.getOutputFormat();
                    if (VERBOSE)
                        Log.d(TAG, "decoder output format changed: "
                                + newFormat);
                } else if (decoderStatus < 0) {
                    Log.e(TAG,
                            "unexpected result from decoder.dequeueOutputBuffer: "
                                    + decoderStatus);
                } else { // decoderStatus >= 0
                    if (VERBOSE)
                        Log.d(TAG, "surface decoder given buffer "
                                + decoderStatus + " (size=" + info.size + ")");
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE)
                            Log.d(TAG, "output EOS");
                        outputDone = true;
                    }

                    boolean doRender = (info.size != 0);

                    // As soon as we call releaseOutputBuffer, the buffer will
                    // be forwarded
                    // to SurfaceTexture to convert to a texture. The API
                    // doesn't guarantee
                    // that the texture will be available before the call
                    // returns, so we
                    // need to wait for the onFrameAvailable callback to fire.
                    decoder.releaseOutputBuffer(decoderStatus, doRender);
                    if (doRender) {
                        if (VERBOSE)
                            Log.d(TAG, "awaiting decode of frame "
                                    + decodeCount);
                        outputSurface.awaitNewImage();
                        outputSurface.drawImage(true);
                        Log.v(TAG, " decodeCount: " + decodeCount);
                        if (decodeCount < MAX_FRAMES) {
                            File outputFile = new File(
                                    FILES_DIR,
                                    String.format("frame-%02d.png", decodeCount));
                            long startWhen = System.nanoTime();
                            outputSurface.saveFrame(outputFile.toString());
                            frameSaveTime += System.nanoTime() - startWhen;
                        }
                        decodeCount++;
                    }
                }
            }
        }

        int numSaved = (MAX_FRAMES < decodeCount) ? MAX_FRAMES : decodeCount;
        Log.d(TAG, "Saving " + numSaved + " frames took "
                + (frameSaveTime / numSaved / 1000) + " us per frame");
    }

}
