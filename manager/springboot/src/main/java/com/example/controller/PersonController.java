package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.RoleEnum;
import com.example.entity.Person;
import com.example.service.PersonService;
import com.example.utils.AuthUtils;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 人员管理。列表和增删只给管理员；修改在服务层区分管理员与本人。
 */
@RestController
@RequestMapping("/person")
public class PersonController {

    @Resource
    private PersonService personService;

    @PostMapping("/add")
    public Result<Void> add(@RequestBody Person person) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        personService.add(person);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        personService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        personService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> updateById(@RequestBody Person person) {
        personService.updateById(person);
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result<Person> selectById(@PathVariable Integer id) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        Person person = personService.selectById(id);
        if (person != null) {
            person.setPassword(null);
        }
        return Result.success(person);
    }

    @GetMapping("/developers")
    public Result<List<Person>> developers() {
        AuthUtils.requireRole(RoleEnum.DEV, RoleEnum.TL);
        return Result.success(personService.listActiveDevelopers());
    }

    @GetMapping("/selectAll")
    public Result<List<Person>> selectAll(Person person) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        return Result.success(personService.selectAll(person));
    }

    @GetMapping("/selectPage")
    public Result<PageInfo<Person>> selectPage(Person person,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        return Result.success(personService.selectPage(person, pageNum, pageSize));
    }
}
