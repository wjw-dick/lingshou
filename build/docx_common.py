# -*- coding: utf-8 -*-
"""三份 Word 交付文档的公共排版模块（排版基线来自 deliverables-docs.md §2）。"""

from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_SECTION, WD_ORIENT
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

BODY_FONT_CN = '宋体'
BODY_FONT_EN = 'Times New Roman'
HEAD_FONT_CN = '黑体'


def _set_eastasia(run_or_style_rpr, font_cn):
    rpr = run_or_style_rpr
    rfonts = rpr.find(qn('w:rFonts'))
    if rfonts is None:
        rfonts = OxmlElement('w:rFonts')
        rpr.append(rfonts)
    rfonts.set(qn('w:eastAsia'), font_cn)
    rfonts.set(qn('w:ascii'), BODY_FONT_EN)
    rfonts.set(qn('w:hAnsi'), BODY_FONT_EN)


def new_document():
    doc = Document()
    # 页面：A4，四边 2.5cm
    for sec in doc.sections:
        sec.page_width = Cm(21.0)
        sec.page_height = Cm(29.7)
        sec.top_margin = Cm(2.5)
        sec.bottom_margin = Cm(2.5)
        sec.left_margin = Cm(2.5)
        sec.right_margin = Cm(2.5)

    # Normal：宋体 12pt、1.5 倍行距、首行缩进 2 字符、两端对齐
    normal = doc.styles['Normal']
    normal.font.name = BODY_FONT_EN
    normal.font.size = Pt(12)
    normal.font.color.rgb = RGBColor(0, 0, 0)
    _set_eastasia(normal.element.get_or_add_rPr(), BODY_FONT_CN)
    pf = normal.paragraph_format
    pf.line_spacing = 1.5
    pf.first_line_indent = Pt(24)
    pf.space_before = Pt(0)
    pf.space_after = Pt(0)
    pf.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY

    # 标题样式：黑体、黑色、无缩进
    heading_specs = [
        ('Heading 1', 16, 14, 6),
        ('Heading 2', 14, 11, 5),
        ('Heading 3', 12, 9, 4),
    ]
    for name, size, before, after in heading_specs:
        st = doc.styles[name]
        st.font.name = BODY_FONT_EN
        st.font.size = Pt(size)
        st.font.bold = True
        st.font.color.rgb = RGBColor(0, 0, 0)
        _set_eastasia(st.element.get_or_add_rPr(), HEAD_FONT_CN)
        p = st.paragraph_format
        p.first_line_indent = Pt(0)
        p.left_indent = Pt(0)
        p.space_before = Pt(before)
        p.space_after = Pt(after)
        p.line_spacing = 1.5
        p.keep_with_next = True
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    return doc


def _run_cn(paragraph, text, size=12, bold=False, cn_font=BODY_FONT_CN, en_font=BODY_FONT_EN):
    run = paragraph.add_run(text)
    run.font.name = en_font
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = RGBColor(0, 0, 0)
    _set_eastasia(run._element.get_or_add_rPr(), cn_font)
    return run


def cover(doc, title, subtitle_cn, subtitle_en, version, date_text, org):
    # 主标题：黑体 22pt 加粗居中
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Pt(0)
    p.paragraph_format.space_before = Pt(90)
    p.paragraph_format.space_after = Pt(18)
    _run_cn(p, title, size=22, bold=True, cn_font=HEAD_FONT_CN)

    # 文档类型副标题：黑体 22pt
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Pt(0)
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(12)
    _run_cn(p, subtitle_cn, size=22, bold=True, cn_font=HEAD_FONT_CN)

    # 英文副标题：Times New Roman 14pt
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Pt(0)
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(80)
    _run_cn(p, subtitle_en, size=14, bold=False, cn_font=BODY_FONT_CN)

    for label, value in (('文档版本', version), ('编制日期', date_text), ('编制单位', org)):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.first_line_indent = Pt(0)
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(6)
        _run_cn(p, '%s：%s' % (label, value), size=12)


