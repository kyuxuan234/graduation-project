package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Environment;
import com.example.exception.BusinessException;
import com.example.mapper.EnvironmentMapper;
import com.example.utils.AuthUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class EnvironmentService {

    @Resource
    private EnvironmentMapper environmentMapper;

    public List<Environment> selectAll() {
        AuthUtils.requireLogin();
        return environmentMapper.selectAll();
    }

    public void update(Environment incoming) {
        AuthUtils.requireRole(RoleEnum.OPS);
        if (incoming == null || incoming.getId() == null || StrUtil.isBlank(incoming.getName())) {
            throw new BusinessException(ResultCodeEnum.PARAM_LOST_ERROR.code, "请填写环境名称");
        }
        Environment db = null;
        for (Environment row : environmentMapper.selectAll()) {
            if (incoming.getId().equals(row.getId())) {
                db = row;
                break;
            }
        }
        if (db == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code, "环境不存在");
        }
        Environment patch = new Environment();
        patch.setId(db.getId());
        patch.setName(incoming.getName().trim());
        patch.setRemark(incoming.getRemark());
        environmentMapper.updateById(patch);
    }
}
