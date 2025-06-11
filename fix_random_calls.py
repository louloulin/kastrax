#!/usr/bin/env python3
import re
import os
import glob

def fix_random_calls(file_path):
    """修复文件中的random()调用"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 修复 (min..max)Random.nextInt() 模式
    pattern1 = r'\((\d+)\.\.(\d+)\)Random\.nextInt\(\)'
    def replace1(match):
        min_val = int(match.group(1))
        max_val = int(match.group(2))
        return f'Random.nextInt({min_val}, {max_val + 1})'
    
    content = re.sub(pattern1, replace1, content)
    
    # 修复 (min.0..max.0)Random.nextInt() 模式 (用于Double)
    pattern2 = r'\((\d+\.?\d*)\.\.(\d+\.?\d*)\)Random\.nextInt\(\)'
    def replace2(match):
        min_val = float(match.group(1))
        max_val = float(match.group(2))
        return f'Random.nextDouble({min_val}, {max_val})'
    
    content = re.sub(pattern2, replace2, content)
    
    # 修复 listOf(...).random() 模式
    pattern3 = r'listOf\([^)]+\)\.random\(\)'
    def replace3(match):
        return match.group(0).replace('.random()', '.random()')
    
    # 这个模式需要保持不变，因为它是正确的
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"Fixed: {file_path}")

def main():
    # 查找所有Kotlin文件
    kotlin_files = glob.glob('kastrax-edutech/src/main/kotlin/**/*.kt', recursive=True)
    
    for file_path in kotlin_files:
        if os.path.isfile(file_path):
            fix_random_calls(file_path)
    
    print("All files fixed!")

if __name__ == "__main__":
    main()
