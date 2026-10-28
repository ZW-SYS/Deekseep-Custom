#!/usr/bin/env python3
"""Rewrite module reflection calls to go through HostRemap (2.4.1 -> 2.7.1 translation).

Idempotent: already-rewritten calls are left alone.  Run from repo root:
  python3 host/map/rewrite.py module/src/com/dsmod/probe
"""
import re, sys, os

SKIP = {'HostRemap.java', 'HostCompat.java', 'BuildInfo.java'}
CALLS = {
    'getDeclaredMethod': 'declaredMethod',
    'getMethod': 'method',
    'getDeclaredField': 'declaredField',
    'getField': 'field',
    'loadClass': 'load',
}
CLASS_EXPR_TAIL = re.compile(r'(getType|getReturnType|getClass|getSuperclass|getDeclaringClass|getComponentType)\(\)$')

def receiver_start(s, end):
    """Given index of the '.' before a call, return start index of the receiver expression."""
    i = end - 1
    depth = 0
    while i >= 0:
        c = s[i]
        if c in ')]':
            depth += 1
        elif c in '([':
            if depth == 0:
                break
            depth -= 1
        elif depth == 0:
            if c.isalnum() or c in '_$.':
                pass
            elif c == '"':
                return None  # string literal receiver (not ours)
            else:
                break
        elif c == '"':
            # skip string literal inside parens
            j = s.rfind('"', 0, i)
            while j > 0 and s[j - 1] == '\\':
                j = s.rfind('"', 0, j - 1)
            i = j
        i -= 1
    start = i + 1
    expr = s[start:end]
    # strip leading casts/keywords like 'return', 'new'
    m = re.match(r'(return|new|throw)\s', expr)
    if m:
        return None
    return start

def rewrite(src, member_vars, class_vars):
    out = src
    n = 0
    for meth, repl in CALLS.items():
        pat = '.' + meth + '('
        pos = 0
        while True:
            k = out.find(pat, pos)
            if k < 0:
                break
            st = receiver_start(out, k)
            if st is None or st == k:
                pos = k + len(pat); continue
            recv = out[st:k].strip()
            if recv in ('HostRemap', 'super', 'this') or recv.endswith('HostRemap'):
                pos = k + len(pat); continue
            # empty arg list (e.g. getMethod()) is not a reflection lookup
            args_start = k + len(pat)
            if out[args_start] == ')':
                pos = args_start; continue
            new = 'HostRemap.%s(%s, ' % (repl, recv)
            out = out[:st] + new + out[args_start:]
            pos = st + len(new)
            n += 1
    # Class.forName
    c = out.count('Class.forName(')
    out = out.replace('Class.forName(', 'HostRemap.forName(')
    # forName with single arg -> keep Class.forName (module own classes)
    out = re.sub(r'HostRemap\.forName\(("[^"]*")\)', r'Class.forName(\1)', out)
    n += c
    # member getName()
    for v in member_vars:
        out, k = re.subn(r'(?<![\w.])%s\.getName\(\)' % re.escape(v), 'HostRemap.name(%s)' % v, out)
        n += k
    for v in class_vars:
        out, k = re.subn(r'(?<![\w.])%s\.getName\(\)' % re.escape(v), 'HostRemap.className(%s)' % v, out)
        n += k
        out, k = re.subn(r'(?<![\w.])%s\.getSimpleName\(\)' % re.escape(v), 'HostRemap.simpleName(%s)' % v, out)
        n += k
    return out, n

def collect_vars(src):
    members = set(re.findall(r'\b(?:Method|Field|Member|Constructor<\?>)\s+(\w+)\s*[:=;,)]', src))
    classes = set(re.findall(r'\bClass<[^>]*>\s+(\w+)\s*[:=;,)]', src))
    return members, classes

def class_expr_getname(src):
    # x.getType().getName() etc.
    pat = re.compile(r'([\w.]+(?:\([^()]*\))?\.(?:getType|getReturnType|getClass|getSuperclass|getDeclaringClass|getComponentType)\(\))\.getName\(\)')
    src, a = pat.subn(r'HostRemap.className(\1)', src)
    pat2 = re.compile(r'([\w.]+(?:\([^()]*\))?\.(?:getType|getReturnType|getClass|getSuperclass|getDeclaringClass|getComponentType)\(\))\.getSimpleName\(\)')
    src, b = pat2.subn(r'HostRemap.simpleName(\1)', src)
    return src, a + b

def main(d):
    total = 0
    for f in sorted(os.listdir(d)):
        if not f.endswith('.java') or f in SKIP:
            continue
        p = os.path.join(d, f)
        s = open(p).read()
        mv, cv = collect_vars(s)
        t, n = rewrite(s, mv, cv)
        t, k = class_expr_getname(t)
        n += k
        if t != s:
            if 'HostRemap' in t and t != s:
                open(p, 'w').write(t)
            print('%-40s %d' % (f, n))
            total += n
    print('total', total)

main(sys.argv[1])
