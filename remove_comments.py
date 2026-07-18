#!/usr/bin/env python3
"""
Remove comments from Kotlin/Java files while preserving string literals.
Handles single-line (//) and multi-line (/* */) comments safely.
"""

import re
import sys
from pathlib import Path

def remove_comments(content):
    """
    Remove single-line and multi-line comments from Kotlin/Java code.
    """
    # State machine to track whether we're in a string or char literal
    result = []
    i = 0
    
    while i < len(content):
        # Handle string literals (double quotes)
        if content[i] == '"':
            result.append(content[i])
            i += 1
            # Skip entire string, handling escape sequences
            while i < len(content):
                if content[i] == '\\' and i + 1 < len(content):
                    result.append(content[i])
                    result.append(content[i + 1])
                    i += 2
                elif content[i] == '"':
                    result.append(content[i])
                    i += 1
                    break
                else:
                    result.append(content[i])
                    i += 1
            continue
        
        # Handle char literals (single quotes)
        if content[i] == "'":
            result.append(content[i])
            i += 1
            while i < len(content):
                if content[i] == '\\' and i + 1 < len(content):
                    result.append(content[i])
                    result.append(content[i + 1])
                    i += 2
                elif content[i] == "'":
                    result.append(content[i])
                    i += 1
                    break
                else:
                    result.append(content[i])
                    i += 1
            continue
        
        # Handle single-line comments
        if content[i:i+2] == '//':
            i += 2
            # Skip until end of line, but keep the newline
            while i < len(content) and content[i] != '\n':
                i += 1
            if i < len(content) and content[i] == '\n':
                result.append('\n')
                i += 1
            continue
        
        # Handle multi-line comments
        if content[i:i+2] == '/*':
            i += 2
            # Skip until */ found
            while i < len(content):
                if content[i:i+2] == '*/':
                    i += 2
                    break
                # Preserve newlines to maintain line structure
                if content[i] == '\n':
                    result.append('\n')
                i += 1
            continue
        
        # Regular character
        result.append(content[i])
        i += 1
    
    return ''.join(result)

def clean_extra_whitespace(content):
    """
    Clean up extra blank lines while preserving at least one newline between code blocks.
    """
    # Replace multiple consecutive newlines with max 2 newlines
    content = re.sub(r'\n\n\n+', '\n\n', content)
    return content

def process_file(filepath):
    """Process a single file and remove comments."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Remove comments
        cleaned = remove_comments(content)
        
        # Clean up excessive whitespace
        cleaned = clean_extra_whitespace(cleaned)
        
        # Write back
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(cleaned)
        
        return True, "Success"
    except Exception as e:
        return False, str(e)

def main():
    # Find all Kotlin and Java files in app/src
    src_dir = Path("app/src")
    if not src_dir.exists():
        print(f"Error: {src_dir} not found")
        sys.exit(1)
    
    files = list(src_dir.glob("**/*.kt")) + list(src_dir.glob("**/*.java"))
    
    if not files:
        print("No Kotlin or Java files found")
        sys.exit(1)
    
    print(f"Found {len(files)} files to process")
    print("=" * 60)
    
    success_count = 0
    error_count = 0
    
    for filepath in sorted(files):
        success, msg = process_file(filepath)
        if success:
            success_count += 1
            print(f"✓ {filepath}")
        else:
            error_count += 1
            print(f"✗ {filepath}: {msg}")
    
    print("=" * 60)
    print(f"\nProcessed: {success_count} files")
    print(f"Errors: {error_count} files")
    
    if error_count == 0:
        print("\n✓ All files processed successfully!")
    else:
        print(f"\n✗ {error_count} files had errors")
        sys.exit(1)

if __name__ == "__main__":
    main()
