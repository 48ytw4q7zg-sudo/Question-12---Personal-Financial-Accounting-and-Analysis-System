from __future__ import annotations

import argparse
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


def natural_key(path: Path) -> tuple[int, str]:
    match = re.search(r"(\d+)$", path.stem)
    return (int(match.group(1)) if match else 0, path.name)


def build_sheets(
    input_dir: Path,
    pattern: str,
    output_prefix: str,
    columns: int,
    rows: int,
    thumb_width: int,
    thumb_height: int,
) -> None:
    files = sorted(input_dir.glob(pattern), key=natural_key)
    if not files:
        raise SystemExit(f"No files matched {pattern!r} in {input_dir}")

    label_height = 26
    gutter = 12
    cell_width = thumb_width + gutter * 2
    cell_height = thumb_height + label_height + gutter * 2
    per_sheet = columns * rows
    font = ImageFont.load_default()

    for sheet_index, start in enumerate(range(0, len(files), per_sheet), 1):
        chunk = files[start : start + per_sheet]
        sheet = Image.new(
            "RGB", (columns * cell_width, rows * cell_height), "white"
        )
        draw = ImageDraw.Draw(sheet)

        for index, image_path in enumerate(chunk):
            row, column = divmod(index, columns)
            x0 = column * cell_width
            y0 = row * cell_height
            image = Image.open(image_path).convert("RGB")
            image.thumbnail((thumb_width, thumb_height), Image.Resampling.LANCZOS)
            image_x = x0 + gutter + (thumb_width - image.width) // 2
            image_y = y0 + gutter + label_height
            sheet.paste(image, (image_x, image_y))
            page_number = natural_key(image_path)[0]
            draw.text((x0 + gutter, y0 + gutter), f"Page {page_number}", fill="black", font=font)
            draw.rectangle(
                (x0, y0, x0 + cell_width - 1, y0 + cell_height - 1),
                outline="#B7B7B7",
                width=1,
            )

        output_path = input_dir / f"{output_prefix}-{sheet_index:02d}.png"
        sheet.save(output_path, optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input_dir", type=Path)
    parser.add_argument("--pattern", default="*.png")
    parser.add_argument("--output-prefix", default="contact")
    parser.add_argument("--columns", type=int, default=4)
    parser.add_argument("--rows", type=int, default=4)
    parser.add_argument("--thumb-width", type=int, default=250)
    parser.add_argument("--thumb-height", type=int, default=340)
    args = parser.parse_args()
    build_sheets(
        args.input_dir,
        args.pattern,
        args.output_prefix,
        args.columns,
        args.rows,
        args.thumb_width,
        args.thumb_height,
    )


if __name__ == "__main__":
    main()
