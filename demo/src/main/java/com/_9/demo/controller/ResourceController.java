package com._9.demo.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import com._9.demo.repository.ResourceRepository;
import com._9.demo.repository.UserRepository;
import com._9.demo.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ResourceController {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    
    // 假设这是您的文件根目录
    private static final String FILE_ROOT = "/root/";
    
    // 移除硬编码的 VIDEO_CONTENT_TYPE

    public ResourceController(ResourceRepository resourceRepository,UserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }
    
    // ----------------------------------------------------
    // 用户认证和资源列表接口
    // ----------------------------------------------------
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("用户名已存在!"); 
        }
        User newUser = new User();
        newUser.setUsername(user.getUsername());
        newUser.setPassword(user.getPassword());
        newUser.setDownloadHistory("{}");
        userRepository.save(newUser);
        return ResponseEntity.ok("注册成功!");
    }
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {

        // 1. 查找用户
        return userRepository.findByUsername(request.getUsername())
            .map(user -> {
                // 2. 验证旧密码 (基于您现有的登录/注册逻辑，使用明文比较)
                if (!user.getPassword().equals(request.getOldPassword())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("旧密码错误!");
                }
                
                // 3. 检查新密码是否符合基本要求 (如长度)
                if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                    return ResponseEntity.badRequest().body("新密码至少需要 6 位!");
                }
                
                // 4. 保存新密码 (⚠️ 警告：明文存储)
                user.setPassword(request.getNewPassword());
                userRepository.save(user);
                
                return ResponseEntity.ok("密码更改成功，请重新登录。");
            })
            .orElseGet(() -> {
                // 统一返回，不暴露用户是否存在的信息
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户不存在或旧密码错误!");
            });
    }
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User loginRequest) {
        return userRepository.findByUsername(loginRequest.getUsername())
            .map(user -> {
                if (user.getPassword().equals(loginRequest.getPassword())) {
                    return ResponseEntity.ok("登录成功");
                } else {
                    return ResponseEntity.badRequest().body("用户名或密码错误");
                }
            })
            .orElseGet(() -> {
                return ResponseEntity.badRequest().body("用户名或密码错误");
            });
    }
    
    @GetMapping("/resources")
    public List<com._9.demo.model.Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    // ----------------------------------------------------
    // 流媒体接口 (视频和音频 FLAC/MP3 等)
    // ----------------------------------------------------
    /**
     * 新增的流媒体接口，供在线播放器使用。
     * 响应头设置支持 Range Request，实现流式传输。
     */
    @GetMapping("/stream/resource/{type}/{fileKey}")
    public ResponseEntity<org.springframework.core.io.Resource> streamResource(
            @PathVariable String type,
            @PathVariable String fileKey,
            HttpServletRequest request) {

        // 1. 查找资源
        com._9.demo.model.Resource resource = resourceRepository.findByFileKeyAndFileType(fileKey, type);
        
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. 调用流式传输服务
        String fullPath = FILE_ROOT + type + "/" + fileKey;
        File file = new File(fullPath);

        if (!file.exists() || !file.canRead()) {
            return ResponseEntity.notFound().build();
        }
        
        // 3. 动态获取 Content-Type
        String contentType = getContentType(fileKey);
        
        try {
            // 4. 使用动态 Content-Type 调用 serveStream
            return serveStream(file, contentType, request);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ----------------------------------------------------
    // 下载接口 (保持不变，用于强制下载)
    // ----------------------------------------------------
 // ResourceController.java (修改后的 downloadResource 方法)

    @GetMapping("/download/resource/{type}/{fileKey}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadResource(
            @PathVariable String type,
            @PathVariable String fileKey,
            @RequestParam(required = true) String username) { // <--- 新增 username 参数

        // 1. 查找资源
        com._9.demo.model.Resource resource = resourceRepository.findByFileKeyAndFileType(fileKey, type);
        
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 2. 更新资源下载次数
        resource.setTimes(resource.getTimes() + 1);
        resourceRepository.save(resource);
        
        // 3. 记录用户下载历史 <--- 新增的逻辑
        userRepository.findByUsername(username).ifPresent(user -> {
            // 假设 Resource 类有一个 getTitle() 方法
            updateDownloadHistory(user, resource.getName());
        }); 

        // 4. 正常返回文件
        return serveFile(type, fileKey, false);
    }
    
    @PostMapping("/download-history")
    public ResponseEntity<?> getDownloadHistory(@RequestBody java.util.Map<String, String> requestBody) {
        
        String username = requestBody.get("username");

        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body("缺少用户名参数。");
        }

        // 🎯 显式将 Optional<User> 存储起来，避免复杂的链式推断
        java.util.Optional<com._9.demo.model.User> userOptional = userRepository.findByUsername(username);

        // 检查 Optional 是否包含值
        if (userOptional.isPresent()) {
            // 如果存在，返回成功响应
            com._9.demo.model.User user = userOptional.get();
            return ResponseEntity.ok(user.getDownloadHistory());
        } else {
            // 如果不存在，返回错误响应
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户不存在。");
        }
    }

    @GetMapping("/download/cover/{coverName}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadCover(
            @PathVariable String coverName) {
        return serveFile("封面", coverName, true); // true: 内联显示
    }
    
    // ----------------------------------------------------
    // 核心辅助方法
    // ----------------------------------------------------
    
    /**
     * 根据文件后缀名动态获取 Content-Type/MIME Type
     */
    private String getContentType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "application/octet-stream";
        }
        
        String lowerCaseFileName = fileName.toLowerCase();
        
        if (lowerCaseFileName.endsWith(".mp4") || lowerCaseFileName.endsWith(".m4v")) {
            return "video/mp4";
        }
        if (lowerCaseFileName.endsWith(".flac")) {
            return "audio/flac"; // FLAC 音频
        }
        if (lowerCaseFileName.endsWith(".mp3")) {
            return "audio/mpeg"; // MP3 音频
        }
        if (lowerCaseFileName.endsWith(".ogg") || lowerCaseFileName.endsWith(".oga")) {
            return "audio/ogg";
        }
        if (lowerCaseFileName.endsWith(".txt")) {
            return "text/plain; charset=GBK"; // 假设您的 TXT 是 GBK 编码
        }
        // 如果无法识别，返回默认的二进制流类型
        return "application/octet-stream";
    }
    
    /**
     * 服务于非流式传输（下载或内联图片）
     */
    private ResponseEntity<org.springframework.core.io.Resource> serveFile(
            String type, String fileName, boolean isInline) {

        String fullPath = FILE_ROOT + type + "/" + fileName;
        File file = new File(fullPath);

        try {
            org.springframework.core.io.Resource resource = new UrlResource(file.toURI());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String encodedFilename = java.net.URLEncoder.encode(file.getName(), "UTF-8")
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(file.length());

            if (isInline) {
                // 封面或图片，尝试内联显示
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFilename + "\"");
                // 尝试设置 Content-Type，但这里逻辑可能不全，可以考虑使用 getContentType()
                if (type.equals("封面")) headers.set(HttpHeaders.CONTENT_TYPE, "image/jpeg"); 
            } else {
                // 强制下载
                headers.set(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename);
                // 强制下载时使用二进制流，通用性强
                headers.set(HttpHeaders.CONTENT_TYPE, "application/octet-stream"); 
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
     * 服务于支持 Range Header 的流式传输 (例如视频和音频)。
     */
    private ResponseEntity<org.springframework.core.io.Resource> serveStream(
            File file, String contentType, HttpServletRequest request) throws IOException {

        org.springframework.core.io.Resource resource = new UrlResource(file.toURI());
        
        // 核心：设置流媒体传输的关键 Headers
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes"); // 告诉客户端（播放器）支持 Range 请求
        headers.set(HttpHeaders.CONTENT_TYPE, contentType); // 动态设置 Content-Type
        
        // 设置 Content-Disposition 为 inline，表示在浏览器内播放
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"");

        // 检查 Range Header
        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        
        if (rangeHeader != null) {
            // 存在 Range Header，直接返回 200 OK 并带上所有信息。
            // 容器（如 Tomcat/Jetty）会自动将状态码改为 206 Partial Content
            // 并处理分块传输，Spring 框架负责大部分工作。
            return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .contentLength(file.length())
                .body(resource);

        } else {
            // 没有 Range Header (首次请求)，返回 200 OK
            return ResponseEntity.status(HttpStatus.OK)
                    .headers(headers)
                    .contentLength(file.length())
                    .body(resource);
        }
    }
 // ResourceController.java (新增辅助方法)

    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson JSON 处理实例

    /**
     * 将资源标题添加到用户的下载历史中
     * 历史格式：{"资源标题A": 3, "资源标题B": 1}
     */
    private void updateDownloadHistory(User user, String resourceTitle) {
        try {
            // 1. 反序列化 JSON 字符串到 Map
            // 注意：使用 TypeReference 来正确处理泛型 Map
            TypeReference<java.util.Map<String, Integer>> typeRef = new TypeReference<java.util.Map<String, Integer>>() {};
            java.util.Map<String, Integer> historyMap = objectMapper.readValue(user.getDownloadHistory(), typeRef);
            
            // 2. 更新下载次数：如果存在则加 1，否则设为 1
            historyMap.merge(resourceTitle, 1, Integer::sum);
            
            // 3. 序列化 Map 回 JSON 字符串并保存
            user.setDownloadHistory(objectMapper.writeValueAsString(historyMap));
            userRepository.save(user);
            
        } catch (IOException e) {
            System.err.println("Error updating download history for user " + user.getUsername() + ": " + e.getMessage());
            // 可以在这里选择抛出异常或静默失败
        }
    }
}