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


    public ResourceController(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @GetMapping("/resources")
    public List<com._9.demo.model.Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    @GetMapping("/download/resource/{type}/{fileKey}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadResource(
            @PathVariable String type,
            @PathVariable String fileKey) {
        return serveFile(type, fileKey, false);
    }

    @GetMapping("/download/cover/{coverName}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadCover(
            @PathVariable String coverName) {
        return serveFile("封面", coverName, true);
    }

    private ResponseEntity<org.springframework.core.io.Resource> serveFile(
            String type, String fileName, boolean isInline) {


        String fullPath = "/root/" + type + "/" + fileName;
        File file = new File(fullPath);

        try {
            org.springframework.core.io.Resource resource = new UrlResource(file.toURI());


            String encodedFilename = java.net.URLEncoder.encode(file.getName(), "UTF-8")
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(file.length());

 
            if (isInline) {
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


}