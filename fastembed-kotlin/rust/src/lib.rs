use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::{Arc, Mutex};

use anyhow::{anyhow, Result};
use fastembed::{Embedding, EmbeddingModel, InitOptions, TextEmbedding};
use jni::objects::{JClass, JFloatArray, JObject, JObjectArray, JString};
use jni::sys::{jboolean, jfloat, jfloatArray, jint, jlong, jsize};
use jni::JNIEnv;
use log::{debug, error, info};

// Global model cache to avoid recreating models
lazy_static::lazy_static! {
    static ref MODEL_CACHE: Mutex<HashMap<jlong, Arc<TextEmbedding>>> = Mutex::new(HashMap::new());
    static ref NEXT_MODEL_ID: Mutex<jlong> = Mutex::new(1);
}

// Helper function to get the next model ID
fn get_next_model_id() -> jlong {
    let mut id = NEXT_MODEL_ID.lock().unwrap();
    let current = *id;
    *id += 1;
    current
}

// Helper function to convert Java string to Rust string
fn jstring_to_string(env: &mut JNIEnv, jstring: &JString) -> Result<String> {
    let string = env.get_string(jstring)?;
    Ok(string.to_str()?.to_string())
}

// Helper function to convert Java string array to Rust vector of strings
fn jstring_array_to_vec(env: &mut JNIEnv, array: &JObjectArray) -> Result<Vec<String>> {
    let length = env.get_array_length(array)?;
    let mut result = Vec::with_capacity(length as usize);

    for i in 0..length {
        let jstring = env.get_object_array_element(array, i)?;
        let jstring_ref = jstring.into();
        let string = jstring_to_string(env, &jstring_ref)?;
        result.push(string);
    }

    Ok(result)
}

// Helper function to convert Embedding to Java float array
fn embedding_to_jfloat_array<'a>(env: &'a mut JNIEnv, embedding: &Embedding) -> Result<JFloatArray<'a>> {
    let length = embedding.len();
    let jarray = env.new_float_array(length as jsize)?;

    // Convert Vec<f32> to jfloat array
    let elements: Vec<jfloat> = embedding.iter().map(|&x| x as jfloat).collect();
    env.set_float_array_region(&jarray, 0, &elements)?;

    Ok(jarray)
}

// Helper function to convert Vec<Embedding> to Java array of float arrays
fn embeddings_to_jfloat_array_array<'a>(
    env: &'a mut JNIEnv,
    embeddings: &[Embedding],
) -> Result<JObjectArray<'a>> {
    // Get the float array class
    let float_array_class = env.find_class("[F")?;

    // Create a new array of float arrays
    let result = env.new_object_array(
        embeddings.len() as jsize,
        float_array_class,
        JObject::null(),
    )?;

    // Fill the array with embeddings
    for (i, embedding) in embeddings.iter().enumerate() {
        // Create a new embedding for each iteration to avoid borrowing issues
        let jembedding = {
            let length = embedding.len();
            let jarray = env.new_float_array(length as jsize)?;
            let elements: Vec<jfloat> = embedding.iter().map(|&x| x as jfloat).collect();
            env.set_float_array_region(&jarray, 0, &elements)?;
            jarray
        };
        env.set_object_array_element(&result, i as jsize, &jembedding)?;
    }

    Ok(result)
}

