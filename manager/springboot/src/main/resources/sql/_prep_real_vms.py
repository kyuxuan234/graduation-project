# -*- coding: utf-8 -*-
"""Prepare /opt/module on real lab VMs and verify SSH + git."""
import sys

import paramiko

HOSTS = [
    ("hadoop101", "192.168.116.131"),
    ("hadoop102", "192.168.116.132"),
    ("hadoop103", "192.168.116.133"),
]
USER = "kang"
PASSWORD = "centos7"
DEPLOY_PATH = "/opt/module"


def run(client, cmd, timeout=60):
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    code = stdout.channel.recv_exit_status()
    out = stdout.read().decode("utf-8", "ignore").strip()
    err = stderr.read().decode("utf-8", "ignore").strip()
    return code, out, err


def prep(name, ip):
    print(f"==> {name} ({ip})")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        client.connect(ip, port=22, username=USER, password=PASSWORD, timeout=12, allow_agent=False, look_for_keys=False)
    except Exception as e:
        print(f"  SSH FAIL: {e}")
        return False
    try:
        code, out, err = run(client, "uname -a; whoami; id")
        print(f"  login ok: {out.splitlines()[0] if out else err}")

        # 创建部署根目录：先直接建，不行再用登记密码走 sudo -S
        script = (
            f"set +e; "
            f"if [ ! -d '{DEPLOY_PATH}' ]; then "
            f"  mkdir -p '{DEPLOY_PATH}' 2>/dev/null && echo MKDIR_OK; "
            f"fi; "
            f"if [ ! -d '{DEPLOY_PATH}' ] || ! touch '{DEPLOY_PATH}/.write_test' 2>/dev/null; then "
            f"  echo '{PASSWORD}' | sudo -S mkdir -p '{DEPLOY_PATH}' && "
            f"  echo '{PASSWORD}' | sudo -S chown {USER}:{USER} '{DEPLOY_PATH}' && "
            f"  echo SUDO_OK; "
            f"fi; "
            f"rm -f '{DEPLOY_PATH}/.write_test' 2>/dev/null; "
            f"ls -ld '{DEPLOY_PATH}' 2>/dev/null || echo MISSING; "
            f"touch '{DEPLOY_PATH}/.write_test' && rm -f '{DEPLOY_PATH}/.write_test' && echo WRITE_OK || echo WRITE_FAIL; "
            f"mkdir -p '{DEPLOY_PATH}/releases' && echo RELEASES_OK || echo RELEASES_FAIL; "
            f"command -v git >/dev/null && git --version || echo GIT_MISSING; "
            f"command -v bash >/dev/null && echo BASH_OK || echo BASH_MISSING"
        )
        code, out, err = run(client, script)
        for line in (out or err).splitlines():
            print(f"  {line}")
        ok = "WRITE_OK" in out and "GIT_MISSING" not in out
        if "GIT_MISSING" in out:
            print("  WARN: git not installed; auto deploy will fail until git is available")
        if "NEED_SUDO" in out or "WRITE_FAIL" in out:
            print(f"  WARN: cannot create/write {DEPLOY_PATH}; run: sudo mkdir -p {DEPLOY_PATH} && sudo chown {USER}:{USER} {DEPLOY_PATH}")
            ok = False
        return ok
    finally:
        client.close()


def main():
    results = []
    for name, ip in HOSTS:
        results.append((name, prep(name, ip)))
    print("-- summary --")
    for name, ok in results:
        print(f"{name}: {'OK' if ok else 'FAIL'}")
    sys.exit(0 if all(ok for _, ok in results) else 1)


if __name__ == "__main__":
    main()
