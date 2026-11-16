#!/usr/bin/env node
/**
 * Lucide SVG -> Jetpack Compose ImageVector converter.
 * Reads lucide-static SVGs (24x24, stroke-based) and emits Kotlin ImageVector
 * builder code. Handles: path (M m L l H h V v C c S s Q q T t A a Z z with
 * compact SVG arc-flag syntax), circle, rect (with rx), line, polyline,
 * polygon.
 *
 * Compose PathBuilder.arcTo(rx, ry, thetaDeg, isMoreThanHalf, isPositiveArc,
 * x1, y1) takes the END point; the arc starts at the builder's current point
 * (android.util.PathParser semantics) — exactly SVG endpoint parameterization.
 */
const fs = require("fs");
const path = require("path");

const DIR = __dirname;
const OUT = path.join(DIR, "LucideIcons.kt");

function f(n) {
  if (!Number.isFinite(n)) throw new Error("non-finite coordinate: " + n);
  const r = Math.round(n * 1000) / 1000;
  return Number.isInteger(r) ? r + ".0f" : r + "f";
}

// ---------- context-aware SVG path scanner ----------
function makeScanner(d) {
  let i = 0;
  const n = d.length;
  function skipSep() {
    while (i < n && (d[i] === " " || d[i] === "," || d[i] === "\t" || d[i] === "\n" || d[i] === "\r")) i++;
  }
  function readNumber() {
    skipSep();
    const start = i;
    if (i < n && (d[i] === "+" || d[i] === "-")) i++;
    while (i < n && d[i] >= "0" && d[i] <= "9") i++;
    if (i < n && d[i] === ".") {
      i++;
      while (i < n && d[i] >= "0" && d[i] <= "9") i++;
    }
    if (i < n && (d[i] === "e" || d[i] === "E")) {
      i++;
      if (i < n && (d[i] === "+" || d[i] === "-")) i++;
      while (i < n && d[i] >= "0" && d[i] <= "9") i++;
    }
    if (i === start) {
      throw new Error("expected number at " + i + " in: " + d.slice(Math.max(0, i - 12), i + 12));
    }
    return parseFloat(d.slice(start, i));
  }
  function readFlag() {
    skipSep();
    const c = d[i];
    if (c !== "0" && c !== "1") {
      throw new Error("expected arc flag at " + i + " in: " + d.slice(Math.max(0, i - 8), i + 8));
    }
    i++;
    return parseInt(c, 10);
  }
  function readCommand() {
    skipSep();
    if (i >= n) return null;
    const c = d[i];
    if (/[MmLlHhVvCcSsQqTtAaZz]/.test(c)) {
      i++;
      return c;
    }
    return "continue"; // implicit repeat of the previous command
  }
  return { readNumber, readFlag, readCommand };
}

