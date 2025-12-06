package com._9.demo.controller;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com._9.demo.repository.ResourceRepository;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ResourceController {

    private final ResourceRepository resourceRepository;

    // 允许的资源类型（必须与你的实际文件夹名完全一致）
    private static final String[] ALLOWED_TYPES = {"文档", "音乐", "视频", "应用", "封面"};

    public ResourceController(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    /**
     * 获取所有资源列表
     */
    @GetMapping("/resources")
    public List<com._9.demo.model.Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    /**
     * 下载普通资源文件（如文档、音乐等）
     * URL: /api/download/resource/{fileType}/{fileKey}
     */
    @GetMapping("/download/resource/{type}/{fileKey}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadResource(
            @PathVariable String type,
            @PathVariable String fileKey) {
        return serveFile(type, fileKey, false);
    }

    /**
     * 获取封面图片（用于 <img> 标签显示）
     * URL: /api/download/cover/{coverName}
     */
    @GetMapping("/download/cover/{coverName}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadCover(
            @PathVariable String coverName) {
        return serveFile("封面", coverName, true);
    }

    /**
     * 核心文件服务方法
     *
     * @param type      文件类型（即文件夹名）
     * @param fileName  文件名
     * @param isInline  是否以 inline 方式返回（用于图片显示）
     */
    private ResponseEntity<org.springframework.core.io.Resource> serveFile(
            String type, String fileName, boolean isInline) {

        // 安全：校验类型是否在允许列表中
        if (!Arrays.asList(ALLOWED_TYPES).contains(type)) {
            return ResponseEntity.badRequest().build();
        }

        // 安全：防止路径遍历攻击
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        // 构建文件路径
        String fullPath = "/root/" + type + "/" + fileName;
        File file = new File(fullPath);

        // 检查文件是否存在
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        try {
            org.springframework.core.io.Resource resource = new UrlResource(file.toURI());

            // UTF-8 编码文件名（兼容中文）
            String encodedFilename = java.net.URLEncoder.encode(file.getName(), "UTF-8")
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(file.length());

            // 设置 Content-Disposition
            if (isInline && isImageFile(fileName)) {
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFilename + "\"");
            } else {
                headers.set(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename);
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 判断是否为常见图片格式
     */
    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".webp") || lower.endsWith(".bmp");
    }
}