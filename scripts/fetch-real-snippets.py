#!/usr/bin/env python3
import json
import urllib.request
import os

# Define the source files, their raw URLs, and user-facing source URLs
languages = {
    "real-kotlin": {
        "language": "kotlin",
        "url": "https://raw.githubusercontent.com/ZacSweers/metro/main/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/graph/MetroSort.kt",
        "sourceUrl": "https://github.com/ZacSweers/metro/blob/main/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/graph/MetroSort.kt"
    },
    "real-c": {
        "language": "c",
        "url": "https://raw.githubusercontent.com/MariaDB/server/main/client/mysql_plugin.c",
        "sourceUrl": "https://github.com/MariaDB/server/blob/main/client/mysql_plugin.c"
    },
    "real-rust": {
        "language": "rust",
        "url": "https://raw.githubusercontent.com/rust-lang/regex/master/src/lib.rs",
        "sourceUrl": "https://github.com/rust-lang/regex/blob/master/src/lib.rs"
    },
    "real-go": {
        "language": "go",
        "url": "https://raw.githubusercontent.com/golang/go/master/src/hash/fnv/fnv.go",
        "sourceUrl": "https://github.com/golang/go/blob/master/src/hash/fnv/fnv.go"
    },
    "real-csharp": {
        "language": "csharp",
        "url": "https://raw.githubusercontent.com/dotnet/efcore/main/src/EFCore/Diagnostics/EventDefinition.cs",
        "sourceUrl": "https://github.com/dotnet/efcore/blob/main/src/EFCore/Diagnostics/EventDefinition.cs"
    }
}

# Resolve paths relative to the script location
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.dirname(SCRIPT_DIR)
manifest_path = os.path.join(REPO_ROOT, "compose-highlight", "src", "test", "resources", "highlight-fixtures", "snippets.json")

def main():
    if os.path.exists(manifest_path):
        with open(manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)
    else:
        manifest = {}

    for name, info in languages.items():
        print(f"Fetching {name} from {info['url']}...")
        try:
            req = urllib.request.Request(
                info["url"], 
                headers={'User-Agent': 'Mozilla/5.0'}
            )
            with urllib.request.urlopen(req) as response:
                code = response.read().decode("utf-8")
                manifest[name] = {
                    "language": info["language"],
                    "sourceUrl": info["sourceUrl"],
                    "code": code
                }
                print(f"Successfully fetched {name} ({len(code)} chars)")
        except Exception as e:
            print(f"Error fetching {name}: {e}")
            return

    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)
    
    print(f"Updated {os.path.relpath(manifest_path, REPO_ROOT)} successfully!")

if __name__ == "__main__":
    main()
