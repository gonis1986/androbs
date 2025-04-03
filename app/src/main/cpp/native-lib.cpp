#include <jni.h>
#include <android/log.h>
#include <gst/gst.h>

#define LOG_TAG "GStreamerNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL
Java_com_gonis_androbs_GStreamerHelper_initGStreamer(JNIEnv *env, jobject thiz) {
    gst_init(nullptr, nullptr);
    LOGD("GStreamer inizializzato correttamente");

    GstElement *pipeline = gst_parse_launch("videotestsrc ! autovideosink", nullptr);
    if (!pipeline) {
        LOGE("Errore nel parsing della pipeline");
        return;
    }

    gst_element_set_state(pipeline, GST_STATE_PLAYING);
    LOGD("Pipeline GStreamer avviata");
}

// Funzione sendToGStreamer che processa i frame
extern "C" JNIEXPORT void JNICALL
Java_com_gonis_androbs_CameraXHelper_sendToGStreamer(JNIEnv *env, jobject thiz, jbyteArray data) {
    LOGD("sendToGStreamer chiamata con %d byte", env->GetArrayLength(data));

    // Elaborazione del byte[] ricevuto (se necessario)
    // Esempio di invio dei dati a GStreamer
}

// Funzione di inizializzazione chiamata dalla MainActivity
extern "C" JNIEXPORT void JNICALL
Java_com_gonis_androbs_MainActivityKt_initGStreamer(JNIEnv *env, jobject thiz) {
    LOGD("initGStreamer chiamata da MainActivityKt");
    // Aggiungi l'inizializzazione di GStreamer se necessario
}


