package ai.kastrax.rag.vectorstore;

/**
 * FAISS JNI 绑定类。
 * 
 * 这个类提供了与 FAISS C++ 库的 JNI 绑定。
 * 注意：使用这个类需要安装 FAISS 库并编译 JNI 绑定。
 */
public class FaissJNI {
    
    /**
     * 创建 Flat 索引。
     *
     * @param dimension 向量维度
     * @param metricType 度量类型（0 表示 L2，1 表示 IP）
     * @return 索引指针
     */
    public static native long createFlatIndex(int dimension, int metricType);
    
    /**
     * 创建 IVFFlat 索引。
     *
     * @param dimension 向量维度
     * @param nlist 聚类中心的数量
     * @param metricType 度量类型（0 表示 L2，1 表示 IP）
     * @return 索引指针
     */
    public static native long createIVFFlatIndex(int dimension, int nlist, int metricType);
    
    /**
     * 设置 nprobe 参数。
     *
     * @param indexPointer 索引指针
     * @param nprobe 搜索时探测的聚类数量
     */
    public static native void setNprobe(long indexPointer, int nprobe);
    
    /**
     * 添加向量到索引。
     *
     * @param indexPointer 索引指针
     * @param vector 向量数据
     * @param id 向量 ID
     */
    public static native void addVector(long indexPointer, float[] vector, int id);
    
    /**
     * 搜索向量。
     *
     * @param indexPointer 索引指针
     * @param queryVector 查询向量
     * @param k 返回结果的最大数量
     * @return 搜索结果（ID 和距离交替排列）
     */
    public static native float[] search(long indexPointer, float[] queryVector, int k);
    
    /**
     * 重置索引。
     *
     * @param indexPointer 索引指针
     */
    public static native void resetIndex(long indexPointer);
    
    /**
     * 释放索引。
     *
     * @param indexPointer 索引指针
     */
    public static native void releaseIndex(long indexPointer);
    
    /**
     * 写入索引到文件。
     *
     * @param indexPointer 索引指针
     * @param filePath 文件路径
     */
    public static native void writeIndex(long indexPointer, String filePath);
    
    /**
     * 从文件读取索引。
     *
     * @param filePath 文件路径
     * @return 索引指针
     */
    public static native long readIndex(String filePath);
}
