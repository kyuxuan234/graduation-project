package com.example.controller;

import com.example.common.Result;
import com.example.entity.Account;
import com.example.entity.DeployArtifact;
import com.example.service.DeployArtifactService;
import com.example.utils.TokenUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 登记已上传的制品。文件本体走 /files/upload。
 */
@RestController
@RequestMapping("/deployArtifact")
public class DeployArtifactController {

    @Resource
    private DeployArtifactService deployArtifactService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody DeployArtifact artifact) {
        Account user = TokenUtils.getCurrentUser();
        String uploader = user == null ? "" : user.getUsername();
        deployArtifactService.register(artifact, uploader);
        return Result.success();
    }
}
