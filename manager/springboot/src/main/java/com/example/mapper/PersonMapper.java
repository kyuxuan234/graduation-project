package com.example.mapper;

import com.example.entity.Person;
import com.example.entity.PersonMailAddress;

import java.util.List;

/**
 * 人员表
 */
public interface PersonMapper {

    int insert(Person person);

    int deleteById(Integer id);

    int updateById(Person person);

    Person selectById(Integer id);

    List<Person> selectAll(Person person);

    Person selectByUsername(String username);

    int countActiveAdmins();

    /**
     * 告警通知：未删除、未禁用、且填写了邮箱的人员
     */
    List<PersonMailAddress> selectNotifyMails();
}
