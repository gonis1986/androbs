package com.gonis.androbs

class GStreamerHelper {
    external fun initGStreamer()

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}
