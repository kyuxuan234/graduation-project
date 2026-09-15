package com.example.mapper;

import com.example.entity.App;

import java.util.List;

/**
 * 操作app相关数据接口
*/
public interface AppMapper {

    /**
      * 新增
    */
    int insert(App app);

    /**
      * 删除（逻辑删除）
    */
    int deleteById(Integer id);

    /**
      * 修改
    */
    int updateById(App app);

    /**
      * 根据ID查询
    */
    App selectById(Integer id);

    /**
      * 查询所有
    */
    List<App> selectAll(App app);

    /**
      * 统计某项目下未删除的应用数量
    */
    int countByProjectId(Integer projectId);

    /**
      * 统计某服务器下未删除的应用数量
    */
    int countByServerId(Integer serverId);

    /**
      * 根据编号查询
    */
    App selectByCode(String code);

}