function convertPath(d) {
  const s = makeScanner(d);
  const calls = [];
  let cx = 0, cy = 0;
  let sx = 0, sy = 0;
  let lastC2x = null, lastC2y = null;
  let lastQx = null, lastQy = null;
  let prevCmd = "";

  let cmd = s.readCommand();
  while (cmd !== null) {
    if (cmd === "Z" || cmd === "z") {
      calls.push(["close", []]);
      cx = sx; cy = sy;
      lastC2x = lastQx = null;
      prevCmd = cmd;
      cmd = s.readCommand();
      continue;
    }
    if (cmd === "continue") {
      cmd = prevCmd === "M" ? "L" : prevCmd === "m" ? "l" : prevCmd;
    }

    switch (cmd) {
      case "M":
      case "L": {
        const x = s.readNumber(), y = s.readNumber();
        if (cmd === "M") { calls.push(["moveTo", [f(x), f(y)]]); sx = x; sy = y; }
        else calls.push(["lineTo", [f(x), f(y)]]);
        cx = x; cy = y;
        break;
      }
      case "m":
      case "l": {
        const dx = s.readNumber(), dy = s.readNumber();
        cx += dx; cy += dy;
        if (cmd === "m") { calls.push(["moveTo", [f(cx), f(cy)]]); sx = cx; sy = cy; }
        else calls.push(["lineTo", [f(cx), f(cy)]]);
        break;
      }
      case "H":
      case "h": {
        const v = s.readNumber();
        cx = cmd === "H" ? v : cx + v;
        calls.push(["lineTo", [f(cx), f(cy)]]);
        break;
      }
      case "V":
      case "v": {
        const v = s.readNumber();
        cy = cmd === "V" ? v : cy + v;
        calls.push(["lineTo", [f(cx), f(cy)]]);
        break;
      }
      case "C":
      case "c": {
        const x1 = s.readNumber(), y1 = s.readNumber();
        const x2 = s.readNumber(), y2 = s.readNumber();
        const x = s.readNumber(), y = s.readNumber();
        const ax1 = cmd === "C" ? x1 : cx + x1;
        const ay1 = cmd === "C" ? y1 : cy + y1;
        const ax2 = cmd === "C" ? x2 : cx + x2;
        const ay2 = cmd === "C" ? y2 : cy + y2;
        const ax = cmd === "C" ? x : cx + x;
        const ay = cmd === "C" ? y : cy + y;
        calls.push(["curveTo", [f(ax1), f(ay1), f(ax2), f(ay2), f(ax), f(ay)]]);
        lastC2x = ax2; lastC2y = ay2;
        cx = ax; cy = ay;
        lastQx = null;
        break;
      }
      case "S":
      case "s": {
        const x2 = s.readNumber(), y2 = s.readNumber();
        const x = s.readNumber(), y = s.readNumber();
        let x1, y1;
        if (lastC2x !== null && (prevCmd === "C" || prevCmd === "S" || prevCmd === "c" || prevCmd === "s")) {
          x1 = 2 * cx - lastC2x; y1 = 2 * cy - lastC2y;
        } else { x1 = cx; y1 = cy; }
        const ax2 = cmd === "S" ? x2 : cx + x2;
        const ay2 = cmd === "S" ? y2 : cy + y2;
        const ax = cmd === "S" ? x : cx + x;
        const ay = cmd === "S" ? y : cy + y;
        calls.push(["curveTo", [f(x1), f(y1), f(ax2), f(ay2), f(ax), f(ay)]]);
        lastC2x = ax2; lastC2y = ay2;
        cx = ax; cy = ay;
        lastQx = null;
        break;
      }
      case "Q":
      case "q": {
        const x1 = s.readNumber(), y1 = s.readNumber();
        const x = s.readNumber(), y = s.readNumber();
        const ax1 = cmd === "Q" ? x1 : cx + x1;
        const ay1 = cmd === "Q" ? y1 : cy + y1;
        const ax = cmd === "Q" ? x : cx + x;
        const ay = cmd === "Q" ? y : cy + y;
        calls.push(["quadraticBezierTo", [f(ax1), f(ay1), f(ax), f(ay)]]);
        lastQx = ax1; lastQy = ay1;
        cx = ax; cy = ay;
        lastC2x = null;
        break;
      }
      case "T":
      case "t": {
        const x = s.readNumber(), y = s.readNumber();
        let x1, y1;
        if (lastQx !== null && (prevCmd === "Q" || prevCmd === "T" || prevCmd === "q" || prevCmd === "t")) {
          x1 = 2 * cx - lastQx; y1 = 2 * cy - lastQy;
        } else { x1 = cx; y1 = cy; }
        const ax = cmd === "T" ? x : cx + x;
        const ay = cmd === "T" ? y : cy + y;
        calls.push(["quadraticBezierTo", [f(x1), f(y1), f(ax), f(ay)]]);
        lastQx = x1; lastQy = y1;
        cx = ax; cy = ay;
        lastC2x = null;
        break;
      }
      case "A":
      case "a": {
        const rx = s.readNumber(), ry = s.readNumber();
        const rot = s.readNumber();
        const laf = s.readFlag();
        const sf = s.readFlag();
        const x = s.readNumber(), y = s.readNumber();
        const ax = cmd === "A" ? x : cx + x;
        const ay = cmd === "A" ? y : cy + y;
        calls.push(["arcTo", [
          f(rx), f(ry), f(rot),
          laf === 1 ? "true" : "false",
          sf === 1 ? "true" : "false",
          f(ax), f(ay),
        ]]);
        cx = ax; cy = ay;
        lastC2x = lastQx = null;
        break;
      }
      default:
        throw new Error("Unknown path command: " + cmd);
    }

    prevCmd = cmd;
    cmd = s.readCommand();
  }
  return calls;
}

