#!/usr/bin/env python3
"""
NAS Player 智能错误修复系统
使用 OpenAI API 分析构建错误并生成修复方案
"""

import os
import sys
import json
import subprocess
import logging
from typing import Dict, List, Optional, Tuple
from openai import OpenAI

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class AutoFixSystem:
    """智能自动修复系统"""
    
    def __init__(self):
        self.client = OpenAI(api_key=os.getenv('OPENAI_API_KEY'))
        self.max_attempts = 3
        self.project_root = os.getcwd()
        
    def run_gradle_build(self) -> Tuple[bool, str]:
        """执行 Gradle 构建并捕获输出"""
        logger.info("🔨 开始执行 Gradle 构建...")
        
        try:
            result = subprocess.run(
                ['./gradlew', 'build', '--no-daemon', '--stacktrace'],
                cwd=self.project_root,
                capture_output=True,
                text=True,
                timeout=600  # 10分钟超时
            )
            
            output = result.stdout + result.stderr
            success = result.returncode == 0
            
            if success:
                logger.info("✅ 构建成功!")
            else:
                logger.warning("❌ 构建失败")
                
            return success, output
            
        except subprocess.TimeoutExpired:
            logger.error("⏰ 构建超时")
            return False, "Build timeout after 10 minutes"
        except Exception as e:
            logger.error(f"💥 构建异常: {e}")
            return False, str(e)
    
    def extract_error_context(self, build_output: str) -> Dict:
        """从构建输出中提取错误上下文"""
        logger.info("🔍 分析构建错误...")
        
        errors = []
        warnings = []
        current_error = None
        
        lines = build_output.split('\n')
        for i, line in enumerate(lines):
            line = line.strip()
            
            # 检测错误
            if any(keyword in line.lower() for keyword in ['error:', 'failed', 'exception']):
                if current_error:
                    errors.append(current_error)
                
                current_error = {
                    'line': line,
                    'context': lines[max(0, i-3):i+4],  # 前后3行上下文
                    'location': self._extract_file_location(line)
                }
            
            # 检测警告
            elif 'warning:' in line.lower():
                warnings.append({
                    'line': line,
                    'context': lines[max(0, i-1):i+2]
                })
        
        if current_error:
            errors.append(current_error)
        
        return {
            'errors': errors,
            'warnings': warnings,
            'full_output': build_output[-3000:] if len(build_output) > 3000 else build_output
        }
    
    def _extract_file_location(self, error_line: str) -> Optional[str]:
        """从错误行中提取文件位置"""
        # 匹配常见的文件路径模式
        import re
        patterns = [
            r'([a-zA-Z0-9_/.-]+\.(?:kt|java|xml|gradle)):(\d+)',
            r'> ([a-zA-Z0-9_/.-]+\.(?:kt|java|xml|gradle))',
            r'at ([a-zA-Z0-9_/.-]+\.(?:kt|java|xml|gradle))',
        ]
        
        for pattern in patterns:
            match = re.search(pattern, error_line)
            if match:
                return match.group(1)
        return None
    
    def generate_fix_with_ai(self, error_context: Dict) -> Optional[str]:
        """使用 AI 生成修复方案"""
        logger.info("🤖 使用 AI 生成修复方案...")
        
        # 构建项目上下文
        project_context = self._get_project_context()
        
        # 构建提示词
        prompt = f"""
你是一个专业的 Android 开发专家，负责分析和修复构建错误。

项目信息:
- Android 项目，使用 Kotlin
- 构建系统: Gradle
- 项目结构: {project_context}

构建错误信息:
{json.dumps(error_context, indent=2, ensure_ascii=False)}

请分析这些错误并提供具体的修复方案。要求:
1. 详细分析错误原因
2. 提供具体的修复步骤
3. 如果需要修改代码，提供完整的修改内容
4. 考虑 Android 开发最佳实践

请以 JSON 格式回复:
{{
    "analysis": "错误分析",
    "fixes": [
        {{
            "file": "文件路径",
            "action": "add|modify|delete",
            "content": "具体修改内容",
            "description": "修改说明"
        }}
    ],
    "commands": ["需要执行的命令"],
    "confidence": 0.8
}}
"""
        
        try:
            response = self.client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": "你是 Android 开发专家，专门修复构建错误。"},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.1,
                max_tokens=2000
            )
            
            return response.choices[0].message.content
            
        except Exception as e:
            logger.error(f"🔥 AI 生成失败: {e}")
            return None
    
    def _get_project_context(self) -> str:
        """获取项目结构上下文"""
        try:
            # 获取关键文件列表
            key_files = []
            for root, dirs, files in os.walk(self.project_root):
                # 跳过 build 和 .git 目录
                dirs[:] = [d for d in dirs if d not in ['build', '.git', '.gradle']]
                
                for file in files:
                    if file.endswith(('.kt', '.java', '.xml', '.gradle', '.properties')):
                        rel_path = os.path.relpath(os.path.join(root, file), self.project_root)
                        key_files.append(rel_path)
                        
                        if len(key_files) > 50:  # 限制文件数量
                            break
                if len(key_files) > 50:
                    break
            
            return '\n'.join(key_files[:30])  # 返回前30个文件
            
        except Exception as e:
            logger.warning(f"获取项目上下文失败: {e}")
            return "Unable to get project context"
    
    def apply_fixes(self, fix_response: str) -> bool:
        """应用 AI 生成的修复方案"""
        logger.info("🔧 应用修复方案...")
        
        try:
            # 解析 AI 响应
            fix_data = json.loads(fix_response)
            
            logger.info(f"📋 分析结果: {fix_data.get('analysis', 'N/A')}")
            logger.info(f"🎯 置信度: {fix_data.get('confidence', 'N/A')}")
            
            # 应用文件修复
            for fix in fix_data.get('fixes', []):
                success = self._apply_file_fix(fix)
                if not success:
                    logger.warning(f"⚠️ 修复失败: {fix.get('description', 'Unknown')}")
            
            # 执行命令
            for cmd in fix_data.get('commands', []):
                logger.info(f"🔄 执行命令: {cmd}")
                try:
                    subprocess.run(cmd, shell=True, check=True, cwd=self.project_root)
                except subprocess.CalledProcessError as e:
                    logger.warning(f"⚠️ 命令执行失败: {e}")
            
            return True
            
        except json.JSONDecodeError as e:
            logger.error(f"🔥 解析 AI 响应失败: {e}")
            return False
        except Exception as e:
            logger.error(f"🔥 应用修复失败: {e}")
            return False
    
    def _apply_file_fix(self, fix: Dict) -> bool:
        """应用单个文件修复"""
        try:
            file_path = os.path.join(self.project_root, fix['file'])
            action = fix['action']
            content = fix['content']
            
            logger.info(f"📝 {action} {fix['file']}: {fix.get('description', '')}")
            
            if action == 'add':
                # 创建新文件
                os.makedirs(os.path.dirname(file_path), exist_ok=True)
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                    
            elif action == 'modify':
                # 修改现有文件
                if os.path.exists(file_path):
                    with open(file_path, 'w', encoding='utf-8') as f:
                        f.write(content)
                else:
                    logger.warning(f"⚠️ 文件不存在: {file_path}")
                    return False
                    
            elif action == 'delete':
                # 删除文件
                if os.path.exists(file_path):
                    os.remove(file_path)
                else:
                    logger.warning(f"⚠️ 文件不存在: {file_path}")
                    
            return True
            
        except Exception as e:
            logger.error(f"🔥 文件修复异常: {e}")
            return False
    
    def run_auto_fix_cycle(self) -> bool:
        """运行一次完整的自动修复循环"""
        logger.info("🚀 开始自动修复循环...")
        
        for attempt in range(1, self.max_attempts + 1):
            logger.info(f"🔄 尝试 {attempt}/{self.max_attempts}")
            
            # 1. 执行构建
            success, build_output = self.run_gradle_build()
            
            if success:
                logger.info("🎉 构建成功! 修复完成")
                return True
            
            # 2. 分析错误
            error_context = self.extract_error_context(build_output)
            
            if not error_context['errors']:
                logger.warning("⚠️ 未找到明确错误信息")
                continue
            
            # 3. 生成修复方案
            fix_response = self.generate_fix_with_ai(error_context)
            
            if not fix_response:
                logger.error("🔥 AI 修复生成失败")
                continue
            
            # 4. 应用修复
            if not self.apply_fixes(fix_response):
                logger.error("🔥 修复应用失败")
                continue
            
            logger.info("✅ 修复应用完成，准备下次构建验证...")
        
        logger.error("💥 达到最大尝试次数，自动修复失败")
        return False


def main():
    """主函数"""
    logger.info("🤖 NAS Player 智能错误修复系统启动")
    
    # 检查环境
    if not os.getenv('OPENAI_API_KEY'):
        logger.error("🔥 未找到 OPENAI_API_KEY 环境变量")
        sys.exit(1)
    
    # 检查 gradlew
    if not os.path.exists('./gradlew'):
        logger.error("🔥 未找到 gradlew 文件")
        sys.exit(1)
    
    # 运行修复系统
    auto_fix = AutoFixSystem()
    success = auto_fix.run_auto_fix_cycle()
    
    if success:
        logger.info("🎉 自动修复成功完成!")
        sys.exit(0)
    else:
        logger.error("💥 自动修复失败")
        sys.exit(1)


if __name__ == '__main__':
    main()
