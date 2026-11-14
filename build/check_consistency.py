# -*- coding: utf-8 -*-
"""交付前一致性核对：项目名、端名、技术栈、人天与报价复算。"""

from docx import Document
from pathlib import Path

BASE = Path(r'D:\ai\retail-convenience')
FILES = {
    'SRS': BASE / '零售便利连锁SaaS平台_需求规格说明书_V1.0.docx',
    'TECH': BASE / '零售便利连锁SaaS平台_技术方案与报价_V1.0.docx',
    'MANUAL': BASE / '零售便利连锁SaaS平台_用户使用手册_V1.0.docx',
}

ENDS = ['平台运营端', 'PC 品牌官网', 'PC 租户后台', '微信公众号',
        '小程序用户端', 'H5 分享页', '店长/店员端', '供应商端',
        '加盟商/渠道商端', '独立 App']

TECH_WORDS = ['Java 17', 'Spring Boot 3', 'MyBatis-Plus', 'MySQL 8', 'Redis',
              'RabbitMQ', 'Vue 3', 'Element Plus', 'Flutter', 'MinIO', 'Docker Compose']


def text_of(path):
    doc = Document(str(path))
    parts = [p.text for p in doc.paragraphs]
    for t in doc.tables:
        for row in t.rows:
            for cell in row.cells:
                parts.append(cell.text)
    return '\n'.join(parts)


def main():
    print('=== 文件与体量 ===')
    texts = {}
    for k, p in FILES.items():
        if not p.exists():
            print(k, 'MISSING', p)
            continue
        texts[k] = text_of(p)
        chars = len(texts[k].replace('\n', ''))
        doc = Document(str(p))
        print('%s | %.1f KB | 段落 %d | 表格 %d | 字符 %d'
              % (k, p.stat().st_size / 1024.0, len(doc.paragraphs), len(doc.tables), chars))

    print('\n=== 项目名一致性 ===')
    for k, t in texts.items():
        print(k, '含项目名:', '零售便利连锁SaaS平台' in t)

    print('\n=== 端名一致性 ===')
    for k, t in texts.items():
        missing = [e for e in ENDS if e not in t]
        print(k, '缺端名:', missing if missing else '无')

    print('\n=== 技术栈（技术方案） ===')
    t = texts['TECH']
    print('缺失:', [w for w in TECH_WORDS if w not in t] or '无')
    print('SRS 未出现技术栈关键词（应不含框架）:',
          [w for w in ['Spring Boot', 'MyBatis-Plus', 'RabbitMQ'] if w in texts['SRS']] or '不含')

    print('\n=== 工作量与报价复算 ===')
    for d in (357 * 1500, 357 * 2200):
        print('357 人天 × 单价 =', d)
    print('技术方案含 357:', '357' in texts['TECH'])

    print('\n=== 公众号 11 子页 ===')
    for k in ('SRS', 'MANUAL'):
        print(k, '含“11 个子页”或“11 子页”:',
              ('11 个子页' in texts[k]) or ('11 个子页' in texts[k]) or ('共 11 个子页' in texts[k]))

    print('\n=== 占位符检查 ===')
    for k, t in texts.items():
        print(k, '【单价】:', '【单价】' in t, '| 【编制单位】:', '【编制单位】' in t)

    print('\n=== 表题编号连续性 ===')
    import re
    for k, p in FILES.items():
        doc = Document(str(p))
        caps = [q.text.strip() for q in doc.paragraphs if q.text.strip().startswith('表')]
        nums = []
        for c in caps:
            m = re.match(r'表\s*(\d+)', c)
            if m:
                nums.append(int(m.group(1)))
        print(k, '表题:', [c[:22] for c in caps])
        expect = list(range(1, len(nums) + 1))
        print('  编号连续:', nums == expect, '| 实得:', nums)


if __name__ == '__main__':
    main()
