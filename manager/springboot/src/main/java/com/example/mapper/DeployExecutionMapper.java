package com.example.mapper;

import com.example.entity.DeployExecution;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployExecutionMapper {

    int insert(DeployExecution execution);

    int updateById(DeployExecution execution);

    DeployExecution selectLatest(@Param("deployId") Integer deployId);

    DeployExecution selectRunning(@Param("deployId") Integer deployId);

    List<DeployExecution> selectByStatus(@Param("status") String status);
}
