#!/usr/bin/env python3
"""Second pass: fuzzy-score unmatched classes using mapped neighbourhood + strings + shape."""
import pickle, json, collections, sys
sys.path.insert(0, '.')
from extract import norm, norm_proto, is_obf

A = pickle.load(open('/tmp/c257.pkl', 'rb'))
B = pickle.load(open('/tmp/c287.pkl', 'rb'))
m = {k: v[0] for k, v in json.load(open('/tmp/cmap2.json')).items()}
why = {k: v[1] for k, v in json.load(open('/tmp/cmap2.json')).items()}
rm = {v: k for k, v in m.items()}

# reverse refs
def rev(C):
    r = collections.defaultdict(set)
    for n, c in C.items():
        for t in c['refs']:
            r[t].add(n)
    return r
ra, rb = rev(A), rev(B)

def tsig(c):
    return collections.Counter(norm_proto(x['desc']) + str(x['acc'] & 8) for x in c['methods']) + \
           collections.Counter(norm(d) + str(a & 8) for _, d, a in c['fields'])

def mapped(s, mp):
    return {mp[x] for x in s if x in mp}

def jac(a, b):
    if not a and not b: return 0.0
    return len(a & b) / max(1, len(a | b))

def score(a, b):
    ca, cb = A[a], B[b]
    s = 0.0
    # neighbourhood: outgoing and incoming refs through current mapping
    oa = mapped(ca['refs'], m); ob = cb['refs'] & set(rm)
    ia = mapped(ra.get(a, ()), m); ib = rb.get(b, set()) & set(rm)
    s += 3 * jac(oa, ob) + 3 * jac(ia, ib)
    s += 2 * jac(set(ca['strings']), set(cb['strings']))
    s += 1 * jac(ca['ext'], cb['ext'])
    ta, tb = tsig(ca), tsig(cb)
    inter = sum((ta & tb).values()); uni = sum((ta | tb).values())
    s += 2 * (inter / uni if uni else 0)
    if norm(ca['sup']) == norm(cb['sup']): s += 0.5
    if ca['sup'] in m and m[ca['sup']] == cb['sup']: s += 1
    return s

def block(c):
    return (norm(c['sup']), tuple(sorted(norm(i) for i in c['ifs'])))

def run(targets, rounds=3):
    added = {}
    for r in range(rounds):
        bb = collections.defaultdict(list)
        for n, c in B.items():
            if n not in rm: bb[block(c)].append(n)
        new = 0
        for a in targets:
            if a in m or a not in A: continue
            cands = bb.get(block(A[a]), [])
            # also candidates via mapped neighbours (handles changed super/ifs)
            extra = set()
            for x in A[a]['refs']:
                if x in m:
                    extra |= {y for y in rb.get(m[x], ()) if y not in rm}
            for x in ra.get(a, ()):
                if x in m:
                    extra |= {y for y in B[m[x]]['refs'] if y not in rm and y in B}
            pool = set(cands) | extra if len(cands) < 4000 else extra
            best = sorted(((score(a, b), b) for b in pool), reverse=True)[:2]
            if not best: continue
            s1 = best[0][0]; s2 = best[1][0] if len(best) > 1 else 0
            if s1 >= 2.5 and s1 - s2 >= 0.6:
                b = best[0][1]
                m[a] = b; rm[b] = a; why[a] = 'fz%.1f/%.1f' % (s1, s2); added[a] = (b, s1, s2); new += 1
        print('round', r, 'new', new)
        if not new: break
    return added

if __name__ == "__main__" and len(sys.argv) > 1 and sys.argv[0].endswith("fuzzy.py"):
    targets = [l.strip() for l in open(sys.argv[1])] if len(sys.argv) > 1 else [n for n in A if n not in m]
    added = run(targets)
    json.dump({a: [b, why[a]] for a, b in m.items()}, open('/tmp/cmap2.json', 'w'))
    print('total', len(m))
