# -*- coding: utf-8 -*-
"""发布流程时序图 → 论文插图/图5.png"""
from PIL import Image, ImageDraw, ImageFont

W, H = 1680, 1260
img = Image.new("RGB", (W, H), "white")
d = ImageDraw.Draw(img)

title_f = ImageFont.truetype(r"C:\Windows\Fonts\simhei.ttf", 24)
name_f = ImageFont.truetype(r"C:\Windows\Fonts\simhei.ttf", 16)
msg_f = ImageFont.truetype(r"C:\Windows\Fonts\simhei.ttf", 14)
note_f = ImageFont.truetype(r"C:\Windows\Fonts\simsun.ttc", 13)
small_f = ImageFont.truetype(r"C:\Windows\Fonts\simhei.ttf", 13)
tag_f = ImageFont.truetype(r"C:\Windows\Fonts\simhei.ttf", 12)


def tw(font, s):
    b = d.textbbox((0, 0), s, font=font)
    return b[2] - b[0], b[3] - b[1]


# 审查链在左，发布系统与运维相邻（后半段交互最密）
# 0开发 1技术负责人 2测试人员 3产品经理 4发布系统 5运维人员
names = ["开发人员", "技术负责人", "测试人员", "产品经理", "发布系统", "运维人员"]
xs = [120, 360, 600, 840, 1120, 1440]
DEV, TL, QA, PM, SYS, OPS = range(6)

BOX_W, BOX_H = 118, 36
top_y = 52
life_bottom = 1158
ML, MR = 32, W - 32

C_DEV = (232, 245, 233)
C_TEST = (255, 243, 224)
C_PROD = (252, 228, 236)
C_OPS = (227, 242, 253)
C_BOX = (248, 248, 248)
C_ACT = (210, 222, 236)

env0 = 130
hdr = 26
h_dev, h_test, h_prod = 70, 122, 180
env_h = hdr + h_dev + h_test + h_prod
y_dev = env0 + hdr
y_test = y_dev + h_dev
y_prod = y_test + h_test

rej0 = env0 + env_h + 12
rej_h = 50

ops0 = rej0 + rej_h + 14
ops_h = 390
IL, IR = ML + 16, MR - 16
exe0 = ops0 + 86
exe_h = 196
rb0 = exe0 + exe_h + 10


def head_box(x, y, name):
    d.rectangle(
        [x - BOX_W / 2, y, x + BOX_W / 2, y + BOX_H],
        outline="black",
        width=2,
        fill=C_BOX,
    )
    nw, nh = tw(name_f, name)
    d.text((x - nw / 2, y + (BOX_H - nh) / 2 - 1), name, fill="black", font=name_f)


def arrow(x1, x2, y, text, dashed=False):
    if dashed:
        x = min(x1, x2)
        end = max(x1, x2)
        while x < end - 8:
            nx = min(x + 6, end - 8)
            d.line([(x, y), (nx, y)], fill="black", width=2)
            x += 12
    else:
        d.line([(x1, y), (x2, y)], fill="black", width=2)
    if x2 >= x1:
        d.polygon([(x2, y), (x2 - 9, y - 4), (x2 - 9, y + 4)], fill="black")
    else:
        d.polygon([(x2, y), (x2 + 9, y - 4), (x2 + 9, y + 4)], fill="black")
    if text:
        mw, mh = tw(msg_f, text)
        mx = (x1 + x2) / 2 - mw / 2
        my = y - mh - 3
        d.rectangle([mx - 3, my - 1, mx + mw + 3, my + mh + 1], fill="white")
        d.text((mx, my), text, fill="black", font=msg_f)


def self_call(x, y, text):
    # 弧画在左侧，落在产品经理与系统之间
    d.arc([x - 46, y - 14, x - 5, y + 14], 90, 270, fill="black", width=2)
    d.polygon([(x - 5, y + 8), (x - 14, y + 4), (x - 12, y + 14)], fill="black")
    mw, mh = tw(msg_f, text)
    tx = x - 52 - mw
    d.rectangle([tx - 2, y - mh / 2 - 2, x - 50, y + mh / 2 + 2], fill="white")
    d.text((tx, y - mh / 2 - 1), text, fill="black", font=msg_f)


def frame(x0, y0, x1, y1, label):
    d.rectangle([x0, y0, x1, y1], outline=(45, 45, 45), width=2)
    lw, lh = tw(tag_f, label)
    d.rectangle([x0, y0, x0 + lw + 14, y0 + lh + 8], fill=(45, 45, 45))
    d.text((x0 + 6, y0 + 3), label, fill="white", font=tag_f)


def sep(y, x0=None, x1=None):
    x0 = ML + 2 if x0 is None else x0
    x1 = MR - 2 if x1 is None else x1
    x = x0
    while x < x1:
        nx = min(x + 8, x1)
        d.line([(x, y), (nx, y)], fill=(175, 175, 175), width=1)
        x += 14


def branch(y, text, hint=None, x0=None):
    x0 = ML + 12 if x0 is None else x0
    d.text((x0, y), text, fill=(20, 60, 110), font=small_f)
    if hint:
        bw, _ = tw(small_f, text)
        d.text((x0 + 4 + bw, y), hint, fill=(100, 100, 100), font=note_f)


def act(x, y):
    d.rectangle([x - 5, y - 5, x + 5, y + 9], outline="black", width=1, fill=C_ACT)


title = "发布流程时序图"
tw_, _ = tw(title_f, title)
d.text(((W - tw_) / 2, 8), title, fill="black", font=title_f)

