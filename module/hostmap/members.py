#!/usr/bin/env python3
"""Member-level mapping (fields/methods) for matched class pairs, 257 -> 287.
Output /tmp/mmap.json: { "Lold;": {"to": "Lnew;", "f": {old: new}, "m": {"old(desc)": "new(desc)"}} }
Descriptors are rewritten through the class map so old/new protos can be compared exactly.
"""
import pickle, json, re, collections, sys
sys.path.insert(0, '.')
from extract import norm, norm_proto, is_obf

A = pickle.load(open('/tmp/c257.pkl', 'rb'))
B = pickle.load(open('/tmp/c287.pkl', 'rb'))
cm = {k: v[0] for k, v in json.load(open(sys.argv[1] if len(sys.argv) > 1 else '/tmp/cmap2.json')).items()}
TYPE = re.compile(r'(\[*)(L[^;]+;)')

def xl(desc):
    """translate an old descriptor through the class map; unmapped obf -> '?'"""
    def r(mo):
        t = mo.group(2)
        if is_obf(t):
            return mo.group(1) + cm.get(t, '?')
        return mo.group(0)
    return TYPE.sub(r, desc)

out = {}
for a, b in cm.items():
    ca, cb = A[a], B[b]
    fm = {}
    # exact translated descriptor groups, in declaration order
    ga = collections.defaultdict(list); gb = collections.defaultdict(list)
    for n, d, acc in ca['fields']: ga[(xl(d), acc & 8)].append(n)
    for n, d, acc in cb['fields']: gb[(d, acc & 8)].append(n)
    for k, la in ga.items():
        lb = gb.get(k, [])
        if len(la) == len(lb):
            fm.update(zip(la, lb))
        elif len(la) == 1 and len(lb) == 1:
            fm[la[0]] = lb[0]
    # fallback: normalized group
    na = collections.defaultdict(list); nb = collections.defaultdict(list)
    for n, d, acc in ca['fields']:
        if n not in fm: na[(norm(d), acc & 8)].append(n)
    used = set(fm.values())
    for n, d, acc in cb['fields']:
        if n not in used: nb[(norm(d), acc & 8)].append(n)
    for k, la in na.items():
        lb = nb.get(k, [])
        if len(la) == len(lb):
            for x, y in zip(la, lb): fm[x] = y + '~'   # '~' = lower confidence
    mm = {}
    ga = collections.defaultdict(list); gb = collections.defaultdict(list)
    for m in ca['methods']: ga[(xl(m['desc']), m['acc'] & 8)].append(m)
    for m in cb['methods']: gb[(m['desc'], m['acc'] & 8)].append(m)
    for k, la in ga.items():
        lb = gb.get(k, [])
        if not lb: continue
        if len(la) == 1 and len(lb) == 1:
            mm[la[0]['name'] + la[0]['desc']] = lb[0]['name'] + lb[0]['desc']
            continue
        # disambiguate by strings, then by length, then order
        left = list(lb)
        for x in la:
            best = None
            for y in left:
                s = (len(set(x['strings']) & set(y['strings'])) * 10 if x['strings'] else 0) - abs(x['len'] - y['len']) / max(1, x['len'])
                if best is None or s > best[0]: best = (s, y)
            if best:
                tag = '' if len(la) == len(lb) else '~'
                mm[x['name'] + x['desc']] = best[1]['name'] + best[1]['desc'] + ('~' if best[0] < 0 and not x['strings'] else tag)
                left.remove(best[1])
    out[a] = {'to': b, 'f': fm, 'm': mm}
json.dump(out, open('/tmp/mmap.json', 'w'))
print('classes', len(out))
