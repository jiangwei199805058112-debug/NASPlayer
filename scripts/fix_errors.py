import os
import openai

openai.api_key = os.getenv("OPENAI_API_KEY")

def generate_fix(log_content: str) -> str:
    prompt = f"""
你是一个 Kotlin/Android 构建修复专家。
以下是 Gradle 编译错误日志，请分析并输出一个 Git 补丁 (diff 格式)，用于修复错误。

错误日志：
{log_content}

⚠️ 要求：
1. 输出必须是标准的 `git diff` 格式补丁。
2. 不要写解释，只要补丁内容。
3. 确保补丁能直接用 `git apply` 应用。
"""
    response = openai.ChatCompletion.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": prompt}],
    )
    return response["choices"][0]["message"]["content"]

if __name__ == "__main__":
    log_file = "app/build/reports/build.log"
    if not os.path.exists(log_file):
        print("No build log found")
        exit(1)

    with open(log_file, "r") as f:
        log_content = f.read()

    patch = generate_fix(log_content)
    print(patch)
