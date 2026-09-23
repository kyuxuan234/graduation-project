package com.example.mapper;

import com.example.entity.DeployArtifact;
import org.apache.ibatis.annotations.Param;

public interface DeployArtifactMapper {

    int insert(DeployArtifact artifact);

    DeployArtifact selectLatest(@Param("appId") Integer appId, @Param("version") String version);
}
