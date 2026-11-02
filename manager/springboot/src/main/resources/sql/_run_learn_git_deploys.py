# -*- coding: utf-8 -*-
"""Run AUTO then MANUAL learn-git deploys; MANUAL also does real git pull over SSH."""
import json
import time
import urllib.error
import urllib.parse
import urllib.request

import paramiko

BASE = "http://localhost:9090"
HOST = "192.168.116.131"
USER = "kang"
PASSWORD = "centos7"


def req(method, path, token=None, body=None, params=None):
    url = BASE + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    data = None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["token"] = token
    if body is not None:
        data = json.dumps(body).encode("utf-8")
    r = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(r, timeout=90) as resp:
        return json.loads(resp.read().decode("utf-8"))


def ssh(cmd, timeout=180):
    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect(HOST, username=USER, password=PASSWORD, timeout=20, allow_agent=False, look_for_keys=False)
    try:
        stdin, stdout, stderr = c.exec_command(cmd, timeout=timeout)
        out = stdout.read().decode("utf-8", "ignore")
        err = stderr.read().decode("utf-8", "ignore")
        code = stdout.channel.recv_exit_status()
        return code, out, err
    finally:
        c.close()


def log_text(token, deploy_id):
    logs = req("GET", f"/deploy/{deploy_id}/logs", token=token, params={"cursor": 0})
    payload = logs.get("data") or {}
    lines = payload.get("lines") or []
    parts = []
    for x in lines:
        if isinstance(x, dict):
            parts.append(x.get("detail") or x.get("content") or str(x))
        else:
            parts.append(str(x))
    return "\n".join(parts)


def main():
    login = req("POST", "/login", body={"username": "ops1", "password": "123456"})
    token = (login.get("data") or {}).get("token")
    if not token:
        raise SystemExit(login)
    print("login ok")

    print("== AUTO ==")
    print(req("PUT", "/deploy/startDeploy/30", token=token, params={"executeMode": "AUTO"}))
    st = None
    for i in range(40):
        time.sleep(4)
        st = (req("GET", "/deploy/selectById/30", token=token).get("data") or {}).get("status")
        text = log_text(token, 30)
        print(f"-- poll {i+1} status={st} --")
        print(text[-1200:])
        if st in ("SUCCESS", "FAILED"):
            break
    print("AUTO final", st)
    code, out, err = ssh("ls -la /opt/module /opt/module/releases /opt/module/current 2>&1; git -C /opt/module/releases/master remote -v 2>&1; git -C /opt/module/releases/master log -1 --oneline 2>&1")
    print("VM after AUTO", code, out, err)

    print("== MANUAL ==")
    # keep existing master for fetch path, or re-pull into same dir as operator would
    print(req("PUT", "/deploy/startDeploy/31", token=token, params={"executeMode": "MANUAL"}))
    manual_cmd = r"""
set -e
DEPLOY_PATH=/opt/module
VERSION=master
REPO_URL='https://github.com/kyuxuan234/learn-git.git'
DEST="$DEPLOY_PATH/releases/$VERSION"
mkdir -p "$DEPLOY_PATH/releases"
if [ -d "$DEST/.git" ]; then
  echo "[manual] repo exists, fetch & checkout $VERSION"
  git -C "$DEST" fetch --tags --force origin || true
  git -C "$DEST" checkout --force "$VERSION"
else
  echo "[manual] cloning $REPO_URL"
  rm -rf "$DEST"
  git clone --branch "$VERSION" "$REPO_URL" "$DEST"
fi
ln -sfn "$DEST" "$DEPLOY_PATH/current"
echo "[manual] deploy ok order-service $VERSION"
ls -la "$DEPLOY_PATH/current"
git -C "$DEST" remote -v
git -C "$DEST" log -1 --oneline
"""
    code, out, err = ssh(manual_cmd)
    print("manual ssh", code)
    print(out)
    print(err)
    print(req("PUT", "/deploy/finishDeploy/31", token=token, params={"result": "SUCCESS"}))
    print("MANUAL status", (req("GET", "/deploy/selectById/31", token=token).get("data") or {}).get("status"))
    print("MANUAL logs:\n", log_text(token, 31))


if __name__ == "__main__":
    main()
