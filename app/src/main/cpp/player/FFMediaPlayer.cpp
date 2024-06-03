#include "FFMediaPlayer.h"
#include <render/audio/OpenSLRender.h>

void FFMediaPlayer::Init(JNIEnv *jniEnv, jobject obj, char *url) {
    jniEnv->GetJavaVM(&m_JavaVM);
    m_JavaObj = jniEnv->NewGlobalRef(obj);
    m_AudioDecoder = new AudioDecoder(url);

    m_AudioRender = new OpenSLRender();
    m_AudioDecoder->SetAudioRender(m_AudioRender);
    m_AudioDecoder->SetMessageCallback(this, PostMessage);
}

void FFMediaPlayer::UnInit() {
    LOGCATE("FFMediaPlayer::UnInit");

    if(m_AudioDecoder) {
        delete m_AudioDecoder;
        m_AudioDecoder = nullptr;
    }

    if(m_AudioRender) {
        delete m_AudioRender;
        m_AudioRender = nullptr;
    }

    bool isAttach = false;
    GetJNIEnv(&isAttach)->DeleteGlobalRef(m_JavaObj);
    if(isAttach)
        GetJavaVM()->DetachCurrentThread();

}

void FFMediaPlayer::Play() {
    LOGCATE("FFMediaPlayer::Play");
    if(m_AudioDecoder)
        m_AudioDecoder->Start();
}

void FFMediaPlayer::Pause() {
    LOGCATE("FFMediaPlayer::Pause");
    if(m_AudioDecoder)
        m_AudioDecoder->Pause();

}

void FFMediaPlayer::Stop() {
    LOGCATE("FFMediaPlayer::Stop");
    if(m_AudioDecoder)
        m_AudioDecoder->Stop();
}

void FFMediaPlayer::SeekToPosition(float position) {
    LOGCATE("FFMediaPlayer::SeekToPosition position=%f", position);
    if(m_AudioDecoder)
        m_AudioDecoder->SeekToPosition(position);
}

float FFMediaPlayer::GetDuration() {
    float duration ;
    if (m_AudioDecoder)
        duration = m_AudioDecoder->GetDuration();
    LOGCATE("FFMediaPlayer::GetDuration position=%f", duration);
    return duration;
}



JNIEnv *FFMediaPlayer::GetJNIEnv(bool *isAttach) {
    JNIEnv *env;
    int status;
    if (nullptr == m_JavaVM) {
        LOGCATE("FFMediaPlayer::GetJNIEnv m_JavaVM == nullptr");
        return nullptr;
    }
    *isAttach = false;
    status = m_JavaVM->GetEnv((void **)&env, JNI_VERSION_1_4);
    if (status != JNI_OK) {
        status = m_JavaVM->AttachCurrentThread(&env, nullptr);
        if (status != JNI_OK) {
            LOGCATE("FFMediaPlayer::GetJNIEnv failed to attach current thread");
            return nullptr;
        }
        *isAttach = true;
    }
    return env;
}

jobject FFMediaPlayer::GetJavaObj() {
    return m_JavaObj;
}

JavaVM *FFMediaPlayer::GetJavaVM() {
    return m_JavaVM;
}

void FFMediaPlayer::PostMessage(void *context, int msgType, float msgCode) {
    if(context != nullptr)
    {
        FFMediaPlayer *player = static_cast<FFMediaPlayer *>(context);
        bool isAttach = false;
        JNIEnv *env = player->GetJNIEnv(&isAttach);
        LOGCATE("FFMediaPlayer::PostMessage env=%p", env);
        if(env == nullptr)
            return;
        jobject javaObj = player->GetJavaObj();
        jmethodID mid = env->GetMethodID(env->GetObjectClass(javaObj), JAVA_PLAYER_EVENT_CALLBACK_API_NAME, "(IF)V");
        env->CallVoidMethod(javaObj, mid, msgType, msgCode);
        if(isAttach)
            player->GetJavaVM()->DetachCurrentThread();

    }
}


