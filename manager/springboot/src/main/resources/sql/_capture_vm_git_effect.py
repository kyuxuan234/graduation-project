# -*- coding: utf-8 -*-
import paramiko
from PIL import Image, ImageDraw, ImageFont

HOST = "192.168.116.131"
cmd = "\n".join([
    "echo '=== hadoop101 /opt/module after learn-git pull ==='",
    "echo '$ ls -la /opt/module'",
    "ls -la /opt/module",
    "echo",
    "echo '$ ls -la /opt/module/releases'",
    "ls -la /opt/module/releases",
    "echo",
    "echo '$ ls -la /opt/module/current'",
    "ls -la /opt/module/current",
    "echo",
    "echo '$ readlink -f /opt/module/current'",
    "readlink -f /opt/module/current",
    "echo",
    "cd /opt/module/releases/master && git remote set-url origin https://github.com/kyuxuan234/learn-git.git",
    "echo '$ cd /opt/module/releases/master && git remote -v'",
    "cd /opt/module/releases/master && git remote -v",
    "echo",
    "echo '$ git log -1 --oneline'",
    "git log -1 --oneline",
    "echo",
    "echo '$ git branch'",
    "git branch",
    "echo",
    "echo '$ ls | head'",
    "ls | head",
])

c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect(HOST, username="kang", password="centos7", timeout=12, allow_agent=False, look_for_keys=False)
stdin, stdout, stderr = c.exec_command(cmd)
text = stdout.read().decode("utf-8", "ignore") + stderr.read().decode("utf-8", "ignore")
c.close()
print(text)

font = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 16)
lines = text.splitlines() or ["(empty)"]
W = 980
H = 40 + len(lines) * 22 + 30
img = Image.new("RGB", (W, H), (12, 16, 24))
d = ImageDraw.Draw(img)
d.text((18, 12), "kang@hadoop101:~", fill=(80, 200, 120), font=font)
y = 40
for line in lines:
    d.text((18, y), line[:120], fill=(220, 230, 240), font=font)
    y += 22
out = r"d:\Encoding\IntelliJProject\graduation-project\论文插图\图17c.png"
img.save(out)
print("WROTE", out)
