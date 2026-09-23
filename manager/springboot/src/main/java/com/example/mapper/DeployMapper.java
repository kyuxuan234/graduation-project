package com.example.mapper;

import com.example.entity.Deploy;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作deploy相关数据接口
*/
public interface DeployMapper {

    /**
      * 新增
    */
    int insert(Deploy deploy);

    /**
      * 删除（逻辑删除）
    */
    int deleteById(Integer id);

    /**
      * 修改
    */
    int updateById(Deploy deploy);

    /**
      * 根据ID查询
    */
    Deploy selectById(Integer id);

    /**
      * 查询所有
    */
    List<Deploy> selectAll(Deploy deploy);

    /**
      * 统计某应用下未删除的部署单数量
    */
    int countByAppId(Integer appId);

    int casUpdateStatus(@Param("id") Integer id,
                        @Param("expectStatus") String expectStatus,
                        @Param("rowVersion") Integer rowVersion,
                        @Param("status") String status,
                        @Param("finishTime") String finishTime,
                        @Param("deployType") String deployType);

    int countOtherDeploying(@Param("appId") Integer appId, @Param("excludeId") Integer excludeId);

    String selectLatestSuccessVersion(@Param("appId") Integer appId, @Param("excludeId") Integer excludeId);

    List<Deploy> selectRecentForServer(@Param("serverId") Integer serverId, @Param("since") String since);

}
