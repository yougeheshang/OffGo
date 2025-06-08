package org.tinkerhub.offgo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class ImgBBService {
    private static final Logger logger = LoggerFactory.getLogger(ImgBBService.class);
    
    @Value("${imgbb.api.key}")
    private String apiKey;
    
    @Value("${imgbb.api.url}")
    private String apiUrl;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public ImgBBService() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public String uploadImage(MultipartFile file) throws IOException {
        try {
            logger.info("Starting image upload to ImgBB for file: {}", file.getOriginalFilename());
            
            // 将文件转换为 base64
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            
            // 构建请求体
            RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", apiKey)
                .addFormDataPart("image", base64Image)
                .build();
            
            // 构建请求
            Request request = new Request.Builder()
                .url(apiUrl)
                .post(formBody)
                .build();
            
            // 发送请求
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.error("ImgBB API request failed with code: {}, error: {}", response.code(), errorBody);
                    throw new IOException("Unexpected response code: " + response.code());
                }
                
                // 解析响应
                String responseBody = response.body().string();
                JsonNode rootNode = objectMapper.readTree(responseBody);
                
                if (rootNode.get("success").asBoolean()) {
                    String imageUrl = rootNode.get("data").get("url").asText();
                    logger.info("Image uploaded successfully to ImgBB, URL: {}", imageUrl);
                    return imageUrl;
                } else {
                    String errorMessage = rootNode.has("error") ? rootNode.get("error").asText() : "Unknown error";
                    logger.error("ImgBB upload failed: {}", errorMessage);
                    throw new IOException("Upload failed: " + errorMessage);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to upload image to ImgBB", e);
            throw new IOException("Failed to upload image: " + e.getMessage());
        }
    }
} 