package com.example.common;

import cn.hutool.core.util.StrUtil;

/**
 * 部署脚本在本地替换变量后再送给远程 bash -s。
 * 应用标识、版本、部署目录只允许字母、数字、点、下划线和斜杠。
 * 仓库地址单独校验，并以单引号写入脚本。
 */
public final class DeployScripts {

    private static final String SAFE = "[A-Za-z0-9._/\\-]+";

    private static final String DEPLOY = ""
            + "set -eu\n"
            + "APP_NAME='__APP_NAME__'\n"
            + "VERSION='__VERSION__'\n"
            + "DEPLOY_PATH='__DEPLOY_PATH__'\n"
            + "REPO_URL='__REPO_URL__'\n"
            + "DEST=\"$DEPLOY_PATH/releases/$VERSION\"\n"
            + "mkdir -p \"$DEPLOY_PATH/releases\"\n"
            + "if [ -d \"$DEST/.git\" ]; then\n"
            + "  git -C \"$DEST\" fetch --tags --force origin\n"
            + "  git -C \"$DEST\" checkout --force \"$VERSION\"\n"
            + "else\n"
            + "  rm -rf \"$DEST\"\n"
            + "  git clone --branch \"$VERSION\" --depth 1 \"$REPO_URL\" \"$DEST\"\n"
            + "fi\n"
            + "ln -sfn \"$DEST\" \"$DEPLOY_PATH/current\"\n"
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

    public static String deploy(String appName, String version, String deployPath, String repoUrl) {
        return fill(DEPLOY, appName, version, deployPath, repoUrl);
    }

    public static String rollback(String version, String deployPath) {
        return fill(ROLLBACK, "rollback", version, deployPath, "");
    }

    private static String fill(String template, String appName, String version, String deployPath, String repoUrl) {
        check("应用标识", appName);
        check("版本", version);
        check("部署目录", deployPath);
        if (template.contains("__REPO_URL__")) {
            checkRepo(repoUrl);
        }
        return template
                .replace("__APP_NAME__", appName)
                .replace("__VERSION__", version)
                .replace("__DEPLOY_PATH__", deployPath)
                .replace("__REPO_URL__", repoUrl == null ? "" : repoUrl)
                .replace("\r\n", "\n");
    }

    private static void checkRepo(String repoUrl) {
        if (StrUtil.isBlank(repoUrl)
                || repoUrl.indexOf('\'') >= 0
                || repoUrl.indexOf('"') >= 0
                || repoUrl.indexOf('\n') >= 0
                || repoUrl.indexOf('\r') >= 0
                || repoUrl.indexOf(' ') >= 0
                || repoUrl.indexOf('\\') >= 0
                || repoUrl.contains("$")
                || repoUrl.contains("`")
                || repoUrl.contains(";")
                || repoUrl.contains("|")
                || repoUrl.contains("&")) {
            throw new IllegalArgumentException("代码仓库地址包含非法字符");
        }
        if (!repoUrl.startsWith("https://") && !repoUrl.startsWith("http://") && !repoUrl.startsWith("git@")) {
            throw new IllegalArgumentException("代码仓库地址需以 http://、https:// 或 git@ 开头");
        }
    }

    private static void check(String label, String value) {
        if (StrUtil.isBlank(value) || !value.matches(SAFE)) {
            throw new IllegalArgumentException(label + "包含非法字符");
        }
    }
}