def h1(doc, text, new_page=True):
    p = doc.add_heading(level=1)
    _run_cn(p, text, size=16, bold=True, cn_font=HEAD_FONT_CN)
    if new_page:
        p.paragraph_format.page_break_before = True
    return p


def h2(doc, text):
    p = doc.add_heading(level=2)
    _run_cn(p, text, size=14, bold=True, cn_font=HEAD_FONT_CN)
    return p


def h3(doc, text):
    p = doc.add_heading(level=3)
    _run_cn(p, text, size=12, bold=True, cn_font=HEAD_FONT_CN)
    return p


def body(doc, text, indent=True):
    p = doc.add_paragraph()
    if not indent:
        p.paragraph_format.first_line_indent = Pt(0)
    _run_cn(p, text, size=12)
    return p


def bullet(doc, text):
    p = doc.add_paragraph(style='List Bullet')
    p.paragraph_format.first_line_indent = Pt(0)
    p.paragraph_format.left_indent = Pt(24)
    p.paragraph_format.line_spacing = 1.5
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    _run_cn(p, text, size=12)
    return p


def numbered(doc, text):
    p = doc.add_paragraph(style='List Number')
    p.paragraph_format.first_line_indent = Pt(0)
    p.paragraph_format.left_indent = Pt(24)
    p.paragraph_format.line_spacing = 1.5
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    _run_cn(p, text, size=12)
    return p


def caption(doc, text, above_table=True):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Pt(0)
    p.paragraph_format.line_spacing = 1.0
    p.paragraph_format.space_before = Pt(4 if not above_table else 6)
    p.paragraph_format.space_after = Pt(2 if above_table else 6)
    p.paragraph_format.keep_with_next = above_table
    _run_cn(p, text, size=10.5)
    return p


def _shade(cell, color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), color)
    tcPr.append(shd)


def _borders(table):
    tblPr = table._tbl.tblPr
    borders = OxmlElement('w:tblBorders')
    for edge in ('top', 'left', 'bottom', 'right', 'insideH', 'insideV'):
        el = OxmlElement('w:' + edge)
        el.set(qn('w:val'), 'single')
        el.set(qn('w:sz'), '4')
        el.set(qn('w:space'), '0')
        el.set(qn('w:color'), '000000')
        borders.append(el)
    tblPr.append(borders)


def _cant_split(row):
    trPr = row._tr.get_or_add_trPr()
    trPr.append(OxmlElement('w:cantSplit'))


def _table_header(row):
    trPr = row._tr.get_or_add_trPr()
    trPr.append(OxmlElement('w:tblHeader'))


def _fill_cell(cell, text, bold=False, align=WD_ALIGN_PARAGRAPH.LEFT, size=10.5):
    cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
    p = cell.paragraphs[0]
    p.paragraph_format.first_line_indent = Pt(0)
    p.paragraph_format.left_indent = Pt(0)
    p.paragraph_format.right_indent = Pt(0)
    p.paragraph_format.line_spacing = 1.0
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    p.alignment = align
    _run_cn(p, str(text).replace('\n', '；'), size=size, bold=bold)


