package com.example.mapper;

import com.example.entity.Environment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EnvironmentMapper {
    List<Environment> selectAll();

    Environment selectByCode(@Param("code") String code);

    int updateById(Environment environment);
}
