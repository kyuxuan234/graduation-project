package com.example.mapper;

import com.example.entity.Alarm;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作alarm相关数据接口
*/
public interface AlarmMapper {

    /**
      * 新增
    */
    int insert(Alarm alarm);

    /**
      * 删除（逻辑删除）
    */
    int deleteById(Integer id);

    /**
      * 修改
    */
    int updateById(Alarm alarm);

    /**
      * 根据ID查询
    */
    Alarm selectById(Integer id);

    /**
      * 查询所有
    */
    List<Alarm> selectAll(Alarm alarm);

    Alarm selectLatestByMarker(@Param("marker") String marker,
                               @Param("appId") Integer appId,
                               @Param("serverId") Integer serverId);

    int updateContent(@Param("id") Integer id, @Param("content") String content);

}
