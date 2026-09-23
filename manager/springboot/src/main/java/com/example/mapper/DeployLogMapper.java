package com.example.mapper;

import com.example.entity.DeployLog;
import org.apache.ibatis.annotations.Param;

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

    int insertBatch(@Param("list") List<DeployLog> list);

    Integer maxExecSeq(@Param("deployId") Integer deployId);

    int countExec(@Param("deployId") Integer deployId);

    List<DeployLog> selectExecAfter(@Param("deployId") Integer deployId,
                                    @Param("cursor") Integer cursor,
                                    @Param("limit") Integer limit);

    List<DeployLog> selectExecHead(@Param("deployId") Integer deployId, @Param("limit") Integer limit);

    List<DeployLog> selectExecTail(@Param("deployId") Integer deployId, @Param("limit") Integer limit);

}
