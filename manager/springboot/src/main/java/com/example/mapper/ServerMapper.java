package com.example.mapper;

import com.example.entity.Server;

import java.util.List;

/**
 * 操作server相关数据接口
*/
public interface ServerMapper {

    /**
      * 新增
    */
    int insert(Server server);

    /**
      * 删除（逻辑删除）
    */
    int deleteById(Integer id);

    /**
      * 修改
    */
    int updateById(Server server);

    /**
      * 根据ID查询
    */
    Server selectById(Integer id);

    /**
      * 查询所有
    */
    List<Server> selectAll(Server server);

    /**
      * 查询所有未停机的服务器，给下拉框用
    */
    List<Server> selectNotStopped();

    /**
      * 根据主机名查询
    */
    Server selectByHostname(String hostname);

    /**
      * 根据IP查询
    */
    Server selectByIp(String ip);

}
