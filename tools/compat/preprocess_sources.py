#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from pathlib import Path

MARKER = re.compile(r"^(?P<indent>\s*)(?:\*/)?//\?\s*(?P<body>.*)$")
IF_MARKER = re.compile(r"^if\s+(.+?)\s*\{$")


def version_tuple(value: str) -> tuple[int, ...]:
    return tuple(int(part) for part in value.split("."))


def evaluate(expr: str, minecraft: str) -> bool:
    expr = expr.strip()
    match = re.fullmatch(r"(>=|<=|>|<|==|=)\s*([0-9]+(?:\.[0-9]+)*)", expr)
    if not match:
        raise ValueError(f"Unsupported compatibility condition: {expr!r}")
    op, rhs = match.groups()
    left_v = version_tuple(minecraft)
    right_v = version_tuple(rhs)
    if op == ">=": return left_v >= right_v
    if op == "<=": return left_v <= right_v
    if op == ">": return left_v > right_v
    if op == "<": return left_v < right_v
    return left_v == right_v


def uncomment_selected(line: str) -> str:
    # Stonecutter-style inactive branches are wrapped in /* ... */.  When that
    # branch is selected, only the opening token is present on a source line;
    # the closing token is attached to the //? marker line and is discarded.
    leading = len(line) - len(line.lstrip())
    body = line[leading:]
    if body.startswith("/*"):
        body = body[2:]
    return line[:leading] + body


def preprocess_text(text: str, minecraft: str) -> str:
    output: list[str] = []
    # frame = [outer_active, condition_result, in_else]
    stack: list[list[object]] = []
    active = True

    for raw in text.splitlines(keepends=True):
        stripped = raw.rstrip("\r\n")
        marker = MARKER.match(stripped)
        if marker:
            body = marker.group("body").strip()
            if_match = IF_MARKER.match(body)
            if if_match:
                condition = evaluate(if_match.group(1), minecraft)
                stack.append([active, condition, False])
                active = bool(active and condition)
                continue
            if body == "} else {":
                if not stack:
                    raise ValueError("Unexpected //?} else {")
                outer, condition, _ = stack[-1]
                stack[-1][2] = True
                active = bool(outer and not condition)
                continue
            if body == "}":
                if not stack:
                    raise ValueError("Unexpected //?}")
                outer, _, _ = stack.pop()
                active = bool(outer)
                continue

        if active:
            output.append(uncomment_selected(raw))

    if stack:
        raise ValueError("Unclosed //? compatibility block")

    result = "".join(output)
    # Mojang renamed ResourceLocation to Identifier in 1.21.11.
    if version_tuple(minecraft) < version_tuple("1.21.11"):
        result = re.sub(r"\bIdentifier\b", "ResourceLocation", result)
    return result


def process_path(path: Path, minecraft: str) -> int:
    changed = 0
    files = [path] if path.is_file() else sorted(path.rglob("*.java"))
    for file in files:
        before = file.read_text(encoding="utf-8")
        after = preprocess_text(before, minecraft)
        if after != before:
            file.write_text(after, encoding="utf-8")
            changed += 1
    return changed


def main() -> int:
    parser = argparse.ArgumentParser(description="Resolve MineIPTV //? version branches for a source-compatibility probe.")
    parser.add_argument("--minecraft", required=True)
    parser.add_argument("paths", nargs="+", type=Path)
    args = parser.parse_args()

    changed = sum(process_path(path, args.minecraft) for path in args.paths)
    print(f"Preprocessed {changed} Java file(s) for Minecraft {args.minecraft}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
