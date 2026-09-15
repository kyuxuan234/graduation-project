package com.example.mapper;

import com.example.entity.DeployLog;

import java.util.List;

/**
 * 操作deploy_log相关数据接口
*/
public interface DeployLogMapper {

    /**
      * 新增，仅由Deploy模块内部写入
    */
    int insert(DeployLog deployLog);

    /**
      * 根据ID查询
    */
    DeployLog selectById(Integer id);

    /**
      * 查询所有
    */
    List<DeployLog> selectAll(DeployLog deployLog);

}