def table(doc, headers, rows, widths=None, caption_text=None, align_first_center=True,
          center_cols=None, right_cols=None):
    """widths 为厘米列表，合计建议 16.0（版心宽）。"""
    if caption_text:
        caption(doc, caption_text, above_table=True)
    t = doc.add_table(rows=1, cols=len(headers))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    _borders(t)
    hdr = t.rows[0]
    _cant_split(hdr)
    _table_header(hdr)
    for i, text in enumerate(headers):
        cell = hdr.cells[i]
        _shade(cell, 'F2F2F2')
        _fill_cell(cell, text, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    if widths:
        for i, w in enumerate(widths):
            for row in t.rows:
                row.cells[i].width = Cm(w)

    for data in rows:
        row = t.add_row()
        _cant_split(row)
        for i, text in enumerate(data):
            if i == 0 and align_first_center:
                align = WD_ALIGN_PARAGRAPH.CENTER
            elif center_cols and i in center_cols:
                align = WD_ALIGN_PARAGRAPH.CENTER
            elif right_cols and i in right_cols:
                align = WD_ALIGN_PARAGRAPH.RIGHT
            else:
                align = WD_ALIGN_PARAGRAPH.LEFT
            _fill_cell(row.cells[i], text, align=align)
        if widths:
            for i, w in enumerate(widths):
                row.cells[i].width = Cm(w)
    return t


def footer_page_number(section, start=None):
    section.footer.is_linked_to_previous = False
    p = section.footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Pt(0)
    run = p.add_run()
    run.font.name = BODY_FONT_EN
    run.font.size = Pt(10.5)
    fld_begin = OxmlElement('w:fldChar')
    fld_begin.set(qn('w:fldCharType'), 'begin')
    instr = OxmlElement('w:instrText')
    instr.set(qn('xml:space'), 'preserve')
    instr.text = 'PAGE'
    fld_end = OxmlElement('w:fldChar')
    fld_end.set(qn('w:fldCharType'), 'end')
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_end)

    if start is not None:
        sectPr = section._sectPr
        pg = sectPr.find(qn('w:pgNumType'))
        if pg is None:
            pg = OxmlElement('w:pgNumType')
            sectPr.append(pg)
        pg.set(qn('w:start'), str(start))


def blank_footer(section):
    section.footer.is_linked_to_previous = False
    p = section.footer.paragraphs[0]
    p.paragraph_format.first_line_indent = Pt(0)
    p.text = ''


def toc(doc):
    p = doc.add_paragraph()
    p.paragraph_format.first_line_indent = Pt(0)
    p.paragraph_format.line_spacing = 1.0
    run = p.add_run()
    fld_begin = OxmlElement('w:fldChar')
    fld_begin.set(qn('w:fldCharType'), 'begin')
    instr = OxmlElement('w:instrText')
    instr.set(qn('xml:space'), 'preserve')
    instr.text = 'TOC \\o "1-2" \\h \\z \\u'
    fld_sep = OxmlElement('w:fldChar')
    fld_sep.set(qn('w:fldCharType'), 'separate')
    placeholder = OxmlElement('w:t')
    placeholder.text = '目录内容将在打开文档后自动更新（或右键选择"更新域"）'
    fld_end = OxmlElement('w:fldChar')
    fld_end.set(qn('w:fldCharType'), 'end')
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_sep)
    run._r.append(placeholder)
    run._r.append(fld_end)


def revision_page(doc, rows):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.first_line_indent = Pt(0)
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(12)
    p.paragraph_format.page_break_before = True
    _run_cn(p, '修订记录', size=16, bold=True, cn_font=HEAD_FONT_CN)
    table(doc, ['版本', '日期', '修订内容'], rows, widths=[3.0, 4.0, 9.0],
          caption_text=None)


def build_front_matter(doc, meta):
    """封面 → 修订记录 → 目录页。返回正文所在 section。"""
    sec1 = doc.sections[0]
    blank_footer(sec1)
    cover(doc, meta['title'], meta['subtitle_cn'], meta['subtitle_en'],
          meta['version'], meta['date'], meta['org'])

    revision_page(doc, meta['revisions'])

    sec2 = doc.add_section(WD_SECTION.NEW_PAGE)
    sec2.page_width = Cm(21.0)
    sec2.page_height = Cm(29.7)
    sec2.top_margin = Cm(2.5)
    sec2.bottom_margin = Cm(2.5)
    sec2.left_margin = Cm(2.5)
    sec2.right_margin = Cm(2.5)
    blank_footer(sec2)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Pt(0)
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(18)
    _run_cn(p, '目　　录', size=16, bold=True, cn_font=HEAD_FONT_CN)
    toc(doc)

    sec3 = doc.add_section(WD_SECTION.NEW_PAGE)
    sec3.page_width = Cm(21.0)
    sec3.page_height = Cm(29.7)
    sec3.top_margin = Cm(2.5)
    sec3.bottom_margin = Cm(2.5)
    sec3.left_margin = Cm(2.5)
    sec3.right_margin = Cm(2.5)
    footer_page_number(sec3, start=1)
    return sec3
