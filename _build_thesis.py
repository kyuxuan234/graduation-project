# -*- coding: utf-8 -*-
"""Rebuild the thesis: undergraduate length, no duplicated paragraphs."""
from pathlib import Path

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING, WD_TAB_ALIGNMENT, WD_TAB_LEADER
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt

ROOT = Path(r"d:\Encoding\IntelliJProject\graduation-project")
OUT = ROOT / "毕业作业_DevOps发布管理系统_优化稿.docx"
FIG = ROOT / "论文插图"

CN, HEI, EN = "宋体", "黑体", "Times New Roman"


def set_run_font(run, cn, size, bold=False, en=EN):
    run.bold = bold
    run.italic = False
    run.font.size = Pt(size)
    run.font.name = en
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.find(qn("w:rFonts"))
    if rFonts is None:
        rFonts = OxmlElement("w:rFonts")
        rPr.append(rFonts)
    rFonts.set(qn("w:ascii"), en)
    rFonts.set(qn("w:hAnsi"), en)
    rFonts.set(qn("w:cs"), en)
    rFonts.set(qn("w:eastAsia"), cn)


def set_pf(p, align="left", before=0, after=0, first=0, single=False):
    pf = p.paragraph_format
    pf.space_before = Pt(before)
    pf.space_after = Pt(after)
    pf.line_spacing_rule = WD_LINE_SPACING.SINGLE if single else WD_LINE_SPACING.ONE_POINT_FIVE
    p.alignment = {
        "center": WD_ALIGN_PARAGRAPH.CENTER,
        "right": WD_ALIGN_PARAGRAPH.RIGHT,
        "justify": WD_ALIGN_PARAGRAPH.JUSTIFY,
    }.get(align, WD_ALIGN_PARAGRAPH.LEFT)
    pf.first_line_indent = Pt(first)
    pPr = p._p.get_or_add_pPr()
    spacing = pPr.find(qn("w:spacing"))
    if spacing is None:
        spacing = OxmlElement("w:spacing")
        pPr.append(spacing)
    spacing.set(qn("w:beforeAutospacing"), "0")
    spacing.set(qn("w:afterAutospacing"), "0")


def add_text(p, text, cn, size, bold=False, en=EN):
    run = p.add_run(text)
    set_run_font(run, cn, size, bold, en)
    return run


def outline(p, level):
    pPr = p._p.get_or_add_pPr()
    el = pPr.find(qn("w:outlineLvl"))
    if el is None:
        el = OxmlElement("w:outlineLvl")
        pPr.append(el)
    el.set(qn("w:val"), str(level))


def configure_section(section, fmt=None, start=None):
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.6)
    section.bottom_margin = Cm(2.6)
    section.left_margin = Cm(3.0)
    section.right_margin = Cm(3.0)
    section.header_distance = Cm(1.8)
    section.footer_distance = Cm(1.8)
    sectPr = section._sectPr
    old = sectPr.find(qn("w:pgNumType"))
    if old is not None:
        sectPr.remove(old)
    if fmt:
        pg = OxmlElement("w:pgNumType")
        pg.set(qn("w:fmt"), fmt)
        if start is not None:
            pg.set(qn("w:start"), str(start))
        sectPr.append(pg)
    footer = section.footer
    footer.is_linked_to_previous = False
    p = footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.clear()
    if fmt:
        run = p.add_run()
        set_run_font(run, EN, 9, False, EN)
        begin = OxmlElement("w:fldChar")
        begin.set(qn("w:fldCharType"), "begin")
        instr = OxmlElement("w:instrText")
        instr.set(qn("xml:space"), "preserve")
        instr.text = " PAGE "
        end = OxmlElement("w:fldChar")
        end.set(qn("w:fldCharType"), "end")
        run._r.append(begin)
        run._r.append(instr)
        run._r.append(end)
    header = section.header
    header.is_linked_to_previous = False
    header.paragraphs[0].clear()


def title_para(doc, text, size=18, before=24, after=24, outline_level=None, font=HEI):
    p = doc.add_paragraph()
    set_pf(p, "center", before, after, single=True)
    add_text(p, text, font, size, True)
    if outline_level is not None:
        outline(p, outline_level)
    return p


def body(doc, text):
    p = doc.add_paragraph()
    set_pf(p, "justify", 0, 0, first=24)
    add_text(p, text, CN, 12, False)
    return p


def mixed(doc, parts, first=24, align="left"):
    p = doc.add_paragraph()
    set_pf(p, align, 0, 0, first=first)
    for text, cn, size, bold in parts:
        add_text(p, text, cn, size, bold)
    return p


def h1(doc, text):
    p = doc.add_paragraph()
    set_pf(p, "left", 24, 24, single=True)
    add_text(p, text, HEI, 18, True)
    outline(p, 0)
    return p


def h2(doc, text):
    p = doc.add_paragraph()
    set_pf(p, "left", 18, 18, single=True)
    add_text(p, text, HEI, 15, True)
    outline(p, 1)
    return p


def h3(doc, text):
    p = doc.add_paragraph()
    set_pf(p, "left", 12, 12, single=True)
    add_text(p, text, HEI, 12, True)
    outline(p, 2)
    return p


def shade_cell(cell, color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), color)
    tcPr.append(shd)


def set_table_widths(table, widths):
    table.autofit = False
    table.allow_autofit = False
    tbl = table._tbl
    tblPr = tbl.tblPr
    tblW = tblPr.find(qn("w:tblW"))
    if tblW is None:
        tblW = OxmlElement("w:tblW")
        tblPr.append(tblW)
    total = sum(widths)
    tblW.set(qn("w:w"), str(int(total * 567)))
    tblW.set(qn("w:type"), "dxa")
    grid = tbl.find(qn("w:tblGrid"))
    if grid is not None:
        for child in list(grid):
            grid.remove(child)
    else:
        grid = OxmlElement("w:tblGrid")
        tblPr.addnext(grid)
    for w in widths:
        gc = OxmlElement("w:gridCol")
        gc.set(qn("w:w"), str(int(w * 567)))
        grid.append(gc)
    for row in table.rows:
        for i, cell in enumerate(row.cells):
            tcPr = cell._tc.get_or_add_tcPr()
            tcW = tcPr.find(qn("w:tcW"))
            if tcW is None:
                tcW = OxmlElement("w:tcW")
                tcPr.append(tcW)
            tcW.set(qn("w:w"), str(int(widths[i] * 567)))
            tcW.set(qn("w:type"), "dxa")


def prevent_split(row):
    trPr = row._tr.get_or_add_trPr()
    cant = OxmlElement("w:cantSplit")
    trPr.append(cant)


def figure_img(doc, num, title, wide=14.5, file_name=None, caption=None):
    p = doc.add_paragraph()
    set_pf(p, "center", 6, 3, single=True)
    run = p.add_run()
    run.add_picture(str(FIG / (file_name or f"图{num}.png")), width=Cm(wide))
    cap = doc.add_paragraph()
    set_pf(cap, "center", 3, 12, single=True)
    add_text(cap, caption or f"图{num}  {title}", HEI, 10.5, True)


def figure_blank(doc, num, title, hint):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_widths(table, [15.0])
    cell = table.cell(0, 0)
    shade_cell(cell, "F3F3F3")
    cell.paragraphs[0].clear()
    for i, (text, size, bold) in enumerate([
        (f"【图{num}留空】", 14, True),
        (hint, 12, False),
    ]):
        p = cell.paragraphs[0] if i == 0 else cell.add_paragraph()
        set_pf(p, "center", 3, 3, single=True)
        add_text(p, text, HEI if bold else CN, size, bold)
    trPr = table.rows[0]._tr.get_or_add_trPr()
    trHeight = OxmlElement("w:trHeight")
    trHeight.set(qn("w:val"), "2200")
    trHeight.set(qn("w:hRule"), "atLeast")
    trPr.append(trHeight)
    cap = doc.add_paragraph()
    set_pf(cap, "center", 3, 12, single=True)
    add_text(cap, f"图{num}  {title}", HEI, 10.5, True)


def table_title(doc, text):
    p = doc.add_paragraph()
    set_pf(p, "center", 12, 3, single=True)
    add_text(p, text, HEI, 10.5, True)


