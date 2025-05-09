#include <jni.h>
#include <faiss/IndexFlat.h>
#include <faiss/IndexIVFFlat.h>
#include <faiss/index_io.h>
#include <iostream>
#include <vector>

// 辅助函数：检查并处理异常
static void handleException(JNIEnv *env) {
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

// 创建 Flat 索引
extern "C" JNIEXPORT jlong JNICALL Java_ai_kastrax_rag_vectorstore_FaissJNI_createFlatIndex
  (JNIEnv *env, jclass cls, jint dimension, jint metricType) {
    try {
        faiss::MetricType metric = metricType == 0 ? faiss::METRIC_L2 : faiss::METRIC_INNER_PRODUCT;
        faiss::IndexFlat *index = new faiss::IndexFlat(dimension, metric);
        return (jlong)index;
    } catch (std::exception &e) {
        std::cerr << "Error creating Flat index: " << e.what() << std::endl;
        return 0;
    }
}

// 创建 IVFFlat 索引
extern "C" JNIEXPORT jlong JNICALL Java_ai_kastrax_rag_vectorstore_FaissJNI_createIVFFlatIndex
  (JNIEnv *env, jclass cls, jint dimension, jint nlist, jint metricType) {
    try {
        faiss::MetricType metric = metricType == 0 ? faiss::METRIC_L2 : faiss::METRIC_INNER_PRODUCT;
        faiss::IndexFlat *quantizer = new faiss::IndexFlat(dimension, metric);
        faiss::IndexIVFFlat *index = new faiss::IndexIVFFlat(quantizer, dimension, nlist, metric);
        
        // 训练空索引
        std::vector<float> trainData(dimension * nlist);
        for (int i = 0; i < dimension * nlist; i++) {
            trainData[i] = (float)rand() / RAND_MAX;
        }
        index->train(nlist, trainData.data());
        
        return (jlong)index;
    } catch (std::exception &e) {
        std::cerr << "Error creating IVFFlat index: " << e.what() << std::endl;
        return 0;
    }
}

// 设置 nprobe 参数
extern "C" JNIEXPORT void JNICALL Java_ai_kastrax_rag_vectorstore_FaissJNI_setNprobe
  (JNIEnv *env, jclass cls, jlong indexPointer, jint nprobe) {
    try {
        faiss::IndexIVFFlat *index = (faiss::IndexIVFFlat *)indexPointer;
        index->nprobe = nprobe;
    } catch (std::exception &e) {
        std::cerr << "Error setting nprobe: " << e.what() << std::endl;
        handleException(env);
    }
}

// 添加向量到索引
extern "C" JNIEXPORT void JNICALL Java_ai_kastrax_rag_vectorstore_FaissJNI_addVector
  (JNIEnv *env, jclass cls, jlong indexPointer, jfloatArray vector, jint id) {
    try {
        faiss::Index *index = (faiss::Index *)indexPointer;
        jfloat *vectorData = env->GetFloatArrayElements(vector, NULL);
        jsize vectorLength = env->GetArrayLength(vector);
        
        std::vector<faiss::idx_t> ids(1);
        ids[0] = id;
        
        index->add_with_ids(1, vectorData, ids.data());
        
        env->ReleaseFloatArrayElements(vector, vectorData, JNI_ABORT);
    } catch (std::exception &e) {
        std::cerr << "Error adding vector: " << e.what() << std::endl;
        handleException(env);
    }
}

// 搜索向量
extern "C" JNIEXPORT jfloatArray JNICALL Java_ai_kastrax_rag_vectorstore_FaissJNI_search
  (JNIEnv *env, jclass cls, jlong indexPointer, jfloatArray queryVector, jint k) {
    try {
        faiss::Index *index = (faiss::Index *)indexPointer;
        jfloat *queryData = env->GetFloatArrayElements(queryVector, NULL);
        jsize queryLength = env->GetArrayLength(queryVector);
        
        std::vector<faiss::idx_t> ids(k);
        std::vector<float> distances(k);
        
        index->search(1, queryData, k, distances.data(), ids.data());
        
        env->ReleaseFloatArrayElements(queryVector, queryData, JNI_ABORT);
        
        // 创建结果数组（ID 和距离交替排列）
        jfloatArray results = env->NewFloatArray(k * 2);
        if (results == NULL) {
            return NULL;
        }
        
        std::vector<float> resultsData(k * 2);
        for (int i = 0; i < k; i++) {
            resultsData[i * 2] = (float)ids[i];
            resultsData[i * 2 + 1] = distances[i];
        }
        
        env->SetFloatArrayRegion(results, 0, k * 2, resultsData.data());
        return results;
    } catch (std::exception &e) {
        std::cerr << "Error searching: " << e.what() << std::endl;
        handleException(env);
        return NULL;
    }
}

// 重置索引
extern "C" JNIEXPORT void JNICALL Java_ai_kastrax_rag_vectorstore_FaissJNI_resetIndex
  (JNIEnv *env, jclass cls, jlong indexPointer) {
    try {
        faiss::Index *index = (faiss::Index *)indexPointer;
        index->reset();
    } catch (std::exception &e) {
        std::cerr << "Error resetting index: " << e.what() << std::endl;
        handleException(env);
    }
}

// 释放索引
extern "C" JNIEXPORT void JNICALL Java_ai_kastrax_rag_vectorstore_FaissJNI_releaseIndex
  (JNIEnv *env, jclass cls, jlong indexPointer) {
    try {
        faiss::Index *index = (faiss::Index *)indexPointer;
        delete index;
    } catch (std::exception &e) {
        std::cerr << "Error releasing index: " << e.what() << std::endl;
        handleException(env);
    }
}

// 写入索引到文件
extern "C" JNIEXPORT void JNICALL Java_ai_kastrax_rag_vectorstore_FaissJNI_writeIndex
  (JNIEnv *env, jclass cls, jlong indexPointer, jstring filePath) {
    try {
        faiss::Index *index = (faiss::Index *)indexPointer;
        const char *path = env->GetStringUTFChars(filePath, NULL);
        
        faiss::write_index(index, path);
        
        env->ReleaseStringUTFChars(filePath, path);
    } catch (std::exception &e) {
        std::cerr << "Error writing index: " << e.what() << std::endl;
        handleException(env);
    }
}

// 从文件读取索引
extern "C" JNIEXPORT jlong JNICALL Java_ai_kastrax_rag_vectorstore_FaissJNI_readIndex
  (JNIEnv *env, jclass cls, jstring filePath) {
    try {
        const char *path = env->GetStringUTFChars(filePath, NULL);
        
        faiss::Index *index = faiss::read_index(path);
        
        env->ReleaseStringUTFChars(filePath, path);
        
        return (jlong)index;
    } catch (std::exception &e) {
        std::cerr << "Error reading index: " << e.what() << std::endl;
        handleException(env);
        return 0;
    }
}
