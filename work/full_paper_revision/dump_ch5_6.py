from docx import Document
from pathlib import Path
path = Path(r'C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-master\选题标定-第12题-个人财务记账与分析系统\论文\个人财务记账与分析系统论文_2.2-7.2格式复验优化版.docx')
doc = Document(str(path))
for i,p in enumerate(doc.paragraphs):
    if i < 188: continue
    t=(p.text or '').strip()
    if not t: continue
    style = p.style.name if p.style else ''
    print(f'{i}|{style}|{t[:180]}')
