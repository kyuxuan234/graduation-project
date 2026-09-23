package com.example.common;

import com.example.entity.Server;

import java.util.function.Consumer;

/**
 * 远程执行。实现类负责连接、上传和超时，不负责改部署单状态。
 */
public interface DeployExecutor {

    void testConnection(Server server) throws Exception;

    void upload(Server server, String localPath, String remoteDir) throws Exception;

    String remoteMd5(Server server, String remoteFile) throws Exception;

    /**
     * 把脚本从标准输入交给远程 bash -s，按行回调输出。返回退出码。
     */
    int runScript(Server server, String script, Consumer<String> lines) throws Exception;

    /**
     * 执行一条短命令，失败抛异常，成功返回标准输出。
     */
    String runCommand(Server server, String command) throws Exception;
}
