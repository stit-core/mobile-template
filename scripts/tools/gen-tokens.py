#!/usr/bin/env python3
# Генератор токенов дизайна: design/tokens.json (DTCG) → Tokens.kt (Compose) и Tokens.swift (Foundation-only).
# Детерминирован (порядок — порядок файла), без зависимостей: make gen-tokens пишет в дерево, tokens-check — в temp и сравнивает.
#   gen-tokens.py [--out <корень>]
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOURCE = ROOT / "design" / "tokens.json"
KOTLIN = pathlib.Path("androidApp/core/designsystem/src/main/kotlin/tech/stit/mobile/android/core/designsystem/generated/Tokens.kt")
SWIFT = pathlib.Path("iosApp/Packages/Pure/Sources/DesignSystemTokens/Tokens.swift")
HEADER = "Сгенерировано из design/tokens.json командой make gen-tokens — руками не правится, дрейф ловит tokens-check."
CATEGORIES = {"color": "Colors", "dimension": "Dimensions", "typography": "Typography"}


def tokens(node, path, inherited_type):
    kind = node.get("$type", inherited_type)
    if "$value" in node:
        yield path, kind, node["$value"]
        return
    for key, child in node.items():
        if not key.startswith("$"):
            yield from tokens(child, path + [key], kind)


def camel(path):
    head, *tail = path
    return head + "".join(part[:1].upper() + part[1:] for part in tail)


def argb(hex_color):
    match = re.fullmatch(r"#([0-9A-Fa-f]{6})", hex_color)
    if not match:
        sys.exit(f"gen-tokens: ожидался цвет #RRGGBB, получено {hex_color!r}")
    return "0xFF" + match.group(1).upper()


def dimension(value):
    match = re.fullmatch(r"(\d+)(dp|sp)", value)
    if not match:
        sys.exit(f"gen-tokens: ожидался размер <число>dp|sp, получено {value!r}")
    return match.group(1), match.group(2)


def by_category(source):
    grouped = {name: [] for name in CATEGORIES}
    for path, kind, value in tokens(source, [], None):
        category = path[0]
        if category not in grouped:
            sys.exit(f"gen-tokens: неизвестная категория {category!r} (допустимы {', '.join(CATEGORIES)})")
        grouped[category].append((camel(path[1:]), kind, value))
    return grouped


def kotlin(grouped):
    lines = [
        "package tech.stit.mobile.android.core.designsystem.generated",
        "",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.ui.unit.Dp",
        "import androidx.compose.ui.unit.TextUnit",
        "import androidx.compose.ui.unit.dp",
        "import androidx.compose.ui.unit.sp",
        "",
        f"// {HEADER}",
        "class ColorToken(val light: Color, val dark: Color)",
        "",
        "object Tokens {",
    ]
    for index, (category, object_name) in enumerate(CATEGORIES.items()):
        if index:
            lines.append("")
        lines.append(f"    object {object_name} {{")
        for name, kind, value in grouped[category]:
            if kind == "color":
                lines.append(f"        val {name}: ColorToken = ColorToken(light = Color({argb(value['light'])}), dark = Color({argb(value['dark'])}))")
            else:
                number, unit = dimension(value)
                kotlin_type = "Dp" if unit == "dp" else "TextUnit"
                lines.append(f"        val {name}: {kotlin_type} = {number}.{unit}")
        lines.append("    }")
    lines.append("}")
    return "\n".join(lines) + "\n"


def swift(grouped):
    lines = [
        f"// {HEADER}",
        "nonisolated public struct ColorToken: Sendable {",
        "  public let light: UInt32",
        "  public let dark: UInt32",
        "",
        "  public init(light: UInt32, dark: UInt32) {",
        "    self.light = light",
        "    self.dark = dark",
        "  }",
        "}",
        "",
        "nonisolated public enum Tokens {",
    ]
    for index, (category, enum_name) in enumerate(CATEGORIES.items()):
        if index:
            lines.append("")
        lines.append(f"  nonisolated public enum {enum_name} {{")
        for name, kind, value in grouped[category]:
            if kind == "color":
                lines.append(f"    public static let {name} = ColorToken(light: {argb(value['light'])}, dark: {argb(value['dark'])})")
            else:
                number, _ = dimension(value)
                lines.append(f"    public static let {name}: Double = {number}")
        lines.append("  }")
    lines.append("}")
    return "\n".join(lines) + "\n"


def main(argv):
    out = ROOT
    if argv[1:2] == ["--out"] and len(argv) == 3:
        out = pathlib.Path(argv[2])
    elif argv[1:]:
        sys.exit("использование: gen-tokens.py [--out <корень>]")
    grouped = by_category(json.loads(SOURCE.read_text(encoding="utf-8")))
    for relative, text in ((KOTLIN, kotlin(grouped)), (SWIFT, swift(grouped))):
        target = out / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")
        print(f"gen-tokens: {relative}")


if __name__ == "__main__":
    main(sys.argv)
