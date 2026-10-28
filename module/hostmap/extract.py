#!/usr/bin/env python3
"""Dump per-class structural fingerprints from an APK's dex files (for 257 -> 287 remapping)."""
import sys, re, json, zipfile, pickle
from androguard.core.dex import DEX

OBF = re.compile(r'^L[a-z0-9]{1,4}(\$[A-Za-z0-9_]+)*;$')

def is_obf(t):
    base = t.lstrip('[')
    return bool(OBF.match(base))

def norm(t):
    pre = len(t) - len(t.lstrip('['))
    base = t[pre:]
    return '[' * pre + ('O' if OBF.match(base) else base)

def norm_proto(desc):
    # desc like (Ljava/lang/String;I)V
    m = re.match(r'\((.*)\)(.*)', desc)
    args = re.findall(r'\[*(?:L[^;]+;|[ZBSCIJFDV])', m.group(1))
    return '(' + ''.join(norm(a) for a in args) + ')' + norm(m.group(2))

def run(apk, out):
    z = zipfile.ZipFile(apk)
    classes = {}
    for n in sorted(z.namelist()):
        if not re.match(r'classes\d*\.dex$', n):
            continue
        d = DEX(z.read(n))
        for c in d.get_classes():
            name = c.get_name()
            if not is_obf(name):
                continue
            info = {
                'sup': c.get_superclassname(),
                'ifs': list(c.get_interfaces() or []),
                'acc': c.get_access_flags(),
                'fields': [],
                'methods': [],
                'strings': set(),
                'refs': set(),     # obf classes referenced
                'ext': set(),      # non-obf methods/fields referenced
            }
            for f in c.get_fields():
                info['fields'].append((f.get_name(), f.get_descriptor(), f.get_access_flags()))
            for m in c.get_methods():
                desc = m.get_descriptor()
                mi = {'name': m.get_name(), 'desc': desc, 'acc': m.get_access_flags(),
                      'len': 0, 'strings': [], 'calls': []}
                code = m.get_code()
                if code:
                    try:
                        for ins in m.get_instructions():
                            op = ins.get_name()
                            if op.startswith('const-string'):
                                s = ins.get_output().split(', ', 1)[-1].strip()
                                if s.startswith("'") and s.endswith("'"):
                                    s = s[1:-1]
                                mi['strings'].append(s)
                                info['strings'].add(s)
                            elif op.startswith('invoke') or op.startswith('iget') or op.startswith('sget') \
                                    or op.startswith('iput') or op.startswith('sput') or op.startswith('new-instance') \
                                    or op.startswith('const-class') or op.startswith('check-cast') or op.startswith('instance-of'):
                                o = ins.get_output()
                                tok = o.split(', ')[-1]
                                mi['calls'].append(tok)
                                for t in re.findall(r'\[*L[^;]+;', tok):
                                    if is_obf(t):
                                        info['refs'].add(t.lstrip('['))
                                if '->' in tok:
                                    owner = tok.split('->')[0]
                                    if not is_obf(owner):
                                        info['ext'].add(tok)
                            mi['len'] += 1
                    except Exception as e:
                        pass
                info['methods'].append(mi)
            classes[name] = info
    pickle.dump(classes, open(out, 'wb'))
    print(apk, len(classes))

if __name__ == '__main__':
    run(sys.argv[1], sys.argv[2])
