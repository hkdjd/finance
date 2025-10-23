#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Finance项目架构图生成器（修正版）
修正数据库为PostgreSQL
"""

from PIL import Image, ImageDraw, ImageFont
import os

# 创建画布
width, height = 1600, 1200
img = Image.new('RGB', (width, height), 'white')
draw = ImageDraw.Draw(img)

# 使用默认字体避免字体问题，调大字体尺寸
try:
    title_font = ImageFont.truetype('/System/Library/Fonts/Helvetica.ttc', 42)  
    header_font = ImageFont.truetype('/System/Library/Fonts/Helvetica.ttc', 30) 
    text_font = ImageFont.truetype('/System/Library/Fonts/Helvetica.ttc', 22)   
    small_font = ImageFont.truetype('/System/Library/Fonts/Helvetica.ttc', 18)  
except:
    title_font = ImageFont.load_default()
    header_font = ImageFont.load_default()
    text_font = ImageFont.load_default()
    small_font = ImageFont.load_default()

# 定义颜色
colors = {
    'frontend': '#4A90E2',     # 蓝色 - 前端
    'backend': '#7ED321',      # 绿色 - 后端
    'ai': '#F5A623',          # 橙色 - AI
    'database': '#BD10E0',     # 紫色 - 数据库
    'external': '#B8E986',     # 浅绿 - 外部服务
    'text': '#333333',         # 文本颜色
    'white': '#FFFFFF'
}

def draw_rounded_rect(draw, coords, fill_color, outline_color='black', width=2):
    """绘制圆角矩形"""
    x1, y1, x2, y2 = coords
    draw.rectangle(coords, fill=fill_color, outline=outline_color, width=width)

def draw_arrow(draw, start, end, color='#333333', width=3):
    """绘制箭头"""
    draw.line([start, end], fill=color, width=width)
    # 简单的箭头头部
    x1, y1 = start
    x2, y2 = end
    if x2 > x1:  # 向右的箭头
        draw.polygon([(x2-10, y2-5), (x2, y2), (x2-10, y2+5)], fill=color)
    elif x2 < x1:  # 向左的箭头
        draw.polygon([(x2+10, y2-5), (x2, y2), (x2+10, y2+5)], fill=color)
    elif y2 > y1:  # 向下的箭头
        draw.polygon([(x2-5, y2-10), (x2, y2), (x2+5, y2-10)], fill=color)
    else:  # 向上的箭头
        draw.polygon([(x2-5, y2+10), (x2, y2), (x2+5, y2+10)], fill=color)

# 标题
draw.text((width//2, 50), 'Finance System Architecture', font=title_font, fill=colors['text'], anchor='mm')

# === 前端模块 (MFE) ===
mfe_rect = (100, 150, 400, 350)
draw_rounded_rect(draw, mfe_rect, colors['frontend'])
draw.text((250, 180), 'MFE Frontend', font=header_font, fill=colors['white'], anchor='mm')
draw.text((250, 210), 'React + TypeScript', font=text_font, fill=colors['white'], anchor='mm')
draw.text((250, 240), 'Port: 3000', font=small_font, fill=colors['white'], anchor='mm')
draw.text((250, 270), '• Contract Management UI', font=small_font, fill=colors['white'], anchor='mm')
draw.text((250, 290), '• Amortization Management', font=small_font, fill=colors['white'], anchor='mm')
draw.text((250, 310), '• Audit Log Viewer', font=small_font, fill=colors['white'], anchor='mm')

# === 后端模块 (MS) ===
ms_rect = (500, 150, 800, 350)
draw_rounded_rect(draw, ms_rect, colors['backend'])
draw.text((650, 180), 'MS Backend Service', font=header_font, fill=colors['white'], anchor='mm')
draw.text((650, 210), 'Spring Boot + JPA', font=text_font, fill=colors['white'], anchor='mm')
draw.text((650, 240), 'Port: 8081', font=small_font, fill=colors['white'], anchor='mm')
draw.text((650, 270), '• RESTful API', font=small_font, fill=colors['white'], anchor='mm')
draw.text((650, 290), '• Swagger UI Documentation', font=small_font, fill=colors['white'], anchor='mm')
draw.text((650, 310), '• Audit Log Recording', font=small_font, fill=colors['white'], anchor='mm')

# === AI模块 ===
ai_rect = (900, 150, 1200, 350)
draw_rounded_rect(draw, ai_rect, colors['ai'])
draw.text((1050, 180), 'AI Contract Parser', font=header_font, fill=colors['white'], anchor='mm')
draw.text((1050, 210), 'Spring Boot + AI', font=text_font, fill=colors['white'], anchor='mm')
draw.text((1050, 240), 'Port: 8082', font=small_font, fill=colors['white'], anchor='mm')
draw.text((1050, 270), '• Document Parsing', font=small_font, fill=colors['white'], anchor='mm')
draw.text((1050, 290), '• Information Extraction', font=small_font, fill=colors['white'], anchor='mm')

# === 数据库 (修正为PostgreSQL) ===
db_rect = (500, 450, 800, 600)
draw_rounded_rect(draw, db_rect, colors['database'])
draw.text((650, 480), 'PostgreSQL Database', font=header_font, fill=colors['white'], anchor='mm')
draw.text((650, 510), '• Contract Data', font=small_font, fill=colors['white'], anchor='mm')
draw.text((650, 530), '• Amortization Entries', font=small_font, fill=colors['white'], anchor='mm')
draw.text((650, 550), '• Audit Log (audit_log)', font=small_font, fill=colors['white'], anchor='mm')
draw.text((650, 570), '• Journal Entries', font=small_font, fill=colors['white'], anchor='mm')

# === 文件存储系统 ===
fs_rect = (100, 450, 400, 600)
draw_rounded_rect(draw, fs_rect, colors['external'])
draw.text((250, 480), 'File Storage System', font=header_font, fill=colors['text'], anchor='mm')
draw.text((250, 510), '• Contract Documents', font=small_font, fill=colors['text'], anchor='mm')
draw.text((250, 530), '• Upload File Management', font=small_font, fill=colors['text'], anchor='mm')
draw.text((250, 550), '• Static Resources', font=small_font, fill=colors['text'], anchor='mm')

# === 外部AI服务 ===
ai_service_rect = (1000, 450, 1300, 600)
draw_rounded_rect(draw, ai_service_rect, '#FF6B6B', 'black')
draw.text((1150, 480), 'External AI Services', font=header_font, fill=colors['white'], anchor='mm')
draw.text((1150, 510), '• DeepSeek API', font=small_font, fill=colors['white'], anchor='mm')
draw.text((1150, 530), '• Gemini API (Backup)', font=small_font, fill=colors['white'], anchor='mm')
draw.text((1150, 550), '• Gemma3 Model', font=small_font, fill=colors['white'], anchor='mm')
draw.text((1150, 570), '• Text Analysis & Extraction', font=small_font, fill=colors['white'], anchor='mm')

# === 核心功能模块 ===
# 合同管理
contract_rect = (100, 700, 300, 820)
draw_rounded_rect(draw, contract_rect, '#E8F4FD', '#4A90E2')
draw.text((200, 730), 'Contract Management', font=text_font, fill='#4A90E2', anchor='mm')
draw.text((200, 760), '• Contract CRUD', font=small_font, fill='#4A90E2', anchor='mm')
draw.text((200, 780), '• File Upload', font=small_font, fill='#4A90E2', anchor='mm')

# 摊销管理
amort_rect = (350, 700, 550, 820)
draw_rounded_rect(draw, amort_rect, '#F0F9FF', '#7ED321')
draw.text((450, 730), 'Amortization Mgmt', font=text_font, fill='#7ED321', anchor='mm')
draw.text((450, 760), '• Calculation', font=small_font, fill='#7ED321', anchor='mm')
draw.text((450, 780), '• Payment Records', font=small_font, fill='#7ED321', anchor='mm')

# 审计日志
audit_rect = (600, 700, 800, 820)
draw_rounded_rect(draw, audit_rect, '#FDF2F8', '#BD10E0')
draw.text((700, 730), 'Audit Log', font=text_font, fill='#BD10E0', anchor='mm')
draw.text((700, 760), '• Operation Records', font=small_font, fill='#BD10E0', anchor='mm')
draw.text((700, 780), '• History Tracking', font=small_font, fill='#BD10E0', anchor='mm')

# 报表分析
report_rect = (850, 700, 1050, 820)
draw_rounded_rect(draw, report_rect, '#FFF7ED', '#F5A623')
draw.text((950, 730), 'Reports & Analytics', font=text_font, fill='#F5A623', anchor='mm')
draw.text((950, 760), '• Dashboard', font=small_font, fill='#F5A623', anchor='mm')
draw.text((950, 780), '• Data Statistics', font=small_font, fill='#F5A623', anchor='mm')

# === 连接线和箭头 ===
# MFE -> MS
draw_arrow(draw, (400, 250), (500, 250))
draw.text((450, 230), 'HTTP API', font=small_font, fill=colors['text'], anchor='mm')

# MS -> AI
draw_arrow(draw, (800, 250), (900, 250))
draw.text((850, 230), 'Contract Parse', font=small_font, fill=colors['text'], anchor='mm')

# MS -> Database
draw_arrow(draw, (650, 350), (650, 450))
draw.text((700, 400), 'JPA/Hibernate', font=small_font, fill=colors['text'], anchor='mm')

# MS -> File System
draw_arrow(draw, (500, 300), (400, 500))
draw.text((430, 380), 'File Operations', font=small_font, fill=colors['text'], anchor='mm')

# AI -> External AI Services
draw_arrow(draw, (1050, 350), (1150, 450))
draw.text((1120, 380), 'API Calls', font=small_font, fill=colors['text'], anchor='mm')

# === 端口和技术栈信息 ===
draw.text((100, 900), 'Port Configuration:', font=text_font, fill=colors['text'], anchor='lm')
draw.text((100, 930), '• MFE Frontend: localhost:3000', font=small_font, fill=colors['text'], anchor='lm')
draw.text((100, 950), '• MS Backend: localhost:8081', font=small_font, fill=colors['text'], anchor='lm')
draw.text((100, 970), '• AI Service: localhost:8082', font=small_font, fill=colors['text'], anchor='lm')
draw.text((100, 990), '• Database: PostgreSQL (Local)', font=small_font, fill=colors['text'], anchor='lm')

draw.text((600, 900), 'Technology Stack:', font=text_font, fill=colors['text'], anchor='lm')
draw.text((600, 930), '• Frontend: React + TypeScript + Ant Design', font=small_font, fill=colors['text'], anchor='lm')
draw.text((600, 950), '• Backend: Spring Boot + JPA + PostgreSQL + Swagger', font=small_font, fill=colors['text'], anchor='lm')
draw.text((600, 970), '• AI: Spring Boot + External AI APIs', font=small_font, fill=colors['text'], anchor='lm')
draw.text((600, 990), '• Database: PostgreSQL + Flyway Migration', font=small_font, fill=colors['text'], anchor='lm')

draw.text((1100, 900), 'External Services:', font=text_font, fill=colors['text'], anchor='lm')
draw.text((1100, 930), '• DeepSeek AI API (Primary)', font=small_font, fill=colors['text'], anchor='lm')
draw.text((1100, 950), '• Google Gemini API (Backup)', font=small_font, fill=colors['text'], anchor='lm')
draw.text((1100, 970), '• Gemma3 Model Support', font=small_font, fill=colors['text'], anchor='lm')

# 新增功能说明
draw.text((100, 1050), 'Latest Features:', font=text_font, fill='#E31E24', anchor='lm')
draw.text((100, 1080), '• Audit Log functionality implemented', font=small_font, fill='#E31E24', anchor='lm')
draw.text((100, 1100), '• Swagger UI API documentation integrated', font=small_font, fill='#E31E24', anchor='lm')
draw.text((100, 1120), '• Payment operation history tracking', font=small_font, fill='#E31E24', anchor='lm')
draw.text((100, 1140), '• Interactive API testing interface', font=small_font, fill='#E31E24', anchor='lm')

# 保存图片
output_path = '/Users/victor/Develop/code/ocbc/finance/finance-architecture-corrected.png'
img.save(output_path, 'PNG', quality=95)
print(f"Corrected architecture diagram saved as: {output_path}")