// Helper function to get model by ID
fn get_model(model_id: jlong) -> Result<Arc<TextEmbedding>> {
    let cache = MODEL_CACHE.lock().unwrap();
    cache.get(&model_id)
        .cloned()
        .ok_or_else(|| anyhow!("Model not found with ID: {}", model_id))
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_fastembed_TextEmbeddingNative_initLogger(
    _env: JNIEnv,
    _class: JClass,
) {
    let _ = env_logger::try_init();
    info!("FastEmbed JNI logger initialized");
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_fastembed_TextEmbeddingNative_createModel(
    mut env: JNIEnv,
    _class: JClass,
    model_type: jint,
    cache_dir: JString,
    show_download_progress: jboolean,
) -> jlong {
    // Convert model type to EmbeddingModel
    let model_enum = match model_type {
        0 => EmbeddingModel::BGESmallENV15,
        1 => EmbeddingModel::BGEBaseENV15,
        2 => EmbeddingModel::BGESmallZHV15,
        3 => EmbeddingModel::BGEBaseENV15, // Using BGEBaseENV15 as fallback for BGEBaseZHV15
        4 => EmbeddingModel::AllMiniLML6V2,
        5 => EmbeddingModel::AllMiniLML6V2Q,
        6 => EmbeddingModel::MultilingualE5Small, // Using MultilingualE5Small
        7 => EmbeddingModel::MultilingualE5Large, // Using MultilingualE5Large
        _ => {
            error!("Invalid model type: {}", model_type);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Invalid model type: {}", model_type);
            let _ = env.throw_new(exception_class, error_message);
            return 0;
        }
    };

    // Convert cache_dir to Rust string
    let cache_dir_str = if !cache_dir.is_null() {
        match jstring_to_string(&mut env, &cache_dir) {
            Ok(s) => Some(s),
            Err(e) => {
                error!("Error converting cache_dir: {}", e);
                let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
                let error_message = format!("Failed to convert cache_dir: {}", e);
                let _ = env.throw_new(exception_class, error_message);
                return 0;
            }
        }
    } else {
        None
    };

    // Create InitOptions
    let mut options = InitOptions::new(model_enum.clone());

    // Set cache directory if provided
    if let Some(dir) = cache_dir_str {
        let path = PathBuf::from(dir);
        options = options.with_cache_dir(path);
    }

    // Set download progress flag
    options = options.with_show_download_progress(show_download_progress != 0);

    // Create the model
    info!("Creating TextEmbedding model: {:?}", model_enum);
    let model = match TextEmbedding::try_new(options) {
        Ok(m) => m,
        Err(e) => {
            error!("Error creating model: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to create model: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            return 0;
        }
    };

    // Get a new model ID
    let model_id = get_next_model_id();

    // Store the model in the cache
    let mut cache = MODEL_CACHE.lock().unwrap();
    cache.insert(model_id, Arc::new(model));

    info!("Model created with ID: {}", model_id);
    model_id
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_fastembed_TextEmbeddingNative_embedTexts(
    mut env: JNIEnv,
    _class: JClass,
    model_id: jlong,
    texts: JObjectArray,
    batch_size: jint,
) -> *mut ::jni::sys::_jobject {
    // Get the model from the cache
    let model = match get_model(model_id) {
        Ok(m) => m,
        Err(e) => {
            error!("Error getting model: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to get model: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            return std::ptr::null_mut();
        }
    };

    // Convert Java string array to Rust vector
    let texts_vec = match jstring_array_to_vec(&mut env, &texts) {
        Ok(v) => v,
        Err(e) => {
            error!("Error converting texts: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to convert texts: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            return std::ptr::null_mut();
        }
    };

    // Determine batch size
    let batch_size = if batch_size <= 0 {
        None
    } else {
        Some(batch_size as usize)
    };

    // Generate embeddings
    debug!("Generating embeddings for {} texts", texts_vec.len());
    let embeddings = match model.embed(texts_vec, batch_size) {
        Ok(e) => e,
        Err(e) => {
            error!("Error generating embeddings: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to generate embeddings: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            return std::ptr::null_mut();
        }
    };

    // Convert embeddings to Java array
    match embeddings_to_jfloat_array_array(&mut env, &embeddings) {
        Ok(result) => result.into_raw(),
        Err(e) => {
            error!("Error converting embeddings to Java array: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to convert embeddings to Java array: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_fastembed_TextEmbeddingNative_embedText(
    mut env: JNIEnv,
    _class: JClass,
    model_id: jlong,
    text: JString,
) -> *mut ::jni::sys::_jobject {
    // Get the model from the cache
    let model = match get_model(model_id) {
        Ok(m) => m,
        Err(e) => {
            error!("Error getting model: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to get model: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            return std::ptr::null_mut();
        }
    };

    // Convert Java string to Rust string
    let text_str = match jstring_to_string(&mut env, &text) {
        Ok(s) => s,
        Err(e) => {
            error!("Error converting text: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to convert text: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            return std::ptr::null_mut();
        }
    };

    // Generate embedding
    debug!("Generating embedding for text: {}", text_str);
    let embeddings = match model.embed(vec![text_str], None) {
        Ok(e) => e,
        Err(e) => {
            error!("Error generating embedding: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to generate embedding: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            return std::ptr::null_mut();
        }
    };

    if embeddings.is_empty() {
        error!("No embedding generated");
        let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
        let error_message = "No embedding generated";
        let _ = env.throw_new(exception_class, error_message);
        return std::ptr::null_mut();
    }

    // Convert embedding to Java float array
    match embedding_to_jfloat_array(&mut env, &embeddings[0]) {
        Ok(result) => result.into_raw(),
        Err(e) => {
            error!("Error converting embedding to Java array: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to convert embedding to Java array: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_fastembed_TextEmbeddingNative_releaseModel(
    _env: JNIEnv,
    _class: JClass,
    model_id: jlong,
) -> jboolean {
    let mut cache = MODEL_CACHE.lock().unwrap();
    let removed = cache.remove(&model_id).is_some();

    if removed {
        info!("Model with ID {} released", model_id);
    } else {
        info!("Model with ID {} not found", model_id);
    }

    if removed { 1 } else { 0 }
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_fastembed_TextEmbeddingNative_getEmbeddingDimension(
    mut env: JNIEnv,
    _class: JClass,
    model_id: jlong,
) -> jint {
    // Get the model from the cache
    let model = match get_model(model_id) {
        Ok(m) => m,
        Err(e) => {
            error!("Error getting model: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to get model: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            return -1;
        }
    };

    // Generate a dummy embedding to get the dimension
    let dummy_text = "test";
    let embeddings = match model.embed(vec![dummy_text.to_string()], None) {
        Ok(e) => e,
        Err(e) => {
            error!("Error generating embedding: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to generate embedding: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            return -1;
        }
    };

    if embeddings.is_empty() {
        error!("No embedding generated");
        let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
        let error_message = "No embedding generated";
        let _ = env.throw_new(exception_class, error_message);
        return -1;
    }

    embeddings[0].len() as jint
}

#[no_mangle]
pub extern "system" fn Java_ai_kastrax_fastembed_TextEmbeddingNative_cosineSimilarity(
    mut env: JNIEnv,
    _class: JClass,
    embedding1: JFloatArray,
    embedding2: JFloatArray,
) -> jfloat {
    // Get the lengths of the arrays
    let len1 = match env.get_array_length(&embedding1) {
        Ok(l) => l,
        Err(e) => {
            error!("Error getting array length: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to get array length: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            return 0.0;
        }
    };

    let len2 = match env.get_array_length(&embedding2) {
        Ok(l) => l,
        Err(e) => {
            error!("Error getting array length: {}", e);
            let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
            let error_message = format!("Failed to get array length: {}", e);
            let _ = env.throw_new(exception_class, error_message);
            return 0.0;
        }
    };

    if len1 != len2 {
        error!("Embeddings have different dimensions: {} vs {}", len1, len2);
        let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
        let error_message = format!("Embeddings have different dimensions: {} vs {}", len1, len2);
        let _ = env.throw_new(exception_class, error_message);
        return 0.0;
    }

    // Convert Java float arrays to Rust vectors
    let mut vec1 = vec![0.0; len1 as usize];
    let mut vec2 = vec![0.0; len2 as usize];

    if let Err(e) = env.get_float_array_region(&embedding1, 0, &mut vec1) {
        error!("Error getting float array region: {}", e);
        let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
        let error_message = format!("Failed to get float array region: {}", e);
        let _ = env.throw_new(exception_class, error_message);
        return 0.0;
    }

    if let Err(e) = env.get_float_array_region(&embedding2, 0, &mut vec2) {
        error!("Error getting float array region: {}", e);
        let exception_class = env.find_class("java/lang/RuntimeException").unwrap();
        let error_message = format!("Failed to get float array region: {}", e);
        let _ = env.throw_new(exception_class, error_message);
        return 0.0;
    }

    // Calculate cosine similarity
    let mut dot_product = 0.0;
    let mut norm1 = 0.0;
    let mut norm2 = 0.0;

    for i in 0..len1 as usize {
        dot_product += vec1[i] * vec2[i];
        norm1 += vec1[i] * vec1[i];
        norm2 += vec2[i] * vec2[i];
    }

    norm1 = norm1.sqrt();
    norm2 = norm2.sqrt();

    if norm1 == 0.0 || norm2 == 0.0 {
        return 0.0;
    }

    dot_product / (norm1 * norm2)
}
