from pathlib import Path
from PIL import Image
import json

img_dir = Path(r'C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-master\work\full_paper_revision\extracted_imgs')
keys = [
  '01_图_2-1_系统顶层数据流图.png',
  '03_图_2-3_普通用户用例图.png',
  '04_图_2-4_管理员用例图.png',
  '05_图_4-1_系统总体架构图.png',
  '06_图_4-2_系统总体功能模块图.png',
  '07_图_4-3_账户管理流程图.png',
  '19_图_4-15_系统总体_E-R_图.png',
  '20_图_4-16_系统核心类图.png',
  '26_图_5-1_登录注册页面.png',
  '35_图_5-10_账户管理页面.png',
]
rows=[]
for k in keys:
    p = img_dir / k
    im = Image.open(p)
    rows.append({
        'file': k,
        'size': p.stat().st_size,
        'wh': f'{im.width}x{im.height}',
        'mode': im.mode,
    })
    print(k, im.width, im.height, p.stat().st_size)

# Write final report
report = Path(r'C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-master\work\full_paper_revision\format_recheck_report.md')
text = '''# 2.2-7.2 图表格式复验报告（最终）

## 交付文件
- 最终版：`选题标定-第12题-个人财务记账与分析系统/论文/个人财务记账与分析系统论文_2.2-7.2格式复验优化版.docx`
- 源完整版：`个人财务记账与分析系统论文_2.2-7.2完整优化版.docx`
- 参考：`基于BS的绿植销售系统的设计与实现.pdf`
- 实际系统：`system/`（Vue3 + Spring Boot + MySQL）

## 参考PDF图注/引图格式（已对齐）
- 图注：`图2-1 标题`（图与编号无空格，编号与标题一空格）
- 表注：`表4-1 标题`
- 正文：`如图2-1所示` / `如表4-1所示`
- 居中：全部图注/表注 `CENTER`

## 图表完备性复验
- 图：37 张（2章4 + 4章21 + 5章12），编号连续，无缺号无重复
- 表：17 张（2章1 + 3章2 + 4章8 + 6章6），编号连续，与 docx.tables=17 一致
- 嵌入图片：每张图附近均有图片，无“有图注无图”
- 正文引用：全部图、表均被正文引用（含 `如图5-x所示`、`如表6-1至表6-6所示`）
- 残留空格问题：0（已消除 `图 2-1` / `如图 2-1 所示` 类写法）

## 内容与实际系统一致性（抽查）
- 路由/页面：Login、Dashboard、Account、Category、Transaction、Budget、RecurringBill、Transfer、Analytics、Import、Settings、Admin 与图5-1~5-12覆盖一致
- 数据库七表：user/account/category/transaction/budget/recurring_bill/budget_alert 与图4-8~4-15、表4-1~4-7一致
- BudgetScheduler：每日 2:00 写入 budget_alert，与正文一致
- 管理员：仅用户列表/删除/角色切换，与图2-4、5.2一致，未虚构搜索/详情

## 关键图目视抽查
| 图 | 结论 |
|---|---|
| 图2-1 顶层DFD | 外部实体+处理+数据流结构完整 |
| 图2-3/2-4 用例图 | 普通用户功能全、管理员边界准确 |
| 图4-1 架构图 | B/S 前后端分离分层匹配技术栈 |
| 图4-2 模块图 | 七大功能模块与系统一致 |
| 图4-3 流程图 | 账户管理流程符号规范 |
| 图4-15 E-R | 七实体关系合理 |
| 图4-16 类图 | 核心实体/服务协作清楚 |
| 图5-1/5-10 截图 | 真实系统页面截图 |

## 相对参考PDF的结构覆盖
参考论文图表类型（用例/DFD/架构/模块/流程/实体属性/ER/类图/时序/实现截图/测试表）在本财务论文中均已覆盖，且数量按本系统规模补齐（含预算预警实体、CSV导入流程与时序、管理员真实能力边界）。

## 结论
2.2—7.2 图表格式已按参考PDF严格复验并对齐；图表齐全、编号连续、正文均有引用；关键图与 `system` 源码一致。最终交付使用「格式复验优化版」。
'''
report.write_text(text, encoding='utf-8')
print('REPORT_OK', report)
print(json.dumps(rows, ensure_ascii=False, indent=2))
