package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Admin;
import com.example.entity.Dev;
import com.example.entity.Ops;
import com.example.entity.PersonMailAddress;
import com.example.mapper.AdminMapper;
import com.example.mapper.DevMapper;
import com.example.mapper.OpsMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 正式告警邮箱只查 admin / ops / dev 表的 email 字段，不读 SMTP 配置，不用内存里的登录对象。
 */
@Service
public class PersonMailService {

    @Resource
    private AdminMapper adminMapper;
    @Resource
    private OpsMapper opsMapper;
    @Resource
    private DevMapper devMapper;

    /**
     * 按角色回表取人员邮箱：admin.email / ops.email / dev.email。
     */
    public String emailOf(Account account) {
        if (account == null || StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getRole())) {
            return null;
        }
        String username = account.getUsername();
        String role = account.getRole();
        if (RoleEnum.ADMIN.name().equals(role)) {
            Admin admin = adminMapper.selectByUsername(username);
            return admin == null ? null : normalize(admin.getEmail());
        }
        if (RoleEnum.OPS.name().equals(role)) {
            Ops ops = opsMapper.selectByUsername(username);
            return ops == null ? null : normalize(ops.getEmail());
        }
        if (RoleEnum.DEV.name().equals(role)) {
            Dev dev = devMapper.selectByUsername(username);
            return dev == null ? null : normalize(dev.getEmail());
        }
        return null;
    }

    public String displayName(Account account) {
        if (account == null || StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getRole())) {
            return "系统用户";
        }
        String username = account.getUsername();
        String role = account.getRole();
        String name = null;
        if (RoleEnum.ADMIN.name().equals(role)) {
            Admin admin = adminMapper.selectByUsername(username);
            name = admin == null ? null : admin.getName();
        } else if (RoleEnum.OPS.name().equals(role)) {
            Ops ops = opsMapper.selectByUsername(username);
            name = ops == null ? null : ops.getName();
        } else if (RoleEnum.DEV.name().equals(role)) {
            Dev dev = devMapper.selectByUsername(username);
            name = dev == null ? null : dev.getName();
        }
        if (StrUtil.isNotBlank(name)) {
            return name.trim();
        }
        return StrUtil.blankToDefault(username, "系统用户");
    }

    /**
     * 发件人按账号+角色排除；收件人按账号+角色+邮箱去重。
     * 显示名只用姓名。重名、共用邮箱只要账号或角色不同，都保留。
     */
    public List<PersonMailAddress> notifyRecipients(Account sender) {
        List<PersonMailAddress> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        addAll(result, seen, sender, RoleEnum.ADMIN.name(), adminMapper.selectNotifyMails());
        addAll(result, seen, sender, RoleEnum.OPS.name(), opsMapper.selectNotifyMails());
        addAll(result, seen, sender, RoleEnum.DEV.name(), devMapper.selectNotifyMails());
        return result;
    }

    static String normalize(String email) {
        if (StrUtil.isBlank(email)) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.contains("@") ? trimmed : null;
    }

    public static boolean sameAddress(String left, String right) {
        String a = normalizeKey(left);
        String b = normalizeKey(right);
        return a != null && a.equals(b);
    }

    private static String normalizeKey(String email) {
        String normalized = normalize(email);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private static void addAll(List<PersonMailAddress> result, Set<String> seen, Account sender,
                               String role, List<PersonMailAddress> people) {
        if (people == null) {
            return;
        }
        String skipUser = sender == null ? null : sender.getUsername();
        String skipRole = sender == null ? null : sender.getRole();
        for (PersonMailAddress item : people) {
            if (item == null) {
                continue;
            }
            String email = normalize(item.getEmail());
            if (email == null) {
                continue;
            }
            String username = StrUtil.trim(item.getUsername());
            if (sameAccount(skipUser, skipRole, username, role)) {
                continue;
            }
            if (!seen.add(recipientKey(username, role, email))) {
                continue;
            }
            result.add(new PersonMailAddress(username, StrUtil.trim(item.getName()), email, role));
        }
    }

    static boolean sameAccount(String leftUser, String leftRole, String rightUser, String rightRole) {
        return StrUtil.isNotBlank(leftUser)
                && StrUtil.isNotBlank(leftRole)
                && leftUser.equals(rightUser)
                && leftRole.equals(rightRole);
    }

    static String recipientKey(String username, String role, String email) {
        String user = StrUtil.nullToEmpty(username);
        String personRole = StrUtil.nullToEmpty(role);
        String box = StrUtil.nullToEmpty(normalizeKey(email));
        return user + "\0" + personRole + "\0" + box;
    }
}
