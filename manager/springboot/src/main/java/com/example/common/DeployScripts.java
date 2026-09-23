package com.example.common;

import cn.hutool.core.util.StrUtil;

/**
 * 部署脚本在本地替换变量后再送给远程 bash -s。
 * 变量只允许字母、数字、点、下划线、斜杠和横线，避免把用户输入拼进 shell。
 */
public final class DeployScripts {

    private static final String SAFE = "[A-Za-z0-9._/\\-]+";

    private static final String DEPLOY = ""
            + "set -eu\n"
            + "APP_NAME='__APP_NAME__'\n"
            + "VERSION='__VERSION__'\n"
            + "DEPLOY_PATH='__DEPLOY_PATH__'\n"
            + "PACKAGE_FILE='__PACKAGE_FILE__'\n"
            + "mkdir -p \"$DEPLOY_PATH/packages\" \"$DEPLOY_PATH/releases/$VERSION\"\n"
            + "if [ -n \"$PACKAGE_FILE\" ] && [ -f \"$DEPLOY_PATH/packages/$PACKAGE_FILE\" ]; then\n"
            + "  cp \"$DEPLOY_PATH/packages/$PACKAGE_FILE\" \"$DEPLOY_PATH/releases/$VERSION/\"\n"
            + "fi\n"
            + "ln -sfn \"$DEPLOY_PATH/releases/$VERSION\" \"$DEPLOY_PATH/current\"\n"
            + "if [ -f \"$DEPLOY_PATH/current/docker-compose.yml\" ] || [ -f \"$DEPLOY_PATH/current/compose.yml\" ]; then\n"
            + "  (cd \"$DEPLOY_PATH/current\" && docker compose up -d)\n"
            + "fi\n"
            + "echo \"deploy ok $APP_NAME $VERSION\"\n";

    private static final String ROLLBACK = ""
            + "set -eu\n"
            + "VERSION='__VERSION__'\n"
            + "DEPLOY_PATH='__DEPLOY_PATH__'\n"
            + "if [ ! -d \"$DEPLOY_PATH/releases/$VERSION\" ]; then\n"
            + "  echo \"release not found: $VERSION\" >&2\n"
            + "  exit 2\n"
            + "fi\n"
            + "ln -sfn \"$DEPLOY_PATH/releases/$VERSION\" \"$DEPLOY_PATH/current\"\n"
            + "echo \"rollback ok $VERSION\"\n";

    private DeployScripts() {
    }

    public static String deploy(String appName, String version, String deployPath, String packageFile) {
        return fill(DEPLOY, appName, version, deployPath, packageFile == null ? "" : packageFile);
    }

    public static String rollback(String version, String deployPath) {
        return fill(ROLLBACK, "rollback", version, deployPath, "");
    }

    private static String fill(String template, String appName, String version, String deployPath, String packageFile) {
        check("应用标识", appName);
        check("版本", version);
        check("部署目录", deployPath);
        if (StrUtil.isNotBlank(packageFile)) {
            check("制品文件名", packageFile);
        }
        return template
                .replace("__APP_NAME__", appName)
                .replace("__VERSION__", version)
                .replace("__DEPLOY_PATH__", deployPath)
                .replace("__PACKAGE_FILE__", packageFile == null ? "" : packageFile)
                .replace("\r\n", "\n");
    }

    private static void check(String label, String value) {
        if (StrUtil.isBlank(value) || !value.matches(SAFE)) {
            throw new IllegalArgumentException(label + "包含非法字符");
        }
    }
}
