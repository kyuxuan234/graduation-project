package com.example.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.exception.BusinessException;
import com.example.utils.AuthUtils;
import com.example.utils.FileSafeNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件接口
 */
@RestController
@RequestMapping("/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Value("${file.upload-dir:files}")
    private String uploadDir;

    @Value("${server.port:9090}")
    private String port;

    @Value("${ip:localhost}")
    private String ip;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        AuthUtils.requireLogin();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR);
        }
        if (file.getSize() > FileSafeNames.MAX_BYTES) {
            throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR.code, "文件不能超过 10MB");
        }
        String ext = FileSafeNames.resolveAllowedExt(file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR);
        }
        FileSafeNames.assertMagic(ext, bytes);
        String stored = FileSafeNames.newStoredName(ext);
        Path root = uploadRoot();
        if (!FileUtil.isDirectory(root.toString())) {
            FileUtil.mkdir(root.toString());
        }
        FileUtil.writeBytes(bytes, FileSafeNames.resolveInside(root, stored).toString());
        log.info("文件上传成功 {}", stored);
        String http = "http://" + ip + ":" + port + "/files/";
        return Result.success(http + stored);
    }

    /**
     * 获取文件
     */
    @GetMapping("/{flag:.+}")
    public void avatarPath(@PathVariable String flag, HttpServletResponse response) {
        OutputStream os;
        try {
            if (StrUtil.isNotEmpty(flag)) {
                response.addHeader("Content-Disposition", "inline;filename=" + URLEncoder.encode(flag, "UTF-8"));
                response.setContentType(contentType(flag));
                byte[] bytes = FileUtil.readBytes(fileInRoot(flag).toString());
                os = response.getOutputStream();
                os.write(bytes);
                os.flush();
                os.close();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("文件下载失败");
        }
    }

    /**
     * 删除文件。只有超级管理员可以删。
     */
    @DeleteMapping("/{flag:.+}")
    public void delFile(@PathVariable String flag) {
        AuthUtils.requireRole(ResultCodeEnum.NO_PERMISSION_ERROR, RoleEnum.ADMIN);
        FileUtil.del(fileInRoot(flag).toString());
        log.info("删除文件 {} 成功", flag);
    }

    private Path uploadRoot() {
        return Paths.get(System.getProperty("user.dir", "."), uploadDir);
    }

    private Path fileInRoot(String flag) {
        if (StrUtil.isBlank(flag) || flag.indexOf('\0') >= 0 || flag.contains("..") || flag.contains("/") || flag.contains("\\")) {
            throw new BusinessException(ResultCodeEnum.FILE_NAME_INVALID_ERROR);
        }
        return uploadRoot().resolve(flag);
    }

    private static String contentType(String flag) {
        String lower = flag.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
