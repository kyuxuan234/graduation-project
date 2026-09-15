package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Admin;
import com.example.exception.BusinessException;
import com.example.mapper.AdminMapper;
import com.example.utils.PasswordUtils;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理员业务处理
 **/
@Service
public class AdminService {

    @Resource
    private AdminMapper adminMapper;

    /**
     * 新增
     */
    public void add(Admin admin) {
        Admin dbAdmin = adminMapper.selectByUsername(admin.getUsername());
        if (ObjectUtil.isNotNull(dbAdmin)) {
            throw new BusinessException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(admin.getPassword())) {
            admin.setPassword(Constants.USER_DEFAULT_PASSWORD);
        }
        admin.setPassword(PasswordUtils.encrypt(admin.getPassword()));
        if (ObjectUtil.isEmpty(admin.getName())) {
            admin.setName(admin.getUsername());
        }
        admin.setRole(RoleEnum.ADMIN.name());
        adminMapper.insert(admin);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        adminMapper.deleteById(id);
    }

    /**
     * 批量删除
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            adminMapper.deleteById(id);
        }
    }

    /**
     * 修改
     */
    public void updateById(Admin admin) {
        // 密码只能通过 updatePassword 修改，避免通用修改接口把明文写入数据库
        admin.setPassword(null);
        adminMapper.updateById(admin);
    }

    /**
     * 根据ID查询
     */
    public Admin selectById(Integer id) {
        return adminMapper.selectById(id);
    }

    /**
     * 根据用户名查询
     */
    public Admin selectByUsername(String username) {
        return adminMapper.selectByUsername(username);
    }

    /**
     * 查询所有
     */
    public List<Admin> selectAll(Admin admin) {
        List<Admin> list = adminMapper.selectAll(admin);
        // 列表接口不返回密码密文
        for (Admin item : list) {
            item.setPassword(null);
        }
        return list;
    }

    /**
     * 分页查询
     */
    public PageInfo<Admin> selectPage(Admin admin, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        // 复用 selectAll 以复用密码脱敏，返回的仍是 PageHelper 的 Page 实例
        List<Admin> list = selectAll(admin);
        return PageInfo.of(list);
    }

    /**
     * 登录
     */
    public Account login(Account account) {
        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbAdmin)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbAdmin.getPassword())) {
            throw new BusinessException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        // 生成token
        String tokenData = dbAdmin.getId() + "-" + RoleEnum.ADMIN.name();
        String token = TokenUtils.createToken(tokenData, dbAdmin.getPassword());
        dbAdmin.setToken(token);
        // 密码密文不返回给前端
        dbAdmin.setPassword(null);
        return dbAdmin;
    }

    /**
     * 注册
     */
    public void register(Account account) {
        Admin admin = new Admin();
        BeanUtils.copyProperties(account, admin);
        add(admin);
    }

    /**
     * 修改密码
     */
    public void updatePassword(Account account) {
        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbAdmin)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbAdmin.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        Admin updateAdmin = new Admin();
        updateAdmin.setId(dbAdmin.getId());
        updateAdmin.setPassword(PasswordUtils.encrypt(account.getNewPassword()));
        adminMapper.updateById(updateAdmin);
    }

}
