package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.entity.Account;
import com.example.entity.Person;
import com.example.entity.PersonMailAddress;
import com.example.mapper.PersonMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 正式告警邮箱只查 person.email，不读 SMTP 配置，不用内存里的登录对象。
 */
@Service
public class PersonMailService {

    @Resource
    private PersonMapper personMapper;

    /**
     * 按用户名回表取邮箱。请求里的角色必须和人员表一致。
     */
    public String emailOf(Account account) {
        Person person = find(account);
        return person == null ? null : normalize(person.getEmail());
    }

    public String displayName(Account account) {
        if (account == null || StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getRole())) {
            return "系统用户";
        }
        Person person = find(account);
        if (person != null && StrUtil.isNotBlank(person.getName())) {
            return person.getName().trim();
        }
        return StrUtil.blankToDefault(account.getUsername(), "系统用户");
    }

    /**
     * 发件人按账号+角色排除；收件人按账号+角色+邮箱去重。
     * 显示名只用姓名。重名、共用邮箱只要账号或角色不同，都保留。
     */
    public List<PersonMailAddress> notifyRecipients(Account sender) {
        List<PersonMailAddress> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        addAll(result, seen, sender, personMapper.selectNotifyMails());
        return result;
    }

    private Person find(Account account) {
        if (account == null || StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getRole())) {
            return null;
        }
        Person person = personMapper.selectByUsername(account.getUsername());
        if (person == null || !account.getRole().equals(person.getRole())) {
            return null;
        }
        return person;
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
                               List<PersonMailAddress> people) {
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
            String role = StrUtil.trim(item.getRole());
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
