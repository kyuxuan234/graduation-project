package com.example.mapper;

import com.example.entity.Ops;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作ops相关数据接口
*/
public interface OpsMapper {

    /**
      * 新增
    */
    int insert(Ops ops);

    /**
      * 删除（逻辑删除）
    */
    int deleteById(Integer id);

    /**
      * 修改
    */
    int updateById(Ops ops);

    /**
      * 根据ID查询
    */
    Ops selectById(Integer id);

    /**
      * 查询所有
    */
    List<Ops> selectAll(Ops ops);

    @Select("select * from ops where username = #{username} and deleted = 0")
    Ops selectByUsername(String username);
}
