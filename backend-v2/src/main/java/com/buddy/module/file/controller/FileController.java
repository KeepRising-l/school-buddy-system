package com.buddy.module.file.controller;

import com.buddy.common.context.UserContext;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传控制器（等价迁移自旧版）
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    /** 上传目录 */
    private static final String UPLOAD_DIR = "uploads/avatar/";

    /** 允许的文件类型 */
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/jpg"};

    /** 最大文件大小：2MB */
    private static final long MAX_SIZE = 2 * 1024 * 1024;

    /**
     * 上传头像
     */
    @PostMapping("/upload")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();

        // 校验文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        // 校验文件大小
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("文件大小不能超过2MB");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (!isAllowedType(contentType)) {
            throw new BusinessException("仅支持jpg/png格式图片");
        }

        // 魔数校验：Content-Type 可被伪造，需验证文件内容的真实类型
        try {
            byte[] magicBytes = new byte[4];
            int read = file.getInputStream().read(magicBytes, 0, 4);
            if (read < 4) {
                throw new BusinessException("文件内容不完整");
            }
            // JPEG: FF D8 FF；PNG: 89 50 4E 47
            boolean isJpeg = (magicBytes[0] & 0xFF) == 0xFF
                    && (magicBytes[1] & 0xFF) == 0xD8
                    && (magicBytes[2] & 0xFF) == 0xFF;
            boolean isPng = (magicBytes[0] & 0xFF) == 0x89
                    && magicBytes[1] == 0x50
                    && magicBytes[2] == 0x4E
                    && magicBytes[3] == 0x47;
            if (!isJpeg && !isPng) {
                throw new BusinessException("仅支持jpg/png格式图片");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("文件读取失败: {}", e.getMessage());
            throw new BusinessException("文件读取失败");
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

        // 保存文件（解析为绝对路径：相对路径会被 Servlet 容器解析到 multipart 临时目录，
        // 导致文件落到 temp、/uploads/** 映射无法访问——旧版即存在此问题）
        File uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().toFile();
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        File dest = new File(uploadDir, newFilename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("文件上传失败: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException("文件上传失败，请重试");
        }

        // 返回文件访问URL
        String url = "/uploads/avatar/" + newFilename;
        log.info("头像上传成功: userId={}, url={}", userId, url);
        return R.ok(url);
    }

    /**
     * 校验文件类型
     */
    private boolean isAllowedType(String contentType) {
        if (contentType == null) return false;
        for (String allowed : ALLOWED_TYPES) {
            if (allowed.equalsIgnoreCase(contentType)) return true;
        }
        return false;
    }
}
