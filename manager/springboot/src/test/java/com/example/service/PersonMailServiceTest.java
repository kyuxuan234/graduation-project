package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Person;
import com.example.entity.PersonMailAddress;
import com.example.mapper.PersonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonMailServiceTest {

    @Mock
    private PersonMapper personMapper;

    @InjectMocks
    private PersonMailService personMailService;

    @Test
    void emailOf_readsPersonTableByUsernameAndRole() {
        Account account = new Account();
        account.setUsername("admin");
        account.setRole(RoleEnum.ADMIN.name());
        Person admin = new Person();
        admin.setRole(RoleEnum.ADMIN.name());
        admin.setEmail("  admin@devops.com ");
        when(personMapper.selectByUsername("admin")).thenReturn(admin);

        assertEquals("admin@devops.com", personMailService.emailOf(account));
    }

    @Test
    void emailOf_rejectsRoleThatDoesNotMatchTheRow() {
        Account account = new Account();
        account.setUsername("ops1");
        account.setRole(RoleEnum.DEV.name());
        Person ops = new Person();
        ops.setRole(RoleEnum.OPS.name());
        ops.setEmail("ops1@example.com");
        when(personMapper.selectByUsername("ops1")).thenReturn(ops);

        assertNull(personMailService.emailOf(account));
    }

    @Test
    void notifyRecipients_keepsNameAndIncludesSameEmailForOtherPerson() {
        Account sender = new Account();
        sender.setUsername("ops1");
        sender.setRole(RoleEnum.OPS.name());
        when(personMapper.selectNotifyMails()).thenReturn(Arrays.asList(
                new PersonMailAddress("admin", "超级管理员", "admin@example.com", RoleEnum.ADMIN.name()),
                new PersonMailAddress("ops1", "王强", "same@qq.com", RoleEnum.OPS.name()),
                new PersonMailAddress("ops2", "赵敏", "ops2@example.com", RoleEnum.OPS.name()),
                new PersonMailAddress("dev1", "张伟", "same@qq.com", RoleEnum.DEV.name())));

        List<PersonMailAddress> people = personMailService.notifyRecipients(sender);
        List<String> displays = people.stream().map(PersonMailAddress::display).collect(Collectors.toList());
        assertEquals(Arrays.asList(
                "超级管理员 <admin@example.com>",
                "赵敏 <ops2@example.com>",
                "张伟 <same@qq.com>"), displays);
    }

    @Test
    void notifyRecipients_keepsSameNameWhenAccountDiffers() {
        Account sender = new Account();
        sender.setUsername("dev1");
        sender.setRole(RoleEnum.DEV.name());
        when(personMapper.selectNotifyMails()).thenReturn(Arrays.asList(
                new PersonMailAddress("ops1", "张伟", "ops-zhang@example.com", RoleEnum.OPS.name()),
                new PersonMailAddress("dev1", "张伟", "dev1@example.com", RoleEnum.DEV.name()),
                new PersonMailAddress("dev3", "张伟", "dev3@example.com", RoleEnum.DEV.name())));

        List<PersonMailAddress> people = personMailService.notifyRecipients(sender);
        List<String> displays = people.stream().map(PersonMailAddress::display).collect(Collectors.toList());
        assertEquals(Arrays.asList(
                "张伟 <ops-zhang@example.com>",
                "张伟 <dev3@example.com>"), displays);
    }

    @Test
    void notifyRecipients_dedupesSameAccountRoleAndEmail() {
        Account sender = new Account();
        sender.setUsername("ops1");
        sender.setRole(RoleEnum.OPS.name());
        when(personMapper.selectNotifyMails()).thenReturn(Arrays.asList(
                new PersonMailAddress("dev1", "张伟", "  Dev1@example.com ", RoleEnum.DEV.name()),
                new PersonMailAddress("dev1", "张伟", "dev1@example.com", RoleEnum.DEV.name())));

        List<PersonMailAddress> people = personMailService.notifyRecipients(sender);
        assertEquals(1, people.size());
        assertEquals("张伟 <Dev1@example.com>", people.get(0).display());
    }

    @Test
    void recipientKey_usesAccountRoleEmailNotName() {
        assertEquals(
                PersonMailService.recipientKey("dev1", "DEV", "a@x.com"),
                PersonMailService.recipientKey("dev1", "DEV", "A@x.com"));
        assertFalse(PersonMailService.recipientKey("dev1", "DEV", "a@x.com")
                .equals(PersonMailService.recipientKey("dev3", "DEV", "a@x.com")));
        assertFalse(PersonMailService.recipientKey("dev1", "DEV", "a@x.com")
                .equals(PersonMailService.recipientKey("dev1", "OPS", "a@x.com")));
    }

    @Test
    void notifyRecipients_emptyWhenNobodyElseConfigured() {
        Account sender = new Account();
        sender.setUsername("ops1");
        sender.setRole(RoleEnum.OPS.name());
        when(personMapper.selectNotifyMails()).thenReturn(Collections.emptyList());
        assertTrue(personMailService.notifyRecipients(sender).isEmpty());
    }
}
