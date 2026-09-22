package com.example.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.exception.BusinessException;
import com.example.utils.AuthUtils;
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
        String flag;
        synchronized (FileController.class) {
            flag = System.currentTimeMillis() + "";
            ThreadUtil.sleep(1L);
        }
        String fileName = safeOriginalName(file.getOriginalFilename());
        try {
            Path root = uploadRoot();
            if (!FileUtil.isDirectory(root.toString())) {
                FileUtil.mkdir(root.toString());
            }
            FileUtil.writeBytes(file.getBytes(), root.resolve(flag + "-" + fileName).toString());
            System.out.println(fileName + "--上传成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            System.err.println(fileName + "--文件上传失败");
        }
        String http = "http://" + ip + ":" + port + "/files/";
        return Result.success(http + flag + "-" + fileName);
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
            System.out.println("文件下载失败");
        }
    }

    /**
     * 删除文件。只有超级管理员可以删。
     */
    @DeleteMapping("/{flag:.+}")
    public void delFile(@PathVariable String flag) {
        AuthUtils.requireRole(ResultCodeEnum.NO_PERMISSION_ERROR, RoleEnum.ADMIN);
        FileUtil.del(fileInRoot(flag).toString());
        System.out.println("删除文件" + flag + "成功");
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

    private static String safeOriginalName(String original) {
        String fileName = StrUtil.blankToDefault(original, "file");
        fileName = Paths.get(fileName).getFileName().toString();
        if (fileName.indexOf('\0') >= 0 || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new BusinessException(ResultCodeEnum.FILE_NAME_INVALID_ERROR);
        }
        return fileName;
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
