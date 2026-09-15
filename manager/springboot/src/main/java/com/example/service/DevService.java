package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Constants;
import com.example.common.enums.AccountStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Dev;
import com.example.exception.BusinessException;
import com.example.mapper.DevMapper;
import com.example.utils.PasswordUtils;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 开发人员业务处理
 **/
@Service
public class DevService {

    @Resource
    private DevMapper devMapper;

    /**
     * 新增
     */
    public void add(Dev dev) {
        Dev dbDev = devMapper.selectByUsername(dev.getUsername());
        if (ObjectUtil.isNotNull(dbDev)) {
            throw new BusinessException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(dev.getPassword())) {
            dev.setPassword(Constants.USER_DEFAULT_PASSWORD);
        }
        dev.setPassword(PasswordUtils.encrypt(dev.getPassword()));
        if (ObjectUtil.isEmpty(dev.getName())) {
            dev.setName(dev.getUsername());
        }
        if (ObjectUtil.isEmpty(dev.getStatus())) {
            dev.setStatus(AccountStatusEnum.ACTIVE.name());
        }
        devMapper.insert(dev);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        Dev dbDev = devMapper.selectById(id);
        if (ObjectUtil.isNull(dbDev)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (AccountStatusEnum.DISABLED.name().equals(dbDev.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        devMapper.deleteById(id);
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
    public void updateById(Dev dev) {
        Dev dbDev = devMapper.selectById(dev.getId());
        if (ObjectUtil.isNull(dbDev)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        // 禁用账号只允许改回启用，不允许直接重置密码
        if (AccountStatusEnum.DISABLED.name().equals(dbDev.getStatus())
                && ObjectUtil.isNotEmpty(dev.getPassword())
                && !AccountStatusEnum.ACTIVE.name().equals(dev.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        // 传了密码视为管理员重置密码，入参是明文，这里统一加密后落库；不传则保持原密码不变
        if (ObjectUtil.isNotEmpty(dev.getPassword())) {
            dev.setPassword(PasswordUtils.encrypt(dev.getPassword()));
        } else {
            dev.setPassword(null);
        }
        devMapper.updateById(dev);
    }

    /**
     * 根据ID查询
     */
    public Dev selectById(Integer id) {
        Dev dev = devMapper.selectById(id);
        if (ObjectUtil.isNotNull(dev)) {
            dev.setRole(RoleEnum.DEV.name());
        }
        return dev;
    }

    /**
     * 根据用户名查询
     */
    public Dev selectByUsername(String username) {
        return devMapper.selectByUsername(username);
    }

    /**
     * 查询所有
     */
    public List<Dev> selectAll(Dev dev) {
        List<Dev> list = devMapper.selectAll(dev);
        // 列表接口不返回密码密文
        for (Dev item : list) {
            item.setPassword(null);
            item.setRole(RoleEnum.DEV.name());
        }
        return list;
    }

    /**
     * 分页查询
     */
    public PageInfo<Dev> selectPage(Dev dev, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        // 复用 selectAll 以复用密码脱敏，返回的仍是 PageHelper 的 Page 实例
        List<Dev> list = selectAll(dev);
        return PageInfo.of(list);
    }

    /**
     * 登录
     */
    public Account login(Account account) {
        Dev dbDev = devMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbDev)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbDev.getPassword())) {
            throw new BusinessException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        if (!AccountStatusEnum.ACTIVE.name().equals(dbDev.getStatus())) {
            throw new BusinessException(ResultCodeEnum.USER_DISABLED_ERROR);
        }
        // 生成token
        String tokenData = dbDev.getId() + "-" + RoleEnum.DEV.name();
        String token = TokenUtils.createToken(tokenData, dbDev.getPassword());
        dbDev.setRole(RoleEnum.DEV.name());
        dbDev.setToken(token);
        // 密码密文不返回给前端
        dbDev.setPassword(null);
        return dbDev;
    }

    /**
     * 注册
     */
    public void register(Account account) {
        Dev dev = new Dev();
        BeanUtils.copyProperties(account, dev);
        add(dev);
    }

    /**
     * 修改密码
     */
    public void updatePassword(Account account) {
        Dev dbDev = devMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbDev)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbDev.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        Dev updateDev = new Dev();
        updateDev.setId(dbDev.getId());
        updateDev.setPassword(PasswordUtils.encrypt(account.getNewPassword()));
        devMapper.updateById(updateDev);
    }

}
