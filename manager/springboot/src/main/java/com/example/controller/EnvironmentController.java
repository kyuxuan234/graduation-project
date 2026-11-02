package com.example.controller;

import com.example.common.Result;
import com.example.entity.Environment;
import com.example.service.EnvironmentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/environment")
public class EnvironmentController {

    @Resource
    private EnvironmentService environmentService;

    @GetMapping("/selectAll")
    public Result<List<Environment>> selectAll() {
        return Result.success(environmentService.selectAll());
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Environment environment) {
        environmentService.update(environment);
        return Result.success();
    }
}
