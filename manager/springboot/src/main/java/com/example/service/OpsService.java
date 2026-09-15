package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Constants;
import com.example.common.enums.AccountStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Ops;
import com.example.exception.BusinessException;
import com.example.mapper.OpsMapper;
import com.example.utils.PasswordUtils;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 运维人员业务处理
 **/
@Service
public class OpsService {

    @Resource
    private OpsMapper opsMapper;

    /**
     * 新增
     */
    public void add(Ops ops) {
        Ops dbOps = opsMapper.selectByUsername(ops.getUsername());
        if (ObjectUtil.isNotNull(dbOps)) {
            throw new BusinessException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(ops.getPassword())) {
            ops.setPassword(Constants.USER_DEFAULT_PASSWORD);
        }
        ops.setPassword(PasswordUtils.encrypt(ops.getPassword()));
        if (ObjectUtil.isEmpty(ops.getName())) {
            ops.setName(ops.getUsername());
        }
        if (ObjectUtil.isEmpty(ops.getStatus())) {
            ops.setStatus(AccountStatusEnum.ACTIVE.name());
        }
        opsMapper.insert(ops);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        Ops dbOps = opsMapper.selectById(id);
        if (ObjectUtil.isNull(dbOps)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (AccountStatusEnum.DISABLED.name().equals(dbOps.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        opsMapper.deleteById(id);
    }

    /**
     * 批量删除
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 修改
     */
    public void updateById(Ops ops) {
        Ops dbOps = opsMapper.selectById(ops.getId());
        if (ObjectUtil.isNull(dbOps)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (AccountStatusEnum.DISABLED.name().equals(dbOps.getStatus())
                && ObjectUtil.isNotEmpty(ops.getPassword())
                && !AccountStatusEnum.ACTIVE.name().equals(ops.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        // 传了密码视为管理员重置密码，入参是明文，这里统一加密后落库；不传则保持原密码不变
        if (ObjectUtil.isNotEmpty(ops.getPassword())) {
            ops.setPassword(PasswordUtils.encrypt(ops.getPassword()));
        } else {
            ops.setPassword(null);
        }
        opsMapper.updateById(ops);
    }

    /**
     * 根据ID查询
     */
    public Ops selectById(Integer id) {
        Ops ops = opsMapper.selectById(id);
        if (ObjectUtil.isNotNull(ops)) {
            ops.setRole(RoleEnum.OPS.name());
        }
        return ops;
    }

    /**
     * 根据用户名查询
     */
    public Ops selectByUsername(String username) {
        return opsMapper.selectByUsername(username);
    }

    /**
     * 查询所有
     */
    public List<Ops> selectAll(Ops ops) {
        List<Ops> list = opsMapper.selectAll(ops);
        // 列表接口不返回密码密文
        for (Ops item : list) {
            item.setPassword(null);
            item.setRole(RoleEnum.OPS.name());
        }
        return list;
    }

    /**
     * 分页查询
     */
    public PageInfo<Ops> selectPage(Ops ops, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        // 复用 selectAll 以复用密码脱敏，返回的仍是 PageHelper 的 Page 实例
        List<Ops> list = selectAll(ops);
        return PageInfo.of(list);
    }

    /**
     * 登录
     */
    public Account login(Account account) {
        Ops dbOps = opsMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbOps)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbOps.getPassword())) {
            throw new BusinessException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        if (!AccountStatusEnum.ACTIVE.name().equals(dbOps.getStatus())) {
            throw new BusinessException(ResultCodeEnum.USER_DISABLED_ERROR);
        }
        // 生成token
        String tokenData = dbOps.getId() + "-" + RoleEnum.OPS.name();
        String token = TokenUtils.createToken(tokenData, dbOps.getPassword());
        dbOps.setRole(RoleEnum.OPS.name());
        dbOps.setToken(token);
        // 密码密文不返回给前端
        dbOps.setPassword(null);
        return dbOps;
    }

    /**
     * 注册
     */
    public void register(Account account) {
        Ops ops = new Ops();
        BeanUtils.copyProperties(account, ops);
        add(ops);
    }

    /**
     * 修改密码
     */
    public void updatePassword(Account account) {
        Ops dbOps = opsMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbOps)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbOps.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        Ops updateOps = new Ops();
        updateOps.setId(dbOps.getId());
        updateOps.setPassword(PasswordUtils.encrypt(account.getNewPassword()));
        opsMapper.updateById(updateOps);
    }

}