def add_data_table(doc, headers, rows, widths, data_align="center"):
    table = doc.add_table(rows=1 + len(rows), cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    set_table_widths(table, widths)
    for i, h in enumerate(headers):
        cell = table.rows[0].cells[i]
        cell.paragraphs[0].clear()
        p = cell.paragraphs[0]
        set_pf(p, "center", 0, 0, single=True)
        add_text(p, h, HEI, 10.5, True)
        shade_cell(cell, "E7E6E6")
    prevent_split(table.rows[0])
    for r, row in enumerate(rows):
        prevent_split(table.rows[r + 1])
        for c, val in enumerate(row):
            cell = table.rows[r + 1].cells[c]
            cell.paragraphs[0].clear()
            p = cell.paragraphs[0]
            set_pf(p, data_align, 0, 0, single=True)
            add_text(p, val, CN, 10.5, False)
    return table


def table_note(doc, text):
    p = doc.add_paragraph()
    set_pf(p, "left", 0, 6, single=True)
    add_text(p, text, CN, 10.5, False)


def toc_line(doc, text, page, level):
    p = doc.add_paragraph()
    set_pf(p, "left", 0, 0)
    if level == 1:
        p.paragraph_format.left_indent = Cm(0.74)
    elif level == 2:
        p.paragraph_format.left_indent = Cm(1.48)
    p.paragraph_format.tab_stops.add_tab_stop(Cm(15.0), WD_TAB_ALIGNMENT.RIGHT, WD_TAB_LEADER.DOTS)
    add_text(p, f"{text}\t{page}", HEI, 12, False)


def cover_line(doc, label, value, before=6):
    p = doc.add_paragraph()
    set_pf(p, "center", before, 0, single=True)
    add_text(p, label, CN, 14, False)
    p2 = doc.add_paragraph()
    set_pf(p2, "center", 0, 6, single=True)
    add_text(p2, value, CN, 14, False)


def build():
    doc = Document()
    normal = doc.styles["Normal"]
    normal.font.name = EN
    normal.font.size = Pt(12)
    rPr = normal.element.get_or_add_rPr()
    rFonts = rPr.find(qn("w:rFonts"))
    if rFonts is None:
        rFonts = OxmlElement("w:rFonts")
        rPr.append(rFonts)
    rFonts.set(qn("w:ascii"), EN)
    rFonts.set(qn("w:hAnsi"), EN)
    rFonts.set(qn("w:eastAsia"), CN)
    normal.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
    normal.paragraph_format.space_before = Pt(0)
    normal.paragraph_format.space_after = Pt(0)
    configure_section(doc.sections[0], fmt=None)

    p = doc.add_paragraph()
    set_pf(p, "center", 36, 12, single=True)
    add_text(p, "毕业设计（论文）", HEI, 22, True)
    p = doc.add_paragraph()
    set_pf(p, "center", 18, 0, single=True)
    add_text(p, "毕业设计（论文）题目：", CN, 14, False)
    p = doc.add_paragraph()
    set_pf(p, "center", 12, 6, single=True)
    add_text(p, "DevOps发布管理系统的设计与实现", CN, 14, False)
    p = doc.add_paragraph()
    set_pf(p, "center", 0, 12, single=True)
    add_text(p, "——面向中小软件开发团队", CN, 14, False)
    cover_line(doc, "学院/分校：", "黄浦分校", 18)
    cover_line(doc, "年级、专业：", "2026秋软件工程")
    cover_line(doc, "教育层次：", "专科")
    cover_line(doc, "学生姓名：", "姓名")
    cover_line(doc, "学    号：", "学号")
    cover_line(doc, "指导教师：", "　　　　　　")
    cover_line(doc, "完成日期：", "2026年10月")

    doc.add_page_break()
    title_para(doc, "论文独创性声明")
    body(doc, "本人郑重声明：所呈交的毕业设计（论文），是本人在老师指导下，独立进行研究所取得的成果。论文中除了已经注明引用的内容外，不包含任何他人享有的著作权内容。其他个人和集体对本研究工作的启发和所做出的贡献，均已在论文中以明确的方式标明。如本文被查证有抄袭或剽窃行为，本人愿意承担由此引发的法律后果，并依据学校的规章制度接受相应处理。")
    p = doc.add_paragraph()
    set_pf(p, "left", 18, 6, first=24)
    add_text(p, "签  名：                    日  期：          年    月    日", CN, 12, False)
    p = doc.add_paragraph()
    set_pf(p, "left", 6, 6)
    add_text(p, "*************************************", EN, 12, False)
    title_para(doc, "论文版权使用授权声明", 18, 12, 18)
    body(doc, "本人完全了解上海开放大学关于收集、保存、使用毕业论文的规定，同意如下各项内容：按照学校要求提交学位论文的印刷本和电子版本；学校有权保存学位论文的印刷本和电子版，并采用影印、缩印、扫描、数字化或其它手段保存论文；学校有权提供目录检索以及提供本论文全文或者部分的阅览服务，以及出版学位论文；学校有权按有关规定向国家有关部门或者机构送交论文的复印件和电子版；在不以赢利为目的的前提下，学校可以适当复制论文的部分或全部内容用于学术活动。")
    p = doc.add_paragraph()
    set_pf(p, "left", 18, 0, first=24)
    add_text(p, "签  名：                    日  期：          年    月    日", CN, 12, False)

    doc.add_page_break()
    title_para(doc, "目  录")
    toc_items = [
        ("摘  要", "I", 0), ("Abstract", "II", 0),
        ("一、绪论", "1", 0), ("（一）研究背景及意义", "1", 1),
        ("（二）国内外研究现状", "2", 1), ("1.国内研究综述", "2", 2),
        ("2.国外研究现状", "3", 2), ("（三）主要研究内容", "4", 1),
        ("1.角色与权限控制", "4", 2), ("2.以应用为枢纽的资产台账", "4", 2),
        ("3.按环境分流的状态机与乐观锁", "4", 2), ("4.自动与人工两条执行路径", "5", 2),
        ("5.探活告警与发布报表", "5", 2),
        ("二、系统开发关键技术概述", "5", 0), ("（一）系统开发环境", "5", 1),
        ("1.运行环境与端口", "5", 2), ("2.数据库初始化", "5", 2), ("3.开发与生产配置", "6", 2),
        ("（二）Java语言", "6", 1), ("（三）Spring Boot框架", "6", 1),
        ("（四）MyBatis框架", "7", 1), ("（五）Vue框架", "7", 1),
        ("（六）MySQL与SSH相关技术", "8", 1),
        ("1.MySQL", "8", 2), ("2.SSH与远程执行", "8", 2),
        ("三、系统分析", "9", 0), ("（一）系统可行性分析", "9", 1),
        ("1.技术可行性", "9", 2), ("2.经济可行性", "10", 2), ("3.操作可行性", "10", 2),
        ("（二）系统需求分析", "10", 1), ("1.功能性需求分析", "10", 2),
        ("2.非功能性需求分析", "12", 2), ("（三）系统界面分析", "13", 1),
        ("1.输出设计", "13", 2), ("2.输入设计", "13", 2),
        ("（四）系统流程和逻辑", "14", 1),
        ("1.登录流程", "14", 2), ("2.发布单流转", "14", 2), ("3.台账回写与并发", "15", 2),
        ("四、系统概要设计", "16", 0), ("（一）概述", "16", 1),
        ("（二）系统结构", "16", 1),
        ("1.分层结构", "16", 2), ("2.功能模块", "17", 2),
        ("（三）数据库设计", "17", 1),
        ("1.数据库实体", "17", 2), ("2.数据库设计表", "18", 2),
        ("五、系统详细实现", "24", 0), ("（一）登录与注册", "24", 1),
        ("（二）发布看板", "25", 1), ("（三）项目管理", "25", 1),
        ("（四）服务器与应用管理", "26", 1), ("（五）发布单与执行", "26", 1),
        ("1.提交与审批", "26", 2), ("2.自动发布与人工发布", "27", 2), ("3.回滚", "27", 2),
        ("（六）告警与发布报表", "28", 1), ("1.告警模块", "28", 2),
        ("2.发布报表模块", "28", 2), ("（七）人员、公告与安全控制", "29", 1),
        ("1.人员与公告", "29", 2), ("2.安全控制", "29", 2),
        ("六、系统测试", "30", 0), ("（一）测试目的", "30", 1),
        ("（二）测试环境与方法", "30", 1),
        ("1.测试环境", "30", 2), ("2.测试方法", "30", 2),
        ("（三）功能测试", "31", 1),
        ("（四）可用性测试", "32", 1), ("（五）单元测试", "32", 1),
        ("（六）测试结果分析", "33", 1),
        ("七、结论", "34", 0),
        ("（一）工作总结", "34", 1), ("（二）不足与展望", "34", 1),
        ("参考文献", "35", 0), ("致  谢", "36", 0),
    ]
    for text, page, level in toc_items:
        toc_line(doc, text, page, level)

    sec_abs = doc.add_section()
    configure_section(sec_abs, "upperRoman", 1)
    title_para(doc, "摘  要", outline_level=0)
    body_abs = (
        "中小团队发布软件时，常靠即时通讯和口头约定协调，提交人、审批人、目标版本和执行结果很难按单据回查。"
        "针对这一问题，本文设计并实现了一套DevOps发布管理系统。"
        "服务端采用Spring Boot、MyBatis与MySQL，浏览器端采用Vue与Element UI。"
        "系统按超级管理员、开发、技术负责人、测试、产品经理和运维划分职责，菜单与写操作均在服务端按角色校验。"
        "发布单按开发、测试、生产环境进入不同审查路径，状态机拒绝非法迁移；审批在同一条更新语句中同时比较状态与行版本号，避免并发覆盖。"
        "自动发布在事务提交后经SSH拉取指定版本，人工发布由运维在页面终端确认结果。"
        "成功后只回写该环境实例的版本并置为在线，失败则将该实例置为离线。"
        "进入测试环境的版本须先在开发环境发布成功，进入生产环境的版本须先在测试环境发布成功；只有失败单据可以回滚到同一环境的上一次成功版本。"
        "功能测试与53个单元测试表明，主要路径可以闭环，非法迁移会被拒绝；对比较更新规则的受控复现中，同一版本号的并发提交只有一次能够写入。"
        "执行线程池与远程超时给出了容量边界。系统范围限于发布编排与受控执行，不包含编译和构建流水线。"
    )
    p = doc.add_paragraph()
    set_pf(p, "justify", 0, 0, first=28)
    add_text(p, body_abs, CN, 14, False)
    mixed(doc, [("关键词：", HEI, 12, True), ("DevOps；发布管理；状态机；乐观锁；自动化部署", CN, 14, False)], first=28)

    doc.add_page_break()
    title_para(doc, "Abstract", outline_level=0, font=EN)
    en_abs = (
        "In small and medium-sized teams, software releases are often coordinated by instant messages and verbal agreement, so the requester, the approver, the target version and the outcome are hard to trace by ticket. "
        "This paper designs and implements a DevOps release management system for that setting. "
        "The server uses Spring Boot, MyBatis and MySQL; the browser uses Vue and Element UI. "
        "Duties are divided among super administrator, developer, technical lead, tester, product manager and operator; menus and write operations are checked by role on the server. "
        "A release order follows different review paths for development, test and production, and a state machine rejects illegal transitions. Approval updates compare the current status and the row version in one statement to avoid concurrent overwrite. "
        "After the transaction commits, automatic release fetches the requested version over SSH; manual release confirms the result in a page terminal. "
        "On success, only that environment instance is marked online and its version is written back; on failure, that instance is marked offline. "
        "A version must succeed in development before it may enter test, and in test before it may enter production. Only a failed order may roll back to the last successful version in the same environment. "
        "Functional tests and 53 unit tests show that the main paths close and illegal transitions are rejected. In a controlled reproduction of the compare-and-set rule, concurrent submissions of the same version allow only one write. "
        "The execution thread pool and remote timeouts define capacity bounds. The system covers release orchestration and controlled execution, not compilation or a build pipeline."
    )
    p = doc.add_paragraph()
    set_pf(p, "justify", 0, 0, first=28)
    add_text(p, en_abs, EN, 14, False)
    mixed(
        doc,
        [("Key words", EN, 14, True), (": ", EN, 14, True),
         ("DevOps; release management; state machine; optimistic locking; automated deployment", EN, 14, False)],
        first=28,
    )

    sec_body = doc.add_section()
    configure_section(sec_body, "decimal", 1)

    h1(doc, "一、绪论")
    h2(doc, "（一）研究背景及意义")
    body(doc, "软件上线是把已经写好的程序放到服务器上，使外部可以访问。对几人到几十人的中小软件团队来说，这一步往往没有独立的运维平台岗位来承接。变更在即时通讯里提出，批准靠口头约定，执行时由熟悉主机的人登录上去操作。聊天记录可以证明“当时说过”，却很难回答四件具体的事：这张变更是谁提交的、目标版本是什么、经过了哪几级审查或验收、执行当时的退出结果是什么。故障发生后，排查常常从翻记录开始，而不是从一份可以打开的发布单开始。")
    body(doc, "把上线收成发布单，并不是把聊天搬家到网页里，而是让后面的审查、验收、审批、执行和回滚共用同一份依据。没有这张单据，开发、测试、产品和运维之间的交接只能靠记忆。机器数量不多、用SSH就能登录时，缺的不是再买一套集群，而是把“能不能上、谁来上、上完结果如何”写成可以检查的规则。")
    body(doc, "DevOps概括的是从变更提出、审批、执行到结果留痕的过程，不是单指某一件工具。胡俊等把权限、部署和健康巡检放在同一套服务中，并支持一键上线和快速回滚[1]。本课题面向可以用SSH登录的少量主机，只做发布编排和受控执行：不代替编译，不搭建构建流水线，也不把容器集群作为前提。系统要管住三件事：未经该环境应有的审查或验收不能执行；自动路径和人工路径都要留下日志；成功或失败只回写该环境上的应用实例，开始执行时实例标为发布中，成功后改为在线并写回版本，失败则改为离线，不影响其他环境。")

    h2(doc, "（二）国内外研究现状")
    h3(doc, "1.国内研究综述")
    body(doc, "国内相关工作大致分成三类：开发运维一体化平台、可重复的安装发布脚本或流水线、把日常维护和工单收进同一套规范。胡俊等面向混合虚拟化环境设计了一体化服务，包含用户管理、部署管理和状态监视，并支持一键上线和快速回滚[1]。本系统在角色校验、发布单据和探活上与之相近，但通过SSH登录已有主机拉取指定版本，不向容器资源池申请环境。")
    body(doc, "在持续集成和自动化执行方面，张兆晨等用Jenkins和Docker把开发、测试和部署组织进同一条容器化工作流[2]。这类方案适合已经有构建平台、并且有人维护镜像和集群的团队。只有数台主机、没有专人维护流水线时，直接照搬的成本偏高。滕家雨等把重复安装收成可编排的任务和脚本[3]。本系统的自动发布也是脚本，但脚本不在页面上临时拼接，而是由发布单上的应用标识、版本、目录和仓库地址在服务端生成，并限制这些字段的字符，远程退出码不是零则记为失败。")
    body(doc, "在投入和过程方面，关兆雄等强调以较少投入把日常维护收进平台[4]。谢婉娟等依据信息技术基础架构库，把资产和工单放在同一平台，说明过程和责任要先定清楚，再谈工具[5]。本系统按这个顺序组织：先有项目、服务器和应用台账，再有发布单，再有执行日志和告警处置。没有台账，单据不知道发到哪台机器；没有单据，执行就没有审批依据。")
    body(doc, "更大的场景侧重不同。黄伟讨论了高并发下的自动化运维和动态扩容[6]。舒晖按仓库地址和应用类型组织发布，并借助持续集成完成构建[7]。汪源等讨论了云计算环境下的业务自动化部署[8]。这些工作对编译、调度和弹性伸缩着墨较多，对“未经该环境应有的审查或验收就不能执行、执行过程必须能回看、结果必须写回台账”这一层写得较少。本系统就补在这一层：用六类角色和按环境分流的状态机约束上线，用乐观锁处理并发审批，用SSH拉取或页面终端完成执行，用告警和报表观察发布之后的情况。")
    h3(doc, "2.国外研究现状")
    body(doc, "国外在持续集成、持续交付和容器化方面的实践开展较早。国内文献的归纳是：持续集成把构建和测试放进日常开发，容器用来统一运行环境[2]；面向高并发的平台还负责弹性伸缩，扩容以较完整的基础设施为前提[6]。本节依据上述国内期刊对国外实践的介绍，未直接引用外文原文。对没有专职平台岗位的中小团队，先维护持续集成集群或容器资源池，会变成新的发布对象，日常成本并不低。因此本课题不重复实现编译、镜像构建和多集群调度，只实现按环境审批、SSH受控执行、结果回写和事后查询。自动执行会在目标机按仓库地址拉取指定版本，拉取已有代码不等于构建流水线。")

    h2(doc, "（三）主要研究内容")
    body(doc, "围绕上述边界，本文完成一个可以运行、规则可以检查的发布管理系统。主要工作按五条主线展开，分别对应角色权限、资产台账、审批状态机、受控执行和事后观察。")
    h3(doc, "1.角色与权限控制")
    body(doc, "按超级管理员、开发、技术负责人、测试、产品经理和运维划分职责。菜单按角色开放，写操作在服务端再次校验，不能只在页面上把按钮藏起来。超级管理员的角色旁路只用于演示和排障，其他角色不能借用。")
    h3(doc, "2.以应用为枢纽的资产台账")
    body(doc, "以应用为中心组织项目、环境、服务器、应用实例、发布单、执行记录和告警。应用属于某个项目；一个应用在每个环境至多一条实例，实例再挂到该环境的一台服务器上。发布单按目标环境走审查，执行结果只回写对应实例。页面上可以按项目、环境、应用和发布单号逐级查下去，使一次发布的前后记录落在同一条链上。")
    h3(doc, "3.按环境分流的状态机与乐观锁")
    body(doc, "发布单起点由目标服务器所属环境决定，后续跳转由状态机限制。开发环境提交后进入待运维审批；测试环境先待测试验收，通过后再交给运维；生产环境先待技术审查，通过后待产品验收，再进入待运维审批。驳回后进入已驳回。运维批准后进入待执行，只有待执行才能开始执行；发布中只能变为发布成功或发布失败，只有发布失败才能回滚到同一环境的上一次成功版本。进入测试的版本须先在开发环境发布成功，进入生产的版本须先在测试环境发布成功。审批在同一条更新语句中比较状态与行版本号，避免两人同时改同一张单据。")
    h3(doc, "4.自动与人工两条执行路径")
    body(doc, "运维在待执行单据上开始执行，并用单选选择自动发布或人工发布。自动路径经SSH在目标机拉取指定版本并切换当前目录；人工路径由运维在页面终端标记结果。开始执行时只把该环境实例标为发布中，成功后改为在线并写回版本，失败后改为离线，其他环境的版本和状态不动。系统不编译代码，自动发布按仓库地址拉取版本。")
    h3(doc, "5.探活告警与发布报表")
    body(doc, "定时探活检查运行中的主机和在线应用，按规则生成或更新告警，由运维接手并解决。报表按时间范围统计发布成败和告警走势。探活不改变发布单状态，只观察上线之后的情况。")
    body(doc, "后文按系统设计的一般顺序展开。第一章说明背景、文献缺口和本文工作。第二章概述开发环境和所用技术。第三章做可行性、需求、界面和流程分析。第四章给出总体结构、功能划分和数据库设计。第五章按登录、看板、项目、服务器与应用、发布单与执行、告警与报表、人员与安全的顺序说明实现。第六章给出功能测试、可用性抽测和单元测试。第七章归纳结论与不足。")

    h1(doc, "二、系统开发关键技术概述")
    body(doc, "本章说明系统所用的开发环境和关键技术。每一项技术只写它在本系统中承担的职责，具体页面和接口放到第五章。系统采用浏览器加后端服务的结构：前端是Vue单页应用，后端是Spring Boot提供的REST接口，数据保存在MySQL中，自动发布和页面终端通过SSH到达目标主机。")
    h2(doc, "（一）系统开发环境")
    h3(doc, "1.运行环境与端口")
    body(doc, "开发和演示可以在同一台普通电脑上完成。运行环境为JDK 8、Maven、Node.js和MySQL 8。后端使用Spring Boot 2.5.9，监听9090端口；前端使用Vue 2.6和Element UI，开发服务监听8080端口。浏览器通过axios访问后端。健康检查与业务接口共用9090端口，只开放健康检查端点。自动发布和探活还需要一台可以SSH登录的目标主机；没有目标主机时，开发配置可以关闭探活，主路径仍可用演示账号在页面上走完提交、审查或验收、审批等步骤。")
    h3(doc, "2.数据库初始化")
    body(doc, "数据库名为manager，字符集为utf8mb4。全新安装时先创建数据库，再依次执行结构脚本和演示数据脚本。结构脚本建立人员、公告、项目、环境、服务器、应用、应用实例、发布单、发布日志、告警与执行记录等表及外键，演示数据脚本写入六类角色账号、示例项目、服务器、应用实例、发布单和告警。仓库根目录另有一份早期脚手架脚本，会删除已有表，且只包含管理员和公告两张表，新鲜安装不要使用它。已有旧库需要按文档中的升级脚本顺序补齐字段，不要把升级脚本和全新安装脚本混跑。")
    h3(doc, "3.开发与生产配置")
    body(doc, "配置分为开发和生产两套，未指定时默认进入开发配置。开发配置允许本地数据源和占位密钥，并关闭探活，避免没有SSH目标时不断产生告警。生产配置必须通过环境变量注入数据源、令牌签名密钥、凭据加密密钥和跨域来源，不允许把口令写进代码。令牌签名密钥与用户口令分开配置，不能用口令散列充当签名密钥。执行线程池大小、重试次数和超时时间放在配置里，演示时可以把超时调短。")

    h2(doc, "（二）Java语言")
    body(doc, "后端使用Java编写，JDK版本为8，与Spring Boot 2.5配合比较成熟，教学环境中也容易复现。选择Java，是因为发布规则需要写在服务端并用单元测试检查。角色、发布状态、应用状态使用英文枚举，库中保存英文常量，页面再显示为中文，前后端不会各写一套中文状态去比对。状态机类只判断允许转到哪里、哪一类角色可以操作，不访问数据库；服务层在事务里调用状态机，再做带版本号的更新并写流转日志。口令、凭据、上传文件名和主机密钥策略同样写在服务端，绕过页面直接调接口仍会被拒绝。规则是否生效，可以在不启动远程主机的情况下用单元测试检查。")

    h2(doc, "（三）Spring Boot框架")
    body(doc, "后端在Spring Boot 2.5.9上提供REST接口。该框架把依赖注入、声明式事务、定时任务、WebSocket和健康检查组织在一起，减少了重复配置[9]。控制器接收请求后交给业务服务。登录成功后签发令牌，放在请求头中，有效期两小时。拦截器校验令牌，只解决“是谁”；“能不能做”由服务层按角色再判断一次。超级管理员旁路只用于演示和排障。禁用账号不能继续使用尚未过期的令牌进入写操作。")
    body(doc, "写操作使用声明式事务，状态更新和流转日志要一起成功或失败。自动执行在事务提交之后异步进行，超时后断开SSH，不把远程等待放进数据库事务。探活大约每两分钟一次，应用检查晚半分钟。邮件未配置时不发信，也不阻断启动。")

    h2(doc, "（四）MyBatis框架")
    body(doc, "持久层使用MyBatis，映射文件与实体对应，适合需要精确控制SQL的场合。Spring Boot、MyBatis、Vue和MySQL这一组合已经用于前后端分离的数据读写[10]。本系统的审批、审查、验收和开始执行都依赖同一类比较更新：更新语句同时带上当前状态和当前版本号，只有两者都没被别人改过，才写入新状态并使版本号加一。影响行数为零就视为冲突，页面提示刷新后再操作。")
    body(doc, "针对这条语句的测试不连接数据库，而是读取映射文件，确认条件没有漏写状态或版本号。列表查询使用分页插件，普通查询带上逻辑删除条件，已删除记录不出现在业务列表中。字段以下划线命名，与Java属性的驼峰命名由MyBatis转换。")

    h2(doc, "（五）Vue框架")
    body(doc, "前端使用Vue 2.6和Element UI，路由使用Vue Router，数据请求使用axios，统计图使用ECharts。页面与接口按统一的状态码、提示信息和数据交换[11]。页面把英文状态显示成中文并按状态给出按钮，服务端仍保存英文常量并再次校验角色[12]。侧边栏分为发布看板、项目资产、发布管控和团队设置四组。发布看板对已登录用户可见；业务项目对超级管理员、开发和技术负责人开放；应用服务对超级管理员、开发和运维开放；部署环境、发布告警和发布报表只对超级管理员和运维开放；发布单对参与流程的六类角色开放；团队设置只对超级管理员开放。开发环境下浏览器直接访问9090端口，由后端放行跨域；生产环境按当前主机名访问后端，避免把某一台机器的地址写进打包结果。人工发布时，页面经WebSocket把终端接到该发布单对应的目标机，必须带登录令牌和发布单编号，角色必须是运维或超级管理员。")

    h2(doc, "（六）MySQL与SSH相关技术")
    h3(doc, "1.MySQL")
    body(doc, "数据存放在MySQL 8中，字符集为utf8mb4。业务表含主键、创建时间、更新时间和逻辑删除标记。环境表保存开发、测试、生产三种编码。应用必须属于一个项目；应用实例按应用加环境唯一，并挂到该环境的一台服务器上。发布单必须指向应用和目标服务器，同时写入环境编码。执行记录和流转记录都必须属于一张发布单。开发负责人、申请人、审批人、操作人、告警处理人和公告发布人保存人员用户名，并由外键指向人员表。告警的发布单号可空，有值时必须是已有发布单。删除使用限制方式，仍被引用的记录不能直接物理删除。发布单号、告警编号、人员用户名、项目编号、应用编号、环境编码、主机名和IP均唯一。")
    h3(doc, "2.SSH与远程执行")
    body(doc, "远程连接使用sshj库。保存服务器时，系统用登记口令登录一次，在目标机写入公钥，私钥以密文保存在本地，认证方式改为密钥，之后探活和自动发布使用密钥登录。口令和私钥在页面上不再明文回显。主机密钥默认按首次信任处理：第一次连接记录指纹，以后指纹变化则拒绝，避免连到被替换的主机。生产环境可以改为只信任已经登记的主机。")
    body(doc, "自动发布脚本在服务端填入应用标识、版本、目录和仓库地址，并限制这些字段的字符，不在页面上拼接命令。版本目录已存在则检出该版本，否则浅克隆后再切换当前链接。回滚只切换已有目录上的链接。远程退出码不是零时记为发布失败，并把该环境实例改为离线。人工发布不跑这段脚本，由运维在页面终端标记结果。脚本细节见第五章。")

    h1(doc, "三、系统分析")
    body(doc, "本章从可行性、需求、界面和流程四个方面分析系统。分析的对象是已经确定的边界：系统把一次上线收成一张发布单，按环境走审查或验收，由运维审批后执行，并用台账和告警观察结果。不分析编译流水线、容器调度和灰度发布。")
    h2(doc, "（一）系统可行性分析")
    h3(doc, "1.技术可行性")
    body(doc, "技术上要回答的不是“框架能不能搭起来”，而是“规则会不会只写在页面上”。状态判断、版本比较、角色校验、脚本字符检查和主机密钥校验都放在服务端，并有对应单元测试。Java、Spring Boot、MyBatis、Vue、MySQL和sshj都有现成类库，教学环境中不购买商业软件也可以完成主路径。没有远程主机时，可用状态流转和单元测试代替自动发布，答辩时说明环境依赖即可。开发配置可以关闭探活，一台普通电脑可同时运行数据库、后端和前端。")
    h3(doc, "2.经济可行性")
    body(doc, "框架、数据库和前端库都是开源软件，开发和演示不产生授权费用。运行只需一台普通电脑；自动路径再增加一台可用SSH登录的主机。对中小团队而言，能节省的是事后核对的时间：发布单号、审批人、目标版本、退出码和告警处置可以按单号查到。不引入商业运维平台，也不要求先建设持续集成集群。")
    h3(doc, "3.操作可行性")
    body(doc, "六类角色看到的菜单不同。发布单上的按钮随状态变化：待运维审批显示审批，待执行显示开始部署，发布失败显示回滚，已驳回不能再执行。使用者不必记住状态机的全部边，但页面给出的操作必须是服务端允许的那一些。登录页可以按角色预填演示账号，便于按开发、测试、产品、运维的顺序走完一张单据。口令和私钥保存后不再回显。")

    h2(doc, "（二）系统需求分析")
    h3(doc, "1.功能性需求分析")
    body(doc, "系统把一次发布收成一张发布单。功能按五条线划分，后面的用例和流程都围绕这五条线展开。")
    body(doc, "资产台账维护项目、环境、服务器、应用和应用实例。项目记录名称、编号、技术栈和开发负责人；服务器属于某一环境，并记录SSH参数和发布根目录；应用必须属于项目，再按环境登记实例并挂到服务器。提交发布单时必须已填写仓库地址，没有仓库地址不能提交；进入测试或生产的版本还须满足前一环境已成功的约束。发布管理负责提交、审查、验收、审批、执行和回滚：开发人员提交申请；技术负责人审查生产环境；测试人员验收测试环境；产品经理验收已通过技术审查的生产单据；运维批准后进入待执行，开始执行时选择自动发布或人工发布。只有发布失败的单据可以回滚到同一环境的上一次成功版本。执行记录保存退出码、错误摘要和日志。告警支持接手和解决。报表按时间范围统计成败和告警走势。")
    body(doc, "六类角色的职责不能互相代替，划分如图1、图2所示。超级管理员维护团队成员和发布通知，可以查看全部发布，接口层旁路只用于演示和排障。开发人员维护项目和应用资料，提交发布申请，查看本人提交的单据以及自己担任开发负责人的项目单据，不能改服务器凭据，也不能调用服务器写接口。技术负责人审查生产环境的发布，并从开发人员中指定项目的开发负责人。测试人员验收测试环境的发布，不能批准待运维审批的单据。产品经理验收生产环境中技术审查已通过的发布。运维人员登记服务器，审批待运维审批的单据，开始执行时选择自动发布或人工发布，发起回滚，并接手、解决告警。")
    figure_img(doc, 1, "管理员与开发人员用例图")
    figure_img(doc, 2, "审查验收与运维用例图")
    body(doc, "状态跳转必须满足状态机，详细路径见图4。提交时必须选择应用、填写目标版本和原因，并且应用已经配置仓库地址。开发人员只能修改或删除自己提交、且仍处于待技术审查的单据；开发环境和测试环境提交后不能再改。开发人员可见本人单据以及担任开发负责人的项目单据；技术负责人、测试和产品经理默认只看待自己处理状态的单据；运维只看待运维审批及之后的单据。被逻辑删除的数据不再出现在普通列表中。")
    h3(doc, "2.非功能性需求分析")
    body(doc, "可用性：菜单和按钮按角色、按状态给出，避免点下去才报错。看板数字可以带到列表的筛选条件里。登录失败、令牌失效、版本冲突和非法跳转都给出明确提示。")
    body(doc, "可靠性：非法状态跳转由服务端拒绝。两人同时审批同一张单据时，后写入的一方因版本号不一致失败，页面提示刷新。同一应用在同一环境不能同时有两张处于发布中的单据，开始执行或回滚时会拒绝。开始执行时只把该环境实例标为发布中，成功才改为在线并写回版本，失败改为离线，避免失败后该环境仍显示在线。")
    body(doc, "安全性：口令和私钥加密存放，页面不回显。令牌放在请求头中，签名密钥与用户口令分开。注册不能选择超级管理员。上传文件限制类型和文件名。主机密钥默认按首次信任记录指纹，以后变化则拒绝。系统放在可信网络中使用，不是对公网开放的多租户平台。")
    body(doc, "性能面向单个团队，而不是高并发互联网业务。列表分页，报表在服务端汇总，探活按分钟级间隔进行，同一问题在冷却期内不重复新建告警。自动执行使用独立线程池，超时后断开远程连接。")
    body(doc, "范围上明确不做三件事：不编译代码，自动发布按仓库地址在目标机拉取版本；不做灰度；待审查、待验收或待运维审批的多张单据可以并存，系统不在这一阶段自动排队。使用者需要具备基本的Linux和SSH知识，才能完成自动路径的演示。")

    h2(doc, "（三）系统界面分析")
    h3(doc, "1.输出设计")
    body(doc, "输出以列表、中文状态、待办数字和统计图为主。发布看板给出资产数量、待办和告警概况，数字可以跳到对应列表并带上筛选条件。发布单列表显示中文状态，并只给出当前状态允许的按钮。执行失败时，执行记录里有退出码和错误摘要。报表按七天、三十天和九十天看成败曲线，汇总使用完成时间和终态，不把还在发布中的单据算进成功或失败。")
    h3(doc, "2.输入设计")
    body(doc, "能枚举的字段用下拉或单选，减少自由输入。服务器所属环境为开发、测试或生产。发布类型不由申请人选择：新建单据固定为全量，回滚时由系统改为回滚；库表仍保留增量取值，当前页面不提供该选项，演示库历史单据中仍可见增量。执行方式在运维开始执行时用单选选择，页面称为自动发布和人工发布。端口必须是数字。发布单必须选择应用并填写目标版本和原因；选择应用后带出该环境实例的目标服务器，可以改选，审批路径按所选服务器环境确定。应用必须已有仓库地址，系统生成发布单号，后续审查、验收、审批和执行都基于这张单据。")
    body(doc, "服务器口令保存后不再回显，私钥不以明文出现在列表中。注册只开放开发和运维，不能选择超级管理员。目标版本由申请人填写，系统不从仓库自动列举标签；填错版本会在目标机执行时失败，这是执行失败，不是审批失败。人工发布打开页面终端前，必须已经登录并且单据处于发布中。")

    h2(doc, "（四）系统流程和逻辑")
    h3(doc, "1.登录流程")
    body(doc, "登录过程如图3所示。登录页先选择当前身份，用于预填演示账号并在左侧标出该角色负责的环节；提交给服务端的是用户名和口令。服务端按用户名读取人员表，校验口令和账号状态，角色以人员表为准。新口令使用BCrypt保存，并兼容库中已有的加盐MD5，避免演示数据被一次性作废。校验通过后签发令牌，进入发布看板；口令错误或账号禁用则提示后重新登录。后续请求携带令牌，失效则回到登录页。令牌签名密钥与口令分开配置；禁用账号不能继续做写操作。")
    figure_img(doc, 3, "登录流程图", 11.5)
    h3(doc, "2.发布单流转")
    body(doc, "发布单的起点和后续跳转如图4所示。开发、测试、生产三条路径由不同岗位接力，是为了避免所有单据都由同一个人批准。开发环境变化频繁，提交后直接进入待运维审批；测试环境先由测试人员验收，通过后再交给运维；生产环境先由技术负责人审查，再由产品经理验收，最后才由运维审批并执行。进入测试的版本须先在开发环境发布成功，进入生产的版本须先在测试环境发布成功。待测试验收、待技术审查、待产品验收和待运维审批被驳回后，都进入已驳回。运维批准后进入待执行，开始执行时选择自动发布或人工发布；发布中只能变为发布成功或发布失败；只有发布失败的单据可以回滚到同一环境的上一次成功版本，回滚再次进入发布中，类型改为回滚。")
    figure_img(doc, 4, "发布单状态流转图", 15.5)
    body(doc, "图4从状态角度说明单据如何跳转；角色之间如何按时间交接，如图5所示。开发人员提交后，系统按目标环境分别通知运维、测试，或技术负责人与产品经理；任一环节驳回则回到开发人员并结束。运维审批通过时在同一条语句中比较状态与行版本号，单据进入待执行。开始执行时选择自动发布或人工发布：自动路径在事务提交后经SSH拉取版本并回写该环境实例；人工路径由运维在页面终端确认结果。发布失败且同环境有历史成功版本时，可以回滚后再次进入发布中。")
    figure_img(doc, 5, "发布流程时序图", 15.5)
    h3(doc, "3.台账回写与并发")
    body(doc, "发布单上的目标版本和乐观锁版本号不是同一个字段。目标版本成功后只回写到该环境的应用实例；乐观锁版本号从零开始，每次合法的状态变更加一。两人同时审批时，后写入的一方因为版本号不一致而失败，需要刷新后再看。开始执行时实例只标为发布中，成功才写回目标版本并改为在线，失败改为离线，其他环境的实例不动。回滚成功时写回的是同一环境的上一次成功版本。")

    h1(doc, "四、系统概要设计")
    body(doc, "本章给出系统的总体结构、功能划分和数据库设计。实现细节放到第五章，测试放到第六章。概要设计要固定三件事：请求经过哪些层、菜单按什么模块划分、数据如何以应用为枢纽串起来。")
    h2(doc, "（一）概述")
    body(doc, "系统采用浏览器加后端服务的结构，工作原理如图6所示。展示层是运行在8080端口的Vue单页应用，包含登录、看板、资产、发布单、告警和报表。请求先进入接入层，完成令牌拦截、跨域处理和统一返回。业务层校验角色、执行状态机、比较版本号，并负责SSH自动执行、页面终端和定时探活。持久层用MyBatis完成映射与分页，数据保存在MySQL 8的manager库中。自动路径从业务层连到目标机SSH，人工路径由页面终端经业务层再连到同一台主机。前后端以JSON交换数据。")
    figure_img(doc, 6, "系统工作原理图")
    body(doc, "一次发布在库中的变化可以按时间看。提交后增加一张发布单和一条提交日志，起始状态由目标服务器所属环境决定，发布类型固定为全量；若目标是测试或生产，还要先核对前一环境是否已有该版本的成功记录。审查、验收和批准都会使乐观锁版本号加一，并追加流转日志。开始执行时写入自动发布或人工发布，单据和该环境实例变为发布中并插入执行记录。自动路径在事务提交后异步SSH，成功则发布单为发布成功、实例为在线并写回版本，失败则发布单为发布失败、实例为离线。人工路径终态由运维标记。回滚把类型改为回滚，再次进入发布中。")

    h2(doc, "（二）系统结构")
    h3(doc, "1.分层结构")
    body(doc, "分层与图6一致，职责不交叉。展示层只负责展示中文状态和按角色、按状态给出按钮，不在浏览器里判断状态机是否允许跳转。接入层只解决“是谁”和跨域。业务层集中存放角色校验、状态机、乐观锁、脚本生成和探活。持久层不包含业务判断，只按条件更新和查询。自动SSH和页面终端都从业务层出去，避免页面直接持有主机口令。")
    h3(doc, "2.功能模块")
    body(doc, "前端按四个菜单分组组织，功能结构如图7所示。发布看板对已登录用户可见，给出待办数字并可下钻到列表。项目资产包含业务项目、应用服务和部署环境：业务项目对超级管理员、开发和技术负责人开放；应用服务对超级管理员、开发和运维开放；部署环境只对超级管理员和运维开放。发布管控包含发布单、发布告警和发布报表：发布单对参与流程的角色可见，告警和报表只对超级管理员和运维开放。团队设置包含团队成员和发布通知，只对超级管理员开放。各模块通过应用编号和发布单号联系。")
    figure_img(doc, 7, "功能结构图", 15.0)

    h2(doc, "（三）数据库设计")
    h3(doc, "1.数据库实体")
    body(doc, "主要实体包括人员、公告、项目、环境、服务器、应用、应用实例、发布单、执行记录、发布日志和告警，联系如图8所示。一个项目包含多个应用；开发、测试、生产三种环境各自挂多台服务器。一个应用在每个环境至多一条实例，实例再挂到该环境的一台服务器上。一张发布单必须指定应用和目标服务器，并写入环境编码，同时对应多条执行记录和多条流转记录。人员分别作为开发负责人、申请人、审批人、操作人、告警处理人和公告发布人被引用；尚未分配、尚未审批或由系统自动写入时，对应外键为空。告警通过可空外键指向应用、服务器和发布单。删除仍被引用的记录时由外键限制挡住；业务删除使用逻辑删除。")
    figure_img(doc, 8, "系统E-R图")
    body(doc, "发布单、应用和人员是三条主业务上最常被查询的实体，属性如图9、图10和图11所示。发布单需要同时保存目标版本、环境编码和乐观锁版本号，以及执行方式和回滚版本，状态与页面九种中文名称对应。应用保存所属项目和仓库地址；各环境的当前版本与在线状态保存在应用实例上。人员保存登录名、角色和账号状态，角色用于菜单与写权限校验，账号状态控制能否登录。")
    figure_img(doc, 9, "发布单实体属性图", 12.0)
    figure_img(doc, 10, "应用实体属性图", 12.0)
    figure_img(doc, 11, "人员实体属性图", 12.0)

    h3(doc, "2.数据库设计表")
    body(doc, "当前发布路径使用人员、公告、项目、环境、服务器、应用、应用实例、发布单、发布日志、告警、执行记录十一张表，按此顺序给出主要字段。各表均含主键id、创建时间create_time、更新时间update_time和逻辑删除标记deleted。状态在库中使用英文常量，页面显示为中文。")
    tw = [3.5, 3.5, 2.6, 5.4]
    th = ["字段名称", "数据类型", "约束", "说明"]
    table_title(doc, "表1  人员表（person）")
    add_data_table(doc, th, [
        ["id", "INT", "主键自增", "人员编号"],
        ["username", "VARCHAR(50)", "非空唯一", "用户名"],
        ["password", "VARCHAR(100)", "可空", "BCrypt，兼容历史MD5"],
        ["name", "VARCHAR(50)", "可空", "姓名"],
        ["phone", "VARCHAR(20)", "可空", "手机号"],
        ["email", "VARCHAR(100)", "可空", "邮箱"],
        ["avatar", "VARCHAR(255)", "可空", "头像"],
        ["role", "VARCHAR(20)", "非空", "账号角色"],
        ["status", "VARCHAR(20)", "可空", "正常或禁用"],
        ["create_time", "DATETIME", "可空", "创建时间"],
        ["update_time", "DATETIME", "可空", "更新时间"],
        ["deleted", "TINYINT", "可空", "逻辑删除"],
    ], tw)
    body(doc, "人员表用role保存账号角色，禁用后不能继续登录。用户名被项目、发布单、流转记录、告警和公告引用。")
    table_title(doc, "表2  公告表（notice）")
    add_data_table(doc, th, [
        ["id", "INT", "主键自增", "公告编号"],
        ["title", "VARCHAR(200)", "非空", "标题"],
        ["content", "VARCHAR(1000)", "可空", "内容"],
        ["time", "VARCHAR(50)", "可空", "展示用创建时间"],
        ["user", "VARCHAR(50)", "外键非空", "发布人用户名"],
        ["create_time", "DATETIME", "可空", "创建时间"],
        ["update_time", "DATETIME", "可空", "更新时间"],
        ["deleted", "TINYINT", "可空", "逻辑删除"],
    ], tw)
    body(doc, "公告表虽有逻辑删除列，当前删除接口按主键物理删除，与发布链路各表的逻辑删除不同。")
    table_title(doc, "表3  项目表（project）")
    add_data_table(doc, th, [
        ["id", "INT", "主键自增", "内部编号"],
        ["name", "VARCHAR(100)", "非空", "项目名"],
        ["code", "VARCHAR(50)", "非空唯一", "项目编号"],
        ["tech_stack", "VARCHAR(200)", "可空", "技术栈"],
        ["leader", "VARCHAR(50)", "外键可空", "开发负责人用户名"],
        ["status", "VARCHAR(20)", "可空", "未开始、进行中、已暂停、已完结"],
        ["start_date", "DATE", "可空", "开始日期"],
        ["end_date", "DATE", "可空", "结束日期"],
        ["description", "VARCHAR(500)", "可空", "项目描述"],
        ["create_time", "DATETIME", "可空", "创建时间"],
        ["update_time", "DATETIME", "可空", "更新时间"],
        ["deleted", "TINYINT", "可空", "逻辑删除"],
    ], tw)
    table_title(doc, "表4  环境表（environment）")
    add_data_table(doc, th, [
        ["id", "INT", "主键自增", "环境编号"],
        ["code", "VARCHAR(20)", "非空唯一", "DEV、TEST或PROD"],
        ["name", "VARCHAR(50)", "非空", "环境名称"],
        ["sort_no", "INT", "非空", "排序"],
        ["remark", "VARCHAR(255)", "可空", "说明"],
        ["create_time", "DATETIME", "可空", "创建时间"],
        ["update_time", "DATETIME", "可空", "更新时间"],
        ["deleted", "TINYINT", "可空", "逻辑删除"],
    ], tw)
    body(doc, "环境编码被服务器、应用实例和发布单引用，决定审查路径和版本晋级方向。")
    table_title(doc, "表5  服务器表（server）")
    add_data_table(doc, th, [
        ["id", "INT", "主键自增", "服务器编号"],
        ["hostname", "VARCHAR(50)", "非空唯一", "主机名"],
        ["ip", "VARCHAR(50)", "非空唯一", "IP地址"],
        ["os", "VARCHAR(50)", "可空", "操作系统"],
        ["cpu", "VARCHAR(20)", "可空", "CPU核数"],
        ["memory", "VARCHAR(20)", "可空", "内存"],
        ["disk", "VARCHAR(20)", "可空", "磁盘"],
        ["env", "VARCHAR(20)", "外键可空", "所属环境编码"],
        ["status", "VARCHAR(20)", "可空", "运行中、已停机、维护中"],
        ["remark", "VARCHAR(255)", "可空", "备注"],
        ["ssh_port", "INT", "非空", "SSH端口"],
        ["ssh_user", "VARCHAR(64)", "可空", "SSH登录用户名"],
        ["ssh_auth_type", "VARCHAR(16)", "非空", "口令或密钥"],
        ["ssh_password", "VARCHAR(512)", "可空", "SSH口令密文"],
        ["ssh_private_key", "TEXT", "可空", "SSH私钥密文"],
        ["deploy_path", "VARCHAR(256)", "可空", "发布根目录"],
        ["create_time", "DATETIME", "可空", "创建时间"],
        ["update_time", "DATETIME", "可空", "更新时间"],
        ["deleted", "TINYINT", "可空", "逻辑删除"],
    ], tw)
    body(doc, "一台服务器只属于一个环境。认证方式在生成登录密钥后改为密钥。发布根目录下按版本建立目录，当前链接指向正在使用的版本。")
    table_title(doc, "表6  应用表（app）")
    add_data_table(doc, th, [
        ["id", "INT", "主键自增", "内部编号"],
        ["name", "VARCHAR(100)", "非空", "应用名"],
        ["code", "VARCHAR(50)", "非空唯一", "应用编号"],
        ["project_id", "INT", "外键非空", "所属项目"],
        ["tech_stack", "VARCHAR(200)", "可空", "技术栈"],
        ["repo_url", "VARCHAR(255)", "可空", "代码仓库地址"],
        ["create_time", "DATETIME", "可空", "创建时间"],
        ["update_time", "DATETIME", "可空", "更新时间"],
        ["deleted", "TINYINT", "可空", "逻辑删除"],
    ], tw)
    body(doc, "应用本身不再保存服务器、端口、版本和在线状态，这些字段下沉到应用实例。")
    table_title(doc, "表7  应用实例表（app_instance）")
    add_data_table(doc, th, [
        ["id", "INT", "主键自增", "实例编号"],
        ["app_id", "INT", "外键非空", "应用"],
        ["env_code", "VARCHAR(20)", "外键非空", "环境编码"],
        ["server_id", "INT", "外键非空", "该环境目标服务器"],
        ["port", "INT", "可空", "端口"],
        ["version", "VARCHAR(50)", "可空", "该环境当前版本"],
        ["status", "VARCHAR(20)", "可空", "在线、发布中、离线"],
        ["create_time", "DATETIME", "可空", "创建时间"],
        ["update_time", "DATETIME", "可空", "更新时间"],
        ["deleted", "TINYINT", "可空", "逻辑删除"],
    ], tw)
    body(doc, "同一应用在同一环境只能有一条实例。发布成功或失败只改这一条实例，不影响其他环境。")
    table_title(doc, "表8  发布单表（deploy）")
    add_data_table(doc, th, [
        ["id", "INT", "主键自增", "发布单内部编号"],
        ["deploy_no", "VARCHAR(50)", "非空唯一", "发布单号"],
        ["app_id", "INT", "外键非空", "应用"],
        ["server_id", "INT", "外键非空", "目标服务器"],
        ["env_code", "VARCHAR(20)", "外键可空", "目标环境编码"],
        ["version", "VARCHAR(50)", "可空", "目标版本"],
        ["deploy_type", "VARCHAR(20)", "可空", "新建为全量，回滚时改为回滚"],
        ["execute_mode", "VARCHAR(16)", "非空默认AUTO", "开始执行时改为自动或人工"],
        ["status", "VARCHAR(20)", "可空", "九种发布状态"],
        ["applicant", "VARCHAR(50)", "外键非空", "申请人用户名"],
        ["approver", "VARCHAR(50)", "外键可空", "审批人用户名"],
        ["apply_time", "DATETIME", "可空", "申请时间"],
        ["approve_time", "DATETIME", "可空", "审批时间"],
        ["finish_time", "DATETIME", "可空", "完成时间"],
        ["reason", "VARCHAR(255)", "可空", "申请原因"],
        ["remark", "VARCHAR(255)", "可空", "备注"],
        ["rollback_version", "VARCHAR(50)", "可空", "回滚目标版本"],
        ["row_version", "INT", "非空", "乐观锁版本"],
        ["create_time", "DATETIME", "可空", "创建时间"],
        ["update_time", "DATETIME", "可空", "更新时间"],
        ["deleted", "TINYINT", "可空", "逻辑删除"],
    ], tw)
    body(doc, "version是要发布的目标版本，row_version是乐观锁。env_code在提交时按目标服务器写入。审批类更新必须同时比较当前状态和row_version。")
    table_title(doc, "表9  发布日志表（deploy_log）")
    add_data_table(doc, th, [
        ["id", "INT", "主键自增", "日志编号"],
        ["deploy_id", "INT", "外键非空", "所属发布单"],
        ["action", "VARCHAR(50)", "可空", "提交、审批、执行等动作"],
        ["operator", "VARCHAR(50)", "外键可空", "操作人用户名，自动动作为空"],
        ["detail", "VARCHAR(1024)", "可空", "操作说明或执行输出"],
        ["log_type", "VARCHAR(16)", "非空", "状态流转或执行输出"],
        ["seq", "INT", "非空", "执行输出行序，流转固定0"],
        ["source", "VARCHAR(16)", "非空", "人工或系统自动"],
        ["create_time", "DATETIME", "可空", "创建时间"],
        ["update_time", "DATETIME", "可空", "更新时间"],
        ["deleted", "TINYINT", "可空", "逻辑删除"],
    ], tw)
    table_title(doc, "表10  告警表（alarm）")
    add_data_table(doc, th, [
        ["id", "INT", "主键自增", "告警内部编号"],
        ["alarm_no", "VARCHAR(50)", "非空唯一", "告警编号"],
        ["app_id", "INT", "外键可空", "关联应用"],
        ["server_id", "INT", "外键可空", "关联服务器"],
        ["deploy_no", "VARCHAR(50)", "外键可空", "关联发布单号"],
        ["level", "VARCHAR(20)", "可空", "提示、警告、严重"],
        ["content", "VARCHAR(500)", "可空", "告警内容"],
        ["status", "VARCHAR(20)", "可空", "未处理、处理中、已解决"],
        ["handler", "VARCHAR(50)", "外键可空", "处理人用户名"],
        ["handle_time", "DATETIME", "可空", "接手时间"],
        ["resolve_time", "DATETIME", "可空", "解决时间"],
        ["create_time", "DATETIME", "可空", "创建时间"],
        ["update_time", "DATETIME", "可空", "更新时间"],
        ["deleted", "TINYINT", "可空", "逻辑删除"],
    ], tw)
    table_title(doc, "表11  执行记录表（deploy_execution）")
    add_data_table(doc, th, [
        ["id", "INT", "主键自增", "执行记录编号"],
        ["deploy_id", "INT", "外键非空", "所属发布单"],
        ["status", "VARCHAR(16)", "非空", "运行中、成功、失败、超时"],
        ["exit_code", "INT", "可空", "远程脚本退出码"],
        ["retry_count", "INT", "非空", "已重试次数"],
        ["start_time", "DATETIME", "可空", "开始时间"],
        ["end_time", "DATETIME", "可空", "结束时间"],
        ["error_message", "VARCHAR(500)", "可空", "失败原因"],
        ["create_time", "DATETIME", "可空", "创建时间"],
        ["update_time", "DATETIME", "可空", "更新时间"],
        ["deleted", "TINYINT", "可空", "逻辑删除"],
    ], tw)
    body(doc, "发布单九种状态与页面一致：待技术审查、待测试验收、待产品验收、待运维审批、待执行、已驳回、发布中、发布成功和发布失败。库中保存英文常量，其中待执行对应APPROVED。执行记录状态为运行中、成功、失败或超时。告警状态为未处理、处理中和已解决。自动发布按仓库地址在目标机拉取版本。发布类型新建为全量，回滚时改为回滚；增量字段保留但页面不选。执行方式提交时按库默认写入自动发布，运维开始执行时再改写。版本是否允许进入测试或生产，按前一环境是否已有同版本成功记录判断。状态、应用、环境和时间等筛选字段已建索引。历史发布单按逻辑删除保留，仍可按单号查询。")

    h1(doc, "五、系统详细实现")
    body(doc, "本章按页面顺序说明实现要点。界面截图如图12至图19所示，与菜单中的登录、看板、业务项目、部署环境、应用服务、发布单、告警和报表对应。实现以服务端规则为准：页面负责展示和收集输入，状态跳转、角色校验和远程执行都在后端完成。")
    h2(doc, "（一）登录与注册")
    body(doc, "登录界面如图12所示。登录页先选择当前身份，左侧标出该角色在三条路径中负责的环节，并可按角色预填演示账号。服务端按用户名读取人员资料，校验口令和账号状态，新口令使用BCrypt并兼容加盐MD5，通过后签发两小时令牌，放入请求头。注册只开放开发和运维，不能注册成超级管理员，也不能自行注册成技术负责人、测试或产品经理。其余岗位由人员管理页创建。令牌签名密钥来自配置或环境变量，不从用户口令推导。")
    figure_img(doc, 12, "系统登录界面", 14.5)

    h2(doc, "（二）发布看板")
    body(doc, "登录后首先进入发布看板，界面如图13所示。看板对已登录用户可见，按当前角色给出提示，并展示一览数字、资产分布、交付成功率和告警闭环率。待审批部署、未处理告警等数字都可以跳到对应列表并带上筛选条件：页面卡片名称是待审批部署，筛的是待运维审批；未处理告警只显示未处理记录。数字都来自同一批发布单、服务器、应用和告警，不是每个角色各算一套。顶部通知条用于提示当前最需要处理的单据或告警，例如待执行或发布失败。")
    figure_img(doc, 13, "发布看板界面", 14.5)

    h2(doc, "（三）项目管理")
    body(doc, "业务项目界面如图14所示。超级管理员、开发人员和技术负责人可以进入该页。开发维护名称、编号、技术栈和状态；开发负责人由技术负责人从开发人员中指定。项目状态包括未开始、进行中、已暂停和已完结。项目本身不产生发布单，发布单挂在应用上。没有权限的角色直接访问该路由会进入无权限页，服务端写接口也会再次拒绝。")
    figure_img(doc, 14, "业务项目界面", 14.5)

    h2(doc, "（四）服务器与应用管理")
    body(doc, "部署环境界面如图15所示。服务器写操作只允许运维和超级管理员。保存时如果要用口令开通密钥，系统会实际登录一次，在目标机写入公钥，私钥密文保存在本地，认证方式改为密钥；登录失败则不应把认证方式改成密钥。环境字段决定新发布单走哪条审查或验收路径，填错环境会让单据找错人。主机名和IP唯一。口令和私钥在列表中不以明文回显。")
    figure_img(doc, 15, "部署环境界面", 14.5)
    body(doc, "应用服务界面如图16所示。应用由开发或运维维护，必须属于项目，并按环境登记实例、挂到服务器。提交发布单时要求应用已填写仓库地址，否则无法生成拉取脚本。各环境的当前版本和在线状态写在实例上，发布成功后由系统回写，不在页面上随意改成“已经在线”。端口用于探活时在目标机检查本机端口；没有端口时则检查发布根目录下的当前链接是否存在。发布开始时该环境实例为发布中，成功后为在线，失败后为离线。")
    figure_img(doc, 16, "应用服务界面", 14.5)

    h2(doc, "（五）发布单与执行")
    h3(doc, "1.提交与审批")
    body(doc, "发布单界面如图17所示。开发人员选择应用后，目标服务器默认带出该环境实例所挂主机，可以改选；再填写目标版本和原因后提交，不在此时选择执行方式。页面可以请求服务端生成拉取脚本并只读展示，真正执行时仍由服务端按发布单字段组装脚本。服务端校验仓库地址，没有则拒绝；按所选目标服务器写入环境编码和起始状态，发布类型固定为全量，执行方式先按库默认写入自动发布。进入测试的版本须先在开发环境发布成功，进入生产的版本须先在测试环境发布成功，否则拒绝提交。开发环境进入待运维审批，测试环境进入待测试验收，生产环境进入待技术审查。目标版本由申请人填写，系统不从仓库自动列举标签。开发人员只能修改或删除自己提交且仍为待技术审查的单据；开发和测试环境提交后起点不是待技术审查，因此提交后不能再改。")
    body(doc, "技术负责人处理待技术审查，通过后进入待产品验收。测试人员处理待测试验收，通过后进入待运维审批。产品经理处理待产品验收，通过后进入待运维审批。运维批准后进入待执行，驳回后进入已驳回。每一级都经过状态机并比较乐观锁版本号。页面按状态给出按钮：待运维审批显示审批，待执行显示开始部署，发布失败显示回滚。列表范围与第三章一致：开发人员可见本人单据以及担任开发负责人的项目单据，其余角色按自己处理的状态收窄。")
    figure_img(doc, 17, "发布单管理界面", 14.5)
    h3(doc, "2.自动发布与人工发布")
    body(doc, "只有待执行的单据才能开始执行。运维点开始部署时用单选选择自动发布或人工发布，并写入该单据的执行方式，界面如图17（续）所示。同一应用在同一环境若已有另一张发布中的单据，开始执行会被拒绝。开始执行时单据和该环境实例都变为发布中，并插入执行记录，此时还不改实例版本。自动执行在事务提交后异步进行：服务端用发布单上的应用标识、版本、目录和仓库地址组装脚本，经SSH送到目标机。版本目录已存在则检出该版本，否则浅克隆后再切换当前链接。远程退出码不是零则失败，执行记录写入错误摘要，发布单进入发布失败，该环境实例改为离线。服务重启时，仍标记为运行中的执行记录会被回收为失败。")
    figure_img(doc, 17, "开始部署时选择执行方式", 14.5, file_name="图17b.png", caption="图17（续）  开始部署时选择执行方式")
    body(doc, "人工发布不跑这段脚本，只把单据和该环境实例改为发布中。页面终端通过WebSocket连接到后端，再由后端连到该发布单对应的目标机。连接必须带登录令牌和发布单编号，角色必须是运维或超级管理员，不能变成与单据无关的跳板。运维在终端操作后调用完成接口，标记发布成功或发布失败。标记之前，该环境实例不会提前变成在线。")
    body(doc, "为核对真实主机上的仓库拉取效果，实验用应用仓库指向公开仓库 https://github.com/kyuxuan234/learn-git，目标机为 hadoop101，发布根目录为 /opt/module。自动发布与人工发布各走一张单据，列表如图17（续2）所示；自动发布执行日志如图17（续3）所示，人工发布执行日志如图17（续4）所示。拉取完成后，目标机上 current 指向 releases/master，远程地址与当前提交如图17（续5）所示。")
    figure_img(doc, 17, "自动与人工发布单列表", 14.5, file_name="图17d.png", caption="图17（续2）  自动与人工发布单列表")
    figure_img(doc, 17, "自动发布执行日志", 14.5, file_name="图17e.png", caption="图17（续3）  自动发布执行日志")
    figure_img(doc, 17, "人工发布执行日志", 14.5, file_name="图17f.png", caption="图17（续4）  人工发布执行日志")
    figure_img(doc, 17, "目标机仓库拉取效果", 14.5, file_name="图17c.png", caption="图17（续5）  目标机仓库拉取效果")
    h3(doc, "3.回滚")
    body(doc, "只有发布失败的单据可以回滚。回滚把发布类型改为回滚，状态再次进入发布中，该环境实例改为发布中，并沿用该单据已有的执行方式，不再重新选择。回滚目标默认取同一环境上一次成功的版本；没有历史成功版本则不能回滚。若该应用在同一环境已有另一张发布中的单据，回滚会被拒绝。自动回滚只切换已经存在的版本目录上的当前链接，不重新克隆一个不存在的版本。目录不存在时脚本以非零退出码结束，单据仍为发布失败。回滚成功把该环境实例的版本写回为回滚目标，并改为在线。")

    h2(doc, "（六）告警与发布报表")
    h3(doc, "1.告警模块")
    body(doc, "告警界面如图18所示。探活由定时任务触发，不改变发布单状态。开发配置可以关闭探活。服务器大约每两分钟检查一次状态为运行中的主机：凭据缺失或SSH配置不完整记警告，主机不可达记严重，并在该主机近一天的发布单上补一条探活失败日志。应用检查晚半分钟，并且只检查在线的环境实例：有端口则检查本机端口，否则检查当前发布链接。单台失败不影响下一台。")
    body(doc, "同一问题已有未关闭告警时只更新内容；已解决且仍在约三十分钟冷却期内不新建；冷却结束后故障还在，才另开一条。探活恢复只在内容里补充“探活已恢复”，不自动改为已解决。运维人员把告警从未处理改为处理中，再改为已解决。告警列表给出级别、状态、关联应用和接手、解决操作。")
    figure_img(doc, 18, "发布告警界面", 14.5)
    h3(doc, "2.发布报表模块")
    body(doc, "报表界面如图19所示，只对超级管理员和运维开放。看板看近期概况，报表按七天、三十天和九十天看成败统计、失败分布、错误关键字和告警走势。汇总使用完成时间和终态，不把还在发布中的单据算进成功或失败。演示数据的完成时间和告警时间按相对当天写入，使报表在不同日期打开时仍有曲线。")
    figure_img(doc, 19, "发布报表界面", 14.5)

    h2(doc, "（七）人员、公告与安全控制")
    h3(doc, "1.人员与公告")
    body(doc, "团队成员页只对超级管理员开放，用于维护账号的登录名、角色、启用或禁用。禁用后不能继续登录，也不能凭尚未过期的令牌做写操作。公告用来写本次演示先看哪一张单据或哪一条告警，面向已登录用户展示，不承担审批功能。个人信息页和修改密码页供本人使用，不能改别人的口令，也不能在该页切换成其他角色。")
    h3(doc, "2.安全控制")
    body(doc, "未登录请求被拦截。写操作再次校验角色：开发不能调用服务器写接口，测试不能批准待运维审批的单据。超级管理员旁路只用于演示和排障。上传文件限制大小、扩展名和文件头，存储名由服务端生成，避免路径穿越。生产环境缺少外部注入的令牌密钥或凭据密钥时给出提示。系统应放在可信网络中使用，不是对公网开放的多租户平台。")

    h1(doc, "六、系统测试")
    body(doc, "测试用来核对实现是否与第三、四、五章的规则一致，而不是把没有实现的灰度或构建流水线写成已经完成。本章先说明目的和环境，再给出功能测试、可用性抽测和单元测试，最后分析哪些结论已经站得住，哪些仍依赖真实主机。")
    h2(doc, "（一）测试目的")
    body(doc, "测试要确认三件事。第一，状态机、版本比较、角色校验和凭据处理是否写在服务端。第二，一张发布单能否按环境走完提交、审查或验收、审批、执行或回滚。第三，哪些结论来自单元测试，哪些必须有真实SSH环境和仓库权限。不把没有目标主机时的演示写成已经完成一次远程发布。")
    h2(doc, "（二）测试环境与方法")
    h3(doc, "1.测试环境")
    body(doc, "测试在Windows上完成。运行环境为JDK 8、Maven、Node.js和MySQL 8，库名为manager。后端监听9090端口，前端开发服务监听8080端口。浏览器打开本地前端地址，使用演示账号：超级管理员、开发、技术负责人、测试、产品经理和运维，按角色切换走完同一张单据的不同环节。开发配置关闭探活，避免没有SSH目标时不断产生告警。单元测试不启动前端，也不要求MySQL中已有演示数据以外的远程主机。")
    h3(doc, "2.测试方法")
    body(doc, "单元测试在后端模块用Maven执行，用替身代替数据库和SSH，检查状态机、比较更新语句、主机密钥策略、凭据加密、口令兼容和上传文件名。功能测试在浏览器中按角色切换演示账号，沿三条环境路径各走一张单据。可用性抽测检查菜单、按钮和敏感信息是否按设计出现或隐藏，并补一次越权接口调用，确认藏掉按钮之后服务端仍然拒绝。系统未做大规模压力测试，也没有自动化的端到端脚本。")
    h2(doc, "（三）功能测试")
    body(doc, "发布闭环的测试用例如表12所示。F-1到F-3区分三种环境的起点，并核对进入测试或生产前的版本晋级约束。F-4和F-5检查运维批准与驳回，批准后进入待执行且版本号加一。F-6和F-8依赖可登录的目标主机；没有远程环境时，以状态流转和单元测试作为替代证据。F-7检查人工路径。F-9用过期版本号再提交，应提示刷新。F-10用开发人员调用服务器写接口，应拒绝。F-11走告警接手和解决。F-12把待运维审批直接改为发布成功，状态机应拒绝。")
    table_title(doc, "表12  发布闭环功能测试")
    add_data_table(doc, ["编号", "用例", "操作要点", "预期结果", "结果"], [
        ["F-1", "开发环境提交", "开发提交申请", "进入待运维审批", "通过"],
        ["F-2", "测试环境验收", "版本已在开发成功后，测试通过或驳回", "通过后待运维审批，驳回则已驳回", "通过"],
        ["F-3", "生产环境两级验收", "版本已在测试成功后，技术审查再产品验收", "两级通过后才待运维审批", "通过"],
        ["F-4", "运维审批", "运维批准", "进入待执行，版本号加一", "通过"],
        ["F-5", "运维驳回", "填写原因后驳回", "已驳回且不能执行", "通过"],
        ["F-6", "自动执行", "待执行且仓库可用", "发布中后到达终态", "通过*"],
        ["F-7", "人工发布", "终端操作后标记结果", "发布成功或发布失败", "通过"],
        ["F-8", "失败后回滚", "对发布失败单据回滚到同环境上次成功版本", "再次发布中，类型为回滚", "通过*"],
        ["F-9", "版本冲突", "持过期版本号再提交", "拒绝并提示刷新", "通过"],
        ["F-10", "越权写入", "开发调用服务器写接口", "拒绝", "通过"],
        ["F-11", "告警闭环", "接手后解决", "状态变为已解决", "通过"],
        ["F-12", "非法跳转", "待运维审批直接改为发布成功", "状态机拒绝", "通过"],
    ], [1.6, 2.8, 3.8, 4.6, 2.2])
    table_note(doc, "（1）标有“通过*”的用例，在具备可登录的目标主机和仓库地址时走完整自动路径；不具备远程环境时，以状态流转、执行记录和单元测试作为替代证据，答辩时应说明这一环境依赖。")
    h2(doc, "（四）可用性测试")
    body(doc, "可用性抽测看两层是否一致：页面是否把不允许的操作藏掉，藏掉之后接口是否仍然拒绝。结果如表13所示。开发人员不应看到部署环境维护；运维应看到审批。发布单按钮随状态变化。看板未处理告警可跳到告警列表并筛选。登录页可按角色预填。保存后的SSH口令不以明文回显。直链访问无权限菜单时进入无权限页。")
    table_title(doc, "表13  界面可用性抽测")
    add_data_table(doc, ["编号", "检查项", "预期", "结果"], [
        ["U-1", "菜单按角色隔离", "开发不见服务器维护，运维可见审批", "通过"],
        ["U-2", "按钮随状态变化", "待运维审批显示审批，待执行显示开始部署，发布失败显示回滚", "通过"],
        ["U-3", "看板数字可下钻", "未处理告警跳到告警列表并筛选", "通过"],
        ["U-4", "登录预填", "可按角色进入系统", "通过"],
        ["U-5", "敏感信息不回显", "保存后的SSH口令不以明文显示", "通过"],
        ["U-6", "直链无权限", "无权限角色打开受限路由进入无权限页", "通过"],
    ], [1.8, 4.0, 6.4, 2.8])
    h2(doc, "（五）单元测试")
    body(doc, "单元测试位于后端测试目录，用Maven执行。共11个测试类、53个测试方法，本机全部通过，统计如表14所示。这些测试覆盖状态机迁移、审批比较更新、开始执行时该环境实例只标为发布中、成功写回版本、失败改为离线、SSH主机密钥三档策略、凭据加解密、BCrypt与历史MD5兼容、上传文件名和路径穿越，以及告警、邮件相关行为。它们不代替真实SSH上的一次成功发布。")
    table_title(doc, "表14  单元测试统计")
    add_data_table(doc, ["测试类", "方法数", "关注点", "结果"], [
        ["DeployStatusMachineTest", "7", "合法与非法状态迁移", "通过"],
        ["DeployServiceStatusTest", "11", "审批、实例回写与版本比较", "通过"],
        ["DeployMapperCasApproveXmlTest", "6", "审批语句是否带版本条件", "通过"],
        ["SshHostKeyVerifierTest", "7", "主机密钥校验策略", "通过"],
        ["CredentialCipherTest", "2", "凭据加解密", "通过"],
        ["PasswordUtilsTest", "3", "BCrypt与历史MD5兼容", "通过"],
        ["FileSafeNamesTest", "6", "上传文件名", "通过"],
        ["EnvFileLoaderTest", "2", "环境文件加载", "通过"],
        ["AlarmServiceTest", "1", "告警服务基本行为", "通过"],
        ["AlarmMailServiceTest", "1", "告警邮件", "通过"],
        ["PersonMailServiceTest", "7", "人员邮件", "通过"],
        ["合计", "53", "全部单元测试", "通过"],
    ], [6.2, 1.8, 4.6, 2.4])
    h2(doc, "（六）测试结果分析")
    body(doc, "已经可以反复验证的部分包括：三条环境路径的起点和驳回、运维批准与驳回、非法跳转、版本冲突、越权写入、告警接手和解决，以及表14中的单元测试。主路径可以在本机用演示账号走完提交到审批；人工发布可以在没有远程脚本的情况下标记结果。对比较更新规则的受控复现中，同一版本号的并发提交只有一次能够写入。这些结论不依赖真实SSH主机，可以在答辩前重复执行。")
    body(doc, "自动发布和自动回滚是否真正在目标机切换成功，取决于主机连通、仓库权限和主机密钥。因此表12中标有星号的用例，在没有远程环境时只能证明状态和台账规则正确，不能写成“已经完成一次远程发布”。探活在开发配置中关闭，告警闭环用手工告警验证接手和解决。执行线程池大小与远程超时给出了自动路径的容量边界，本课题不做大规模压测。")
    body(doc, "尚未覆盖的部分也需要写明。同一应用同一环境处于发布中的单据已经互斥，开始执行或回滚时会拒绝第二张；但待审查、待验收或待运维审批的多张单据仍可并存，这一阶段没有排队。没有自动化的浏览器端到端脚本，也没有压力测试。库表升级仍按文档执行脚本。")

    h1(doc, "七、结论")
    h2(doc, "（一）工作总结")
    body(doc, "针对中小团队发布过程缺少可回查单据、审批责任不清的问题，本文设计并实现了一套DevOps发布管理系统。服务端采用Spring Boot、MyBatis与MySQL，浏览器端采用Vue与Element UI；系统按超级管理员、开发、技术负责人、测试、产品经理和运维划分职责，并在服务端校验写操作。业务上以应用为枢纽，经环境实例把项目、服务器、发布单、执行记录和告警串成可追溯的链路。发布单按开发、测试、生产进入不同审查路径，状态机拒绝非法迁移；审批在同一条更新语句中比较状态与行版本号。自动发布经SSH拉取指定版本，人工发布在页面终端确认结果。成功后只回写该环境实例的版本并置为在线，失败则将该实例置为离线；进入测试或生产的版本须先在前一环境成功，只有失败单据可以回滚到同一环境的上一次成功版本。")
    body(doc, "功能测试与53个单元测试表明，主要路径可以闭环，非法迁移会被拒绝；对比较更新规则的受控复现中，同一版本号的并发提交只有一次能够写入。执行线程池与远程超时给出了容量边界。系统范围限于发布编排与受控执行，不包含编译和构建流水线。")
    h2(doc, "（二）不足与展望")
    body(doc, "不足需要写清楚。自动发布和自动回滚依赖真实主机、仓库权限和主机密钥，没有远程环境时不能把状态流转写成已经完成一次远程发布。同一应用同一环境处于发布中的单据已经互斥，但待审查、待验收或待运维审批阶段仍可并存多张单据。测试以单元测试和手工验证为主，缺少自动化的浏览器端到端测试和压力测试。后续可以补上带真实SSH的集成测试，用迁移工具管理表结构，并在待审批阶段的并发单据上加入排队。若团队规模明显扩大，编译和调度应交给专门的持续交付平台。")

    doc.add_page_break()
    title_para(doc, "参考文献", outline_level=0)
    refs = [
        "胡俊,章兵,陈郑平,等.面向混合虚拟化环境的开发运维一体化服务设计[J].微型电脑应用,2022,38(2):60-62+76.",
        "张兆晨,罗铁坚.CCI:一种基于容器化的持续集成系统[J].中国科学院大学学报,2018,35(4):569-575.",
        "滕家雨,张楠,朱广新,等.自动化部署工具设计与实现[J].微型电脑应用,2018,34(12):5-7+17.",
        "关兆雄,郑晓娟.信息化自动运维应用平台建设研究[J].微型电脑应用,2020,36(1):88-90.",
        "谢婉娟,邓国强.智慧校园IT运维服务平台设计与实现[J].微型电脑应用,2022,38(8):4-7.",
        "黄伟.高并发场景下自动化运维方案的设计与实现[J].福建电脑,2021,37(12):111-113.",
        "舒晖.高校智慧校园管理平台的设计与实现[J].微型电脑应用,2021,37(7):56-59.",
        "汪源,刘传昌.云计算环境下业务自动化部署的设计与实现[J].软件,2014,35(9):66-72.",
        "霍福华,韩慧.基于SpringBoot微服务架构下前后端分离的MVVM模型[J].电子技术与软件工程,2022(1):73-76.",
        "郭建卫.基于SpringBoot+Mybatis+Vue的学生德育量化考核系统设计与实现[J].现代信息科技,2023,7(1):18-22.",
        "王锋,刘俊波.前后端分离模式下的WEB系统集成方案[J].通信技术,2020,53(9):2347-2350.",
        "施展,朱彦.基于Vue与SpringBoot框架的学生成绩分析和弱项辅助系统设计[J].信息技术与信息化,2022(8):127-131.",
    ]
    for i, ref in enumerate(refs, 1):
        p = doc.add_paragraph()
        set_pf(p, "left", 0, 3)
        p.paragraph_format.first_line_indent = Cm(-0.74)
        p.paragraph_format.left_indent = Cm(0.74)
        add_text(p, f"[{i}] {ref}", CN, 12, False)

    doc.add_page_break()
    title_para(doc, "致  谢", outline_level=0)
    body(doc, "毕业设计即将结束，回想从选题、实现到写稿的过程，需要感谢的人很多。首先要感谢指导教师。老师在选题范围、系统边界和论文结构上多次提醒：题目必须能讲清楚，系统必须能运行，规则必须能检查，不能把持续集成、容器集群和发布审批全部堆进一个做不完的题目。正是这一边界，使本系统能够把工作收在按环境审批、SSH受控执行和台账回写上。")
    body(doc, "感谢一起学习的同学。在环境搭建、演示数据核对和文档校对时，大家指出了多处容易写混的地方，例如开发、测试、生产三条路径不能写成同一种审批，发布失败后应用不能继续显示为在线，注册不能开放超级管理员。这些提醒直接改到了代码和正文里。")
    body(doc, "也感谢家人在学习期间的理解和支持，使这段时间和精力能够放在开发和写作上。通过这次毕业设计，我比较完整地经历了从需求、设计、实现到测试的过程，对开发和运维之间如何用一张发布单交接有了具体认识。由于水平有限，系统里还有不足，恳请老师批评指正。")

    import re
    import shutil
    import zipfile
    import xml.etree.ElementTree as ET
    tmp = ROOT / "_thesis_out.docx"
    doc.save(str(tmp))
    try:
        shutil.copyfile(tmp, OUT)
        print("WROTE", OUT)
    except PermissionError:
        print("LOCKED, wrote", tmp)
    xml = zipfile.ZipFile(tmp).read("word/document.xml")
    root = ET.fromstring(xml)
    ns = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
    body_el = root.find(ns + "body")
    items = []
    for child in body_el:
        ts = []
        for t in child.iter(ns + "t"):
            if t.text:
                ts.append(t.text)
        items.append((child.tag.endswith("p"), "".join(ts)))
    idxs = [i for i, it in enumerate(items) if it[0] and it[1].strip().startswith("一、绪论")]
    start = idxs[-1]
    end = next(i for i, it in enumerate(items) if i > start and it[0] and it[1].strip() == "参考文献")
    blob = "".join(it[1] for it in items[start:end])
    cjk = len(re.findall(r"[\u4e00-\u9fff]", blob))
    order = []
    for n in re.findall(r"\[(\d+)\]", blob):
        n = int(n)
        if n not in order:
            order.append(n)
    print("BODY_CJK", cjk)
    print("CITE", order)
    print("BLIPS", xml.count(b"a:blip"))
    print("PLACEHOLDERS", re.findall(r"图\d+留空", xml.decode("utf-8")))
    print("CN_ABS", len(re.findall(r"[\u4e00-\u9fff]", body_abs)))
    print("EN_ABS", len(en_abs))


if __name__ == "__main__":
    build()
