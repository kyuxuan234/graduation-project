package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.Constants;
import com.example.common.enums.AccountStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Person;
import com.example.exception.BusinessException;
import com.example.mapper.PersonMapper;
import com.example.utils.PasswordUtils;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 人员账号：管理员、开发、运维同一张表。
 */
@Service
public class PersonService {

    @Resource
    private PersonMapper personMapper;

    public void add(Person person) {
        requireSupportedRole(person.getRole());
        Person dbPerson = personMapper.selectByUsername(person.getUsername());
        if (ObjectUtil.isNotNull(dbPerson)) {
            throw new BusinessException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(person.getPassword())) {
            person.setPassword(Constants.USER_DEFAULT_PASSWORD);
        }
        person.setPassword(PasswordUtils.encrypt(person.getPassword()));
        if (ObjectUtil.isEmpty(person.getName())) {
            person.setName(person.getUsername());
        }
        if (ObjectUtil.isEmpty(person.getStatus())) {
            person.setStatus(AccountStatusEnum.ACTIVE.name());
        }
        personMapper.insert(person);
    }

    public void deleteById(Integer id) {
        Person dbPerson = personMapper.selectById(id);
        if (ObjectUtil.isNull(dbPerson)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (AccountStatusEnum.DISABLED.name().equals(dbPerson.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        Account current = TokenUtils.getCurrentUser();
        if (current.getId() != null && current.getId().equals(id)) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        if (isSoleActiveAdmin(dbPerson)) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        personMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 管理员可改任意人员（含角色、状态、重置密码）。其他人只能改自己的姓名、电话、邮箱、头像。
     */
    public void updateById(Person person) {
        if (person.getId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        Person dbPerson = personMapper.selectById(person.getId());
        if (ObjectUtil.isNull(dbPerson)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        Account current = TokenUtils.getCurrentUser();
        boolean admin = RoleEnum.ADMIN.name().equals(current.getRole());
        if (!admin) {
            if (current.getId() == null || !current.getId().equals(dbPerson.getId())) {
                throw new BusinessException(ResultCodeEnum.NO_PERMISSION_ERROR);
            }
            person.setRole(null);
            person.setStatus(null);
            person.setPassword(null);
        } else {
            if (isSoleActiveAdmin(dbPerson) && leavesAdminSeat(person)) {
                throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
            }
            if (AccountStatusEnum.DISABLED.name().equals(dbPerson.getStatus())
                    && ObjectUtil.isNotEmpty(person.getPassword())
                    && !AccountStatusEnum.ACTIVE.name().equals(person.getStatus())) {
                throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
            }
            if (ObjectUtil.isNotEmpty(person.getRole())) {
                requireSupportedRole(person.getRole());
            }
            if (ObjectUtil.isNotEmpty(person.getPassword())) {
                person.setPassword(PasswordUtils.encrypt(person.getPassword()));
            } else {
                person.setPassword(null);
            }
        }
        person.setUsername(null);
        personMapper.updateById(person);
    }

    public Person selectById(Integer id) {
        return personMapper.selectById(id);
    }

    public Person selectByUsername(String username) {
        return personMapper.selectByUsername(username);
    }

    public List<Person> selectAll(Person person) {
        List<Person> list = personMapper.selectAll(person);
        for (Person item : list) {
            item.setPassword(null);
        }
        return list;
    }

    public PageInfo<Person> selectPage(Person person, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Person> list = selectAll(person);
        return PageInfo.of(list);
    }

    public Account login(Account account) {
        Person dbPerson = personMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbPerson)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (ObjectUtil.isNotEmpty(account.getRole()) && !account.getRole().equals(dbPerson.getRole())) {
            throw new BusinessException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbPerson.getPassword())) {
            throw new BusinessException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        if (AccountStatusEnum.DISABLED.name().equals(dbPerson.getStatus())) {
            throw new BusinessException(ResultCodeEnum.USER_DISABLED_ERROR);
        }
        String tokenData = dbPerson.getId() + "-" + dbPerson.getRole();
        String token = TokenUtils.createToken(tokenData, dbPerson.getPassword());
        dbPerson.setToken(token);
        dbPerson.setPassword(null);
        return dbPerson;
    }

    /**
     * 注册只开放开发和运维。超级管理员由人员管理页创建。
     */
    public void register(Account account) {
        if (RoleEnum.ADMIN.name().equals(account.getRole())
                || (!RoleEnum.DEV.name().equals(account.getRole()) && !RoleEnum.OPS.name().equals(account.getRole()))) {
            throw new BusinessException(ResultCodeEnum.ROLE_NOT_SUPPORT_ERROR);
        }
        Person person = new Person();
        BeanUtils.copyProperties(account, person);
        add(person);
    }

    public void updatePassword(Account account) {
        Person dbPerson = personMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbPerson)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbPerson.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        Person update = new Person();
        update.setId(dbPerson.getId());
        update.setPassword(PasswordUtils.encrypt(account.getNewPassword()));
        personMapper.updateById(update);
    }

    private boolean isSoleActiveAdmin(Person person) {
        return RoleEnum.ADMIN.name().equals(person.getRole())
                && !AccountStatusEnum.DISABLED.name().equals(person.getStatus())
                && personMapper.countActiveAdmins() <= 1;
    }

    private boolean leavesAdminSeat(Person incoming) {
        boolean roleChanged = ObjectUtil.isNotEmpty(incoming.getRole())
                && !RoleEnum.ADMIN.name().equals(incoming.getRole());
        boolean disabling = AccountStatusEnum.DISABLED.name().equals(incoming.getStatus());
        return roleChanged || disabling;
    }

    private void requireSupportedRole(String role) {
        if (StrUtil.isBlank(role)) {
            throw new BusinessException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (!RoleEnum.ADMIN.name().equals(role)
                && !RoleEnum.DEV.name().equals(role)
                && !RoleEnum.OPS.name().equals(role)
                && !RoleEnum.TL.name().equals(role)
                && !RoleEnum.QA.name().equals(role)
                && !RoleEnum.PM.name().equals(role)) {
            throw new BusinessException(ResultCodeEnum.ROLE_NOT_SUPPORT_ERROR);
        }
    }
}
