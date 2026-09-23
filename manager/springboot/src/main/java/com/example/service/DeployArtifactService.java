package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.entity.DeployArtifact;
import com.example.exception.BusinessException;
import com.example.mapper.DeployArtifactMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

/**
 * 制品文件仍由 /files/upload 保存，这里只登记版本和 MD5。
 */
@Service
public class DeployArtifactService {

    @Value("${file.upload-dir:files}")
    private String uploadDir;

    @Resource
    private DeployArtifactMapper deployArtifactMapper;

    public void register(DeployArtifact body, String uploader) {
        if (body == null || body.getAppId() == null || StrUtil.isBlank(body.getVersion()) || StrUtil.isBlank(body.getFilePath())) {
            throw new BusinessException("400", "请选择应用、版本并上传制品");
        }
        String stored = body.getFilePath();
        int slash = Math.max(stored.lastIndexOf('/'), stored.lastIndexOf('\\'));
        if (slash >= 0) {
            stored = stored.substring(slash + 1);
        }
        if (stored.contains("..") || stored.indexOf('/') >= 0 || stored.indexOf('\\') >= 0) {
            throw new BusinessException("400", "制品文件名不合法");
        }
        Path root = Paths.get(System.getProperty("user.dir", "."), uploadDir).toAbsolutePath().normalize();
        Path file = root.resolve(stored).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new BusinessException("400", "制品文件不存在，请重新上传");
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            DeployArtifact row = new DeployArtifact();
            row.setAppId(body.getAppId());
            row.setVersion(body.getVersion());
            row.setFileName(StrUtil.isBlank(body.getFileName()) ? stored : body.getFileName());
            row.setFilePath(file.toString());
            row.setFileSize((long) bytes.length);
            row.setMd5(md5(bytes));
            row.setUploader(uploader);
            deployArtifactMapper.insert(row);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("400", "读取制品失败");
        }
    }

    private String md5(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte value : hash) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
