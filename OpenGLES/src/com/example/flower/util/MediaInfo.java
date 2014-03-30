package com.example.flower.util;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.util.Log;

public class MediaInfo {
    private static final String TAG = MediaInfo.class.getSimpleName();

    /**
     * Log MediaCodec info
     */
    public static void queryMediaCodecInfo() {
        final int CodecCount = MediaCodecList.getCodecCount();
        if (CodecCount <= 0) {
            Log.w(TAG, "There are not codec in your devices");
        } else {
            showMediaCodecInfo(CodecCount);
        }
    }

    /**
     * FIXME use string builder to format friendly string.
     * @param codecCount
     */
    private static void showMediaCodecInfo(final int codecCount) {
        for (int i = 0; i < codecCount; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            Log.i(TAG, "Codec name: " + codecInfo.getName());
            Log.i(TAG, "isEncoder: " + codecInfo.isEncoder());

            String[] types = codecInfo.getSupportedTypes();
            Log.i(TAG, "CodecInfo support type: ");
            for (int j = 0; j < types.length; j++) {
                Log.i(TAG, i + " : " + types[j].toString());
            }
        }
    }
}
