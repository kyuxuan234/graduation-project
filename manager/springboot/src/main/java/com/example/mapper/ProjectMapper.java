package com.example.mapper;

import com.example.entity.Project;

import java.util.List;

/**
 * 操作project相关数据接口
*/
public interface ProjectMapper {

    /**
      * 新增
    */
    int insert(Project project);

    /**
      * 删除（逻辑删除）
    */
    int deleteById(Integer id);

    /**
      * 修改
    */
    int updateById(Project project);

    /**
      * 根据ID查询
    */
    Project selectById(Integer id);

    /**
      * 查询所有
    */
    List<Project> selectAll(Project project);

    /**
      * 根据项目编号查询
    */
    Project selectByCode(String code);

}
