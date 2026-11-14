# -*- coding: utf-8 -*-
"""Generate MyBatis-Plus entities and mappers from schema.sql."""
import os
import re

ROOT = r"D:\ai\retail-convenience\backend\src\main\java\com\retail\convenience"
SCHEMA = r"D:\ai\retail-convenience\backend\src\main\resources\db\schema.sql"

GROUPS = {
    "platform": "platform",
    "sys": "system",
    "mp": "mp",
    "store": "store",
    "product": "product",
    "brand": "product",
    "purchase": "purchase",
    "supply": "purchase",
    "receive": "purchase",
    "difference": "purchase",
    "stock": "inventory",
    "transfer": "inventory",
    "loss": "inventory",
    "sales": "sales",
    "refund": "sales",
    "pos": "sales",
    "day": "sales",
    "invoice": "sales",
    "order": "sales",
    "member": "member",
    "coupon": "member",
    "promotion": "member",
    "supplier": "supplier",
    "franchise": "franchise",
    "franchisee": "franchise",
    "settlement": "settlement",
    "delivery": "mall",
    "cms": "cms",
    "tenant": "system",
    "feedback": "member",
}

def pascal(name):
    return "".join(p.capitalize() for p in name.split("_"))

def camel(name):
    p = pascal(name)
    return p[0].lower() + p[1:]

def group_of(table):
    for prefix, grp in GROUPS.items():
        if table.startswith(prefix):
            return grp
    return "common"

def java_type(col_type):
    t = col_type.lower()
    if "bigint" in t:
        return "Long"
    if "int" in t:
        return "Integer"
    if "decimal" in t:
        return "BigDecimal"
    if "double" in t or "float" in t:
        return "Double"
    if "datetime" in t or "timestamp" in t:
        return "LocalDateTime"
    if t.startswith("date"):
        return "LocalDate"
    if "time" in t:
        return "LocalTime"
    return "String"

sql = open(SCHEMA, encoding="utf-8").read()
blocks = re.findall(r"CREATE TABLE IF NOT EXISTS (\w+) \((.*?)\n\) ENGINE", sql, re.S)
created = 0
for table, body in blocks:
    cols = []
    pk = None
    for line in body.split("\n"):
        line = line.strip().rstrip(",")
        if not line or line.startswith("KEY") or line.startswith("UNIQUE") or line.startswith("CONSTRAINT") or line.startswith("--"):
            continue
        m = re.match(r"(\w+)\s+([A-Za-z0-9_]+(?:\([^)]*\))?)\s*(.*)", line)
        if not m:
            continue
        name, typ, rest = m.group(1), m.group(2), m.group(3)
        if name.upper() in ("PRIMARY",):
            continue
        cols.append((name, typ, rest))
        if "PRIMARY KEY" in rest.upper():
            pk = name
    if not cols:
        continue
    if pk is None:
        pk = "id" if any(c[0] == "id" for c in cols) else None
    grp = group_of(table)
    pkg = f"modules.{grp}"
    cls = pascal(table)
    base = os.path.join(ROOT, *pkg.split("."), "entity")
    mapper_dir = os.path.join(ROOT, *pkg.split("."), "mapper")
    os.makedirs(base, exist_ok=True)
    os.makedirs(mapper_dir, exist_ok=True)

    lines = [
        f"package com.retail.convenience.{pkg}.entity;",
        "",
        "import com.baomidou.mybatisplus.annotation.*;",
        "import lombok.Data;",
    ]
    types = {java_type(c[1]) for c in cols}
    if "BigDecimal" in types:
        lines.append("import java.math.BigDecimal;")
    if "LocalDateTime" in types:
        lines.append("import java.time.LocalDateTime;")
    if "LocalDate" in types:
        lines.append("import java.time.LocalDate;")
    if "LocalTime" in types:
        lines.append("import java.time.LocalTime;")
    lines += ["", "@Data", f'@TableName("{table}")', f"public class {cls} {{", ""]
    for name, typ, rest in cols:
        jt = java_type(typ)
        field = camel(name)
        ann = []
        if name == pk:
            ann.append("    @TableId(type = IdType.AUTO)")
        if name == "deleted":
            ann.append("    @TableLogic")
        if name in ("created_at",):
            ann.append("    @TableField(fill = FieldFill.INSERT)")
        if name in ("updated_at",):
            ann.append("    @TableField(fill = FieldFill.INSERT_UPDATE)")
        lines += ann
        lines.append(f"    private {jt} {field};")
        lines.append("")
    lines.append("}")
    open(os.path.join(base, cls + ".java"), "w", encoding="utf-8").write("\n".join(lines))

    mapper = [
        f"package com.retail.convenience.{pkg}.mapper;",
        "",
        f"import com.retail.convenience.{pkg}.entity.{cls};",
        "import com.baomidou.mybatisplus.core.mapper.BaseMapper;",
        "import org.apache.ibatis.annotations.Mapper;",
        "",
        "@Mapper",
        f"public interface {cls}Mapper extends BaseMapper<{cls}> {{",
        "}",
    ]
    open(os.path.join(mapper_dir, cls + "Mapper.java"), "w", encoding="utf-8").write("\n".join(mapper))
    created += 1

print("generated", created, "entities")
