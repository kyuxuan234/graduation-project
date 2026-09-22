package com.example.mapper;

import com.example.entity.Dev;
import com.example.entity.PersonMailAddress;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作dev相关数据接口
*/
public interface DevMapper {

    /**
      * 新增
    */
    int insert(Dev dev);

    /**
      * 删除（逻辑删除）
    */
    int deleteById(Integer id);

    /**
      * 修改
    */
    int updateById(Dev dev);

    /**
      * 根据ID查询
    */
    Dev selectById(Integer id);

    /**
      * 查询所有
    */
    List<Dev> selectAll(Dev dev);

    @Select("select * from dev where username = #{username} and deleted = 0")
    Dev selectByUsername(String username);

    /**
     * 告警通知：未删除、未禁用、且填写了邮箱的开发
     */
    List<PersonMailAddress> selectNotifyMails();
}
