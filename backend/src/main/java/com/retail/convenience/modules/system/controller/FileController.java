package com.retail.convenience.modules.system.controller;

import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.modules.system.entity.SysFile;
import com.retail.convenience.modules.system.service.StorageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ApiResult<Map<String, Object>> upload(@RequestPart("file") MultipartFile file,
                                                 @RequestParam(defaultValue = "GENERAL") String bizType) {
        String contentType = file.getContentType();
        if (contentType != null && (contentType.startsWith("image/"))
                && file.getSize() > 5 * 1024 * 1024) {
            throw new BizException("图片不能超过 5MB");
        }
        SysFile saved = storageService.upload(file, bizType);
        return ApiResult.ok(Map.of(
                "id", saved.getId(),
                "name", saved.getName(),
                "url", saved.getUrl(),
                "path", saved.getPath(),
                "size", saved.getSize(),
                "contentType", saved.getContentType() == null ? "" : saved.getContentType()
        ));
    }

    @DeleteMapping
    public ApiResult<Void> delete(@RequestParam String path) {
        storageService.delete(path);
        return ApiResult.ok();
    }
}
