package com.example.service;

import com.example.entity.DeployLog;
import com.example.mapper.DeployLogMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 部署流转记录业务处理
 * 记录只由Deploy模块内部写入，这里只提供查询
 **/
@Service
public class DeployLogService {

    @Resource
    private DeployLogMapper deployLogMapper;

    /**
     * 根据ID查询
     */
    public DeployLog selectById(Integer id) {
        return deployLogMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<DeployLog> selectAll(DeployLog deployLog) {
        return deployLogMapper.selectAll(deployLog);
    }

    /**
     * 分页查询
     */
    public PageInfo<DeployLog> selectPage(DeployLog deployLog, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<DeployLog> list = deployLogMapper.selectAll(deployLog);
        return PageInfo.of(list);
    }

}