// ---------- element converters ----------
function circleToPath(cx, cy, r) {
  const calls = [];
  calls.push(["moveTo", [f(cx + r), f(cy)]]);
  calls.push(["arcTo", [f(r), f(r), "0.0f", "false", "true", f(cx - r), f(cy)]]);
  calls.push(["arcTo", [f(r), f(r), "0.0f", "false", "true", f(cx + r), f(cy)]]);
  calls.push(["close", []]);
  return calls;
}

function rectToPath(x, y, w, h, rx, ry) {
  rx = rx || 0;
  ry = ry || rx || 0;
  const calls = [];
  if (rx <= 0 || ry <= 0) {
    calls.push(["moveTo", [f(x), f(y)]]);
    calls.push(["lineTo", [f(x + w), f(y)]]);
    calls.push(["lineTo", [f(x + w), f(y + h)]]);
    calls.push(["lineTo", [f(x), f(y + h)]]);
    calls.push(["close", []]);
    return calls;
  }
  rx = Math.min(rx, w / 2);
  ry = Math.min(ry, h / 2);
  calls.push(["moveTo", [f(x + rx), f(y)]]);
  calls.push(["lineTo", [f(x + w - rx), f(y)]]);
  calls.push(["arcTo", [f(rx), f(ry), "0.0f", "false", "true", f(x + w), f(y + ry)]]);
  calls.push(["lineTo", [f(x + w), f(y + h - ry)]]);
  calls.push(["arcTo", [f(rx), f(ry), "0.0f", "false", "true", f(x + w - rx), f(y + h)]]);
  calls.push(["lineTo", [f(x + rx), f(y + h)]]);
  calls.push(["arcTo", [f(rx), f(ry), "0.0f", "false", "true", f(x), f(y + h - ry)]]);
  calls.push(["lineTo", [f(x), f(y + ry)]]);
  calls.push(["arcTo", [f(rx), f(ry), "0.0f", "false", "true", f(x + rx), f(y)]]);
  calls.push(["close", []]);
  return calls;
}

function pointsToPath(ptsStr, close) {
  const nums = ptsStr.trim().split(/[\s,]+/).map(Number);
  const calls = [];
  calls.push(["moveTo", [f(nums[0]), f(nums[1])]]);
  for (let i = 2; i < nums.length; i += 2) {
    calls.push(["lineTo", [f(nums[i]), f(nums[i + 1])]]);
  }
  if (close) calls.push(["close", []]);
  return calls;
}

// ---------- parse SVG file ----------
function parseSvg(svg) {
  const elements = [];
  const elemRe = /<(path|circle|rect|line|polyline|polygon)\b([^>]*?)\/?>(?:\s*<\/\1>)?/g;
  let m;
  while ((m = elemRe.exec(svg)) !== null) {
    const tag = m[1];
    const attrs = m[2];
    const get = (name) => {
      const r = new RegExp(name + '\\s*=\\s*"([^"]*)"');
      const mm = attrs.match(r);
      return mm ? mm[1] : null;
    };
    if (tag === "path") {
      const d = get("d");
      if (!d) continue;
      const tf = get("transform");
      let calls = convertPath(d);
      if (tf) {
        const tm = tf.match(/translate\(\s*(-?[\d.]+)[,\s]+(-?[\d.]+)\s*\)/);
        if (tm) {
          const dx = parseFloat(tm[1]), dy = parseFloat(tm[2]);
          calls = calls.map(([name, args]) => {
            if (["moveTo", "lineTo", "curveTo", "quadraticBezierTo"].includes(name)) {
              return [name, args.map((a, idx) => (idx % 2 === 0 ? f(parseFloat(a) + dx) : f(parseFloat(a) + dy)))];
            }
            if (name === "arcTo") {
              // [rx, ry, rot, laf, sf, endX, endY] — shift the end point only
              return [name, [args[0], args[1], args[2], args[3], args[4],
                f(parseFloat(args[5]) + dx), f(parseFloat(args[6]) + dy)]];
            }
            return [name, args];
          });
        }
      }
      elements.push(calls);
    } else if (tag === "circle") {
      elements.push(circleToPath(parseFloat(get("cx")), parseFloat(get("cy")), parseFloat(get("r"))));
    } else if (tag === "rect") {
      elements.push(rectToPath(
        parseFloat(get("x") || "0"), parseFloat(get("y") || "0"),
        parseFloat(get("width")), parseFloat(get("height")),
        get("rx") ? parseFloat(get("rx")) : 0,
        get("ry") ? parseFloat(get("ry")) : undefined,
      ));
    } else if (tag === "line") {
      elements.push(pointsToPath(`${get("x1")},${get("y1")} ${get("x2")},${get("y2")}`, false));
    } else if (tag === "polyline") {
      elements.push(pointsToPath(get("points"), false));
    } else if (tag === "polygon") {
      elements.push(pointsToPath(get("points"), true));
    }
  }
  return elements;
}