# 色带
d.rectangle([ML + 3, y_dev, MR - 3, y_test], fill=C_DEV)
d.rectangle([ML + 3, y_test, MR - 3, y_prod], fill=C_TEST)
d.rectangle([ML + 3, y_prod, MR - 3, env0 + env_h], fill=C_PROD)
d.rectangle([ML + 3, ops0 + 1, MR - 3, ops0 + ops_h - 1], fill=C_OPS)
d.rectangle([IL + 2, exe0 + 2, IR - 2, exe0 + exe_h - 2], fill="white")
d.rectangle([IL + 2, rb0 + 2, IR - 2, ops0 + ops_h - 6], fill="white")

for x in xs:
    d.line([(x, top_y + BOX_H), (x, life_bottom)], fill=(170, 170, 170), width=1)

for name, x in zip(names, xs):
    head_box(x, top_y, name)

frame(ML, env0, MR, env0 + env_h, "alt 目标环境")
frame(ML, rej0, MR, rej0 + rej_h, "opt 审查 / 验收 / 审批驳回")
frame(ML, ops0, MR, ops0 + ops_h, "运维审批与执行")
frame(IL, exe0, IR, exe0 + exe_h, "alt 执行方式")
frame(IL, rb0, IR, ops0 + ops_h - 4, "opt 仅失败单可回滚（同环境有历史成功版本）")

# 提交
y = 106
act(xs[DEV], y)
act(xs[SYS], y)
arrow(xs[DEV], xs[SYS], y, "提交发布申请（应用、版本、目标环境）")

# 开发
sep(y_dev)
branch(y_dev + 4, "[开发环境]")
y = y_dev + 38
act(xs[SYS], y)
act(xs[OPS], y)
arrow(xs[SYS], xs[OPS], y, "待运维审批")

# 测试
sep(y_test)
branch(y_test + 4, "[测试环境]", "（版本须已在开发环境发布成功）")
y = y_test + 34
act(xs[SYS], y)
act(xs[QA], y)
arrow(xs[SYS], xs[QA], y, "待测试验收")
y = y_test + 64
act(xs[QA], y)
act(xs[SYS], y)
arrow(xs[QA], xs[SYS], y, "验收通过")
y = y_test + 94
act(xs[SYS], y)
act(xs[OPS], y)
arrow(xs[SYS], xs[OPS], y, "待运维审批")

# 生产
sep(y_prod)
branch(y_prod + 4, "[生产环境]", "（版本须已在测试环境发布成功）")
y = y_prod + 34
act(xs[SYS], y)
act(xs[TL], y)
arrow(xs[SYS], xs[TL], y, "待技术审查")
y = y_prod + 64
act(xs[TL], y)
act(xs[SYS], y)
arrow(xs[TL], xs[SYS], y, "审查通过")
y = y_prod + 94
act(xs[SYS], y)
act(xs[PM], y)
arrow(xs[SYS], xs[PM], y, "待产品验收")
y = y_prod + 124
act(xs[PM], y)
act(xs[SYS], y)
arrow(xs[PM], xs[SYS], y, "验收通过")
y = y_prod + 154
act(xs[SYS], y)
act(xs[OPS], y)
arrow(xs[SYS], xs[OPS], y, "待运维审批")

# 驳回
y = rej0 + 30
act(xs[SYS], y)
act(xs[DEV], y)
arrow(xs[SYS], xs[DEV], y, "已驳回，流程结束", dashed=True)

# 审批
y = ops0 + 34
act(xs[OPS], y)
act(xs[SYS], y)
arrow(xs[OPS], xs[SYS], y, "审批通过（状态与乐观锁版本号校验）")
y = ops0 + 64
act(xs[SYS], y)
act(xs[OPS], y)
arrow(xs[SYS], xs[OPS], y, "进入待执行", dashed=True)

# 自动
sep(exe0 + 24, IL + 2, IR - 2)
branch(exe0 + 28, "[自动发布]", x0=IL + 10)
y = exe0 + 54
act(xs[OPS], y)
act(xs[SYS], y)
arrow(xs[OPS], xs[SYS], y, "开始自动发布")
y = exe0 + 84
act(xs[SYS], y)
self_call(xs[SYS], y, "事务提交后 SSH 拉取版本并回写实例")
y = exe0 + 114
act(xs[SYS], y)
act(xs[OPS], y)
arrow(xs[SYS], xs[OPS], y, "发布成功 / 发布失败", dashed=True)

# 人工
sep(exe0 + 138, IL + 2, IR - 2)
branch(exe0 + 142, "[人工发布]", x0=IL + 10)
y = exe0 + 170
act(xs[OPS], y)
act(xs[SYS], y)
arrow(xs[OPS], xs[SYS], y, "开始人工发布，页面终端确认 → 成功 / 失败")

# 回滚
y = rb0 + 30
act(xs[OPS], y)
act(xs[SYS], y)
arrow(xs[OPS], xs[SYS], y, "回滚到同环境上次成功版本")
y = rb0 + 58
act(xs[SYS], y)
act(xs[OPS], y)
arrow(xs[SYS], xs[OPS], y, "重新进入发布中，类型改为回滚", dashed=True)

for name, x in zip(names, xs):
    head_box(x, life_bottom, name)

notes = [
    "开发环境提交后直接交运维；测试环境先验收；生产环境经技术审查与产品验收。",
    "成功只回写该环境应用实例；失败单可回滚到同一环境的上一次成功版本。",
]
d.text((ML + 6, 1208), notes[0], fill="black", font=note_f)
d.text((ML + 6, 1232), notes[1], fill="black", font=note_f)

out = r"d:\Encoding\IntelliJProject\graduation-project\论文插图\图5.png"
img.save(out, "PNG")
print(out)
