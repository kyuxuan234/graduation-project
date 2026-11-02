package com.example.mapper;

import com.example.entity.AppInstance;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AppInstanceMapper {
    int insert(AppInstance row);

    int updateById(AppInstance row);

    AppInstance selectById(@Param("id") Integer id);

    AppInstance selectByAppAndEnv(@Param("appId") Integer appId, @Param("envCode") String envCode);

    List<AppInstance> selectByAppId(@Param("appId") Integer appId);

    List<AppInstance> selectOnline();

    int countByServerId(@Param("serverId") Integer serverId);

    int countOnlineByAppId(@Param("appId") Integer appId);

    int deleteByAppId(@Param("appId") Integer appId);
}