// ---------- emit Kotlin ----------
const ICON_MAP = [
  ["menu", "Menu"], ["help-circle", "CircleHelp"], ["square-terminal", "SquareTerminal"],
  ["panel-right-open", "PanelRightOpen"], ["panel-right-close", "PanelRightClose"],
  ["plus", "Plus"], ["chevron-down", "ChevronDown"], ["chevron-right", "ChevronRight"],
  ["hand", "Hand"], ["package", "Package"], ["arrow-up", "ArrowUp"],
  ["message-circle-plus", "MessageCirclePlus"], ["calendar-clock", "CalendarClock"],
  ["blocks", "Blocks"], ["search", "Search"], ["list-filter", "ListFilter"],
  ["folder", "Folder"], ["ellipsis", "Ellipsis"], ["undo-2", "Undo2"], ["check", "Check"],
  ["settings", "Settings"], ["user", "User"], ["smartphone", "Smartphone"],
  ["x", "X"], ["square", "Square"], ["brain", "Brain"], ["pencil", "Pencil"],
  ["loader-circle", "LoaderCircle"], ["file", "File"], ["plug", "Plug"],
  ["git-branch", "GitBranch"], ["paperclip", "Paperclip"], ["square-pen", "SquarePen"],
  ["clock", "Clock"], ["lightbulb", "Lightbulb"], ["shield-check", "ShieldCheck"],
  ["shield-alert", "ShieldAlert"], ["copy", "Copy"], ["thumbs-up", "ThumbsUp"],
  ["thumbs-down", "ThumbsDown"],
];

function renderCalls(calls, indent) {
  return calls.map(([name, args]) => `${indent}${name}(${args.join(", ")})`).join("\n");
}

let out = `package com.qodex.app.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

`;

const body = [];
for (const [file, name] of ICON_MAP) {
  const svgPath = path.join(DIR, file + ".svg");
  if (!fs.existsSync(svgPath)) {
    console.error("MISSING ICON:", file);
    continue;
  }
  const svg = fs.readFileSync(svgPath, "utf8");
  const elements = parseSvg(svg);
  if (elements.length === 0) {
    console.error("NO ELEMENTS:", file);
    continue;
  }
  const pathBlocks = elements.map((calls) => {
    return `        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
${renderCalls(calls, "            ")}
        }`;
  }).join("\n");

  body.push(`    /**
     * Lucide "${file}" icon.
     */
    public val ${name}: ImageVector by lazy {
        ImageVector.Builder(
            name = "Qodex.${name}",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
${pathBlocks}
        }.build()
    }
`);
}

out += `/**
 * The Qodex icon set — exact Lucide geometry (lucide-static v1.73.0), so the
 * Android app renders the same glyphs as the React/Tailwind desktop UI.
 */
object QodexIcons {
${body.join("\n")}
}
`;

fs.writeFileSync(OUT, out);
console.log("WROTE", OUT, "icons:", ICON_MAP.length);
