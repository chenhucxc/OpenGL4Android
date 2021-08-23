//
// Created by gavinandre on 17-5-3.
//

#ifndef ANDROIDLOG_H
#define ANDROIDLOG_H

#include <android/log.h>

#define LOG_TAG    "ObjectDetectNative"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#endif //ANDROIDLOG_H
