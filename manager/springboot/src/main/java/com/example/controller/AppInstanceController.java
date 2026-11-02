package com.example.controller;

import com.example.common.Result;
import com.example.entity.AppInstance;
import com.example.service.AppInstanceService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/appInstance")
public class AppInstanceController {

    @Resource
    private AppInstanceService appInstanceService;

    @GetMapping("/byApp/{appId}")
    public Result<List<AppInstance>> byApp(@PathVariable Integer appId) {
        return Result.success(appInstanceService.listByApp(appId));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody AppInstance row) {
        appInstanceService.add(row);
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody AppInstance row) {
        appInstanceService.update(row);
        return Result.success();
    }
}
