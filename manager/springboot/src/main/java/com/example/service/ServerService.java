package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.CredentialCipher;
import com.example.common.SshDeployExecutor;
import com.example.common.SshKeyMaterial;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.ServerStatusEnum;
import com.example.entity.Server;
import com.example.exception.BusinessException;
import com.example.mapper.AppMapper;
import com.example.mapper.ServerMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 服务器业务处理
 **/
@Service
public class ServerService {

    private static final Logger log = LoggerFactory.getLogger(ServerService.class);

    @Resource
    private ServerMapper serverMapper;

    @Resource
    private AppMapper appMapper;

    @Resource
    private CredentialCipher credentialCipher;

    @Resource
    private SshDeployExecutor sshDeployExecutor;

    /**
     * 新增
     */
    public void add(Server server) {
        checkUnique(server, null);
        encryptSecrets(server);
        if (ObjectUtil.isEmpty(server.getStatus())) {
            server.setStatus(ServerStatusEnum.RUNNING.name());
        }
        serverMapper.insert(server);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        Server dbServer = serverMapper.selectById(id);
        if (ObjectUtil.isNull(dbServer)) {
            throw new BusinessException(ResultCodeEnum.SERVER_NOT_EXIST_ERROR);
        }
        if (ServerStatusEnum.STOPPED.name().equals(dbServer.getStatus())
                || ServerStatusEnum.MAINTENANCE.name().equals(dbServer.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        // 该服务器下仍有应用时不允许删除
        if (appMapper.countByServerId(id) > 0) {
            throw new BusinessException(ResultCodeEnum.SERVER_HAS_APPS);
        }
        serverMapper.deleteById(id);
    }

    /**
     * 批量删除
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 修改
     */
    public void updateById(Server server) {
        checkUnique(server, server.getId());
        encryptSecrets(server);
        serverMapper.updateById(server);
    }

    /**
     * 主机名与IP唯一性校验，excludeId 为本次修改的记录id
     */
    private void checkUnique(Server server, Integer excludeId) {
        if (ObjectUtil.isNotEmpty(server.getHostname())) {
            Server dbServer = serverMapper.selectByHostname(server.getHostname());
            if (ObjectUtil.isNotNull(dbServer) && !dbServer.getId().equals(excludeId)) {
                throw new BusinessException(ResultCodeEnum.SERVER_HOSTNAME_EXIST_ERROR);
            }
        }
        if (ObjectUtil.isNotEmpty(server.getIp())) {
            Server dbServer = serverMapper.selectByIp(server.getIp());
            if (ObjectUtil.isNotNull(dbServer) && !dbServer.getId().equals(excludeId)) {
                throw new BusinessException(ResultCodeEnum.SERVER_IP_EXIST_ERROR);
            }
        }
    }

    /**
     * 根据ID查询
     */
    public Server selectById(Integer id) {
        Server server = serverMapper.selectById(id);
        hideSecrets(server);
        return server;
    }

    /**
     * 查询所有未停机的服务器，给前端下拉框用
     */
    public List<Server> selectAll() {
        List<Server> list = serverMapper.selectNotStopped();
        for (Server server : list) {
            hideSecrets(server);
        }
        return list;
    }

    /**
     * 分页查询
     */
    public PageInfo<Server> selectPage(Server server, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Server> list = serverMapper.selectAll(server);
        for (Server item : list) {
            hideSecrets(item);
        }
        return PageInfo.of(list);
    }

    private void encryptSecrets(Server server) {
        if (server.getSshPort() == null) {
            server.setSshPort(22);
        }
        String password = server.getSshPassword();
        server.setSshAuthType(null);
        server.setSshPrivateKey(null);
        if (StrUtil.isBlank(password)) {
            server.setSshPassword(null);
            return;
        }
        if (StrUtil.isBlank(server.getIp()) || StrUtil.isBlank(server.getSshUser())) {
            throw new BusinessException("400", "填写 SSH 密码时需要同时填写地址和用户");
        }
        SshKeyMaterial key = SshKeyMaterial.generate();
        int port = server.getSshPort();
        try {
            sshDeployExecutor.provisionLoginKey(server.getIp(), port, server.getSshUser(), password,
                    key.getPrivatePem(), key.getPublicLine());
        } catch (Exception e) {
            log.warn("用登记密码放置登录私钥失败 host={}", server.getHostname());
            throw new BusinessException("400", "登记的密码无法登录这台服务器，没有生成登录私钥");
        }
        server.setSshPassword(credentialCipher.encrypt(password));
        server.setSshPrivateKey(credentialCipher.encrypt(key.getPrivatePem()));
        server.setSshAuthType("KEY");
        log.info("已按登记密码生成登录私钥 host={}", server.getHostname());
    }

    private void hideSecrets(Server server) {
        if (server == null) {
            return;
        }
        server.setSshPassword(null);
        server.setSshPrivateKey(null);
    }

}
