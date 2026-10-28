#!/usr/bin/env python3
"""Match obfuscated classes between DeepSeek 2.4.1 (code257) and 2.7.1 (code287).

Phases: unique-string votes -> unique structural signature -> reference propagation.
Output: /tmp/cmap.json {old: [new, score, reason]}
"""
import pickle, re, json, collections, sys
sys.path.insert(0, '.')
from extract import norm, norm_proto, is_obf

A = pickle.load(open('/tmp/c257.pkl', 'rb'))
B = pickle.load(open('/tmp/c287.pkl', 'rb'))

def sig(c):
    f = sorted(norm(d) + ('S' if a & 8 else '') for _, d, a in c['fields'])
    m = sorted(norm_proto(x['desc']) + ('S' if x['acc'] & 8 else '') + (x['name'] if x['name'].startswith('<') or len(x['name']) > 3 else '') for x in c['methods'])
    return (norm(c['sup']), tuple(sorted(norm(i) for i in c['ifs'])), tuple(f), tuple(m))

def coarse(c):
    return (norm(c['sup']), tuple(sorted(norm(i) for i in c['ifs'])), len(c['fields']), len(c['methods']))

match = {}   # old -> new
rmatch = {}
reason = {}

def accept(a, b, why):
    if a in match or b in rmatch:
        return False
    match[a] = b; rmatch[b] = a; reason[a] = why
    return True

# ---- Phase 0: inner classes / names that look the same? (R8 renames all; skip)

# ---- Phase A: unique strings
def str_index(C):
    idx = collections.defaultdict(set)
    for n, c in C.items():
        for s in c['strings']:
            if len(s) >= 4:
                idx[s].add(n)
    return idx
ia, ib = str_index(A), str_index(B)
votes = collections.defaultdict(collections.Counter)
for s, na in ia.items():
    nb = ib.get(s)
    if nb and len(na) == 1 and len(nb) == 1:
        votes[next(iter(na))][next(iter(nb))] += 1
# global greedy: strongest evidence first; reverse direction must agree too
rvotes = collections.defaultdict(collections.Counter)
for a, cnt in votes.items():
    for b, v in cnt.items(): rvotes[b][a] += v
cands = []
for a, cnt in votes.items():
    (b, v), *rest = cnt.most_common()
    if rest and v < 2 * rest[0][1]: continue
    (a2, v2), *rrest = rvotes[b].most_common()
    if a2 != a and v2 >= v: continue
    if coarse(A[a])[0] == coarse(B[b])[0] or v >= 3:
        cands.append((v, a, b))
for v, a, b in sorted(cands, reverse=True):
    accept(a, b, 'str%d' % v)
print('phase A', len(match))

# ---- Phase B: unique exact structural signature
def sig_index(C, skip):
    idx = collections.defaultdict(list)
    for n, c in C.items():
        if n in skip: continue
        idx[sig(c)].append(n)
    return idx
def phaseB():
    sa, sb = sig_index(A, match), sig_index(B, rmatch)
    k = 0
    for s, la in sa.items():
        lb = sb.get(s)
        if lb and len(la) == 1 and len(lb) == 1:
            # avoid trivial signatures (empty classes, lambdas with only invoke)
            if len(s[2]) + len(s[3]) >= 3:
                k += accept(la[0], lb[0], 'sig')
    return k
print('phase B', phaseB(), len(match))

# ---- Phase C: propagation along typed slots of matched classes
TYPE = re.compile(r'\[*L[^;]+;')
def slots(c):
    out = []
    out.append(('sup', c['sup']))
    for i, t in enumerate(sorted(c['ifs'], key=norm)):
        out.append(('if%s' % norm(t), t))
    # fields grouped by normalized desc, in declaration order
    for name, d, a in c['fields']:
        out.append(('f', d))
    for m in c['methods']:
        for t in TYPE.findall(m['desc']):
            out.append(('m' + norm_proto(m['desc']) + str(m['acc'] & 8), t))
    return out

def align(ca, cb):
    """Yield (oldType, newType) pairs from structurally aligned slots."""
    ga, gb = collections.defaultdict(list), collections.defaultdict(list)
    for k, t in slots(ca): ga[k].append(t)
    for k, t in slots(cb): gb[k].append(t)
    # fields: align by normalized descriptor groups
    fa = collections.defaultdict(list); fb = collections.defaultdict(list)
    for _, d, a in ca['fields']: fa[norm(d) + str(a & 8)].append(d)
    for _, d, a in cb['fields']: fb[norm(d) + str(a & 8)].append(d)
    for k in fa:
        if k in fb and len(fa[k]) == len(fb[k]) == 1:
            yield fa[k][0], fb[k][0]
    for k in ga:
        if k == 'f': continue
        if k in gb and len(ga[k]) == len(gb[k]) and len(ga[k]) <= 6:
            # only when the group is a single method/slot occurrence pattern
            na = ga[k]; nb = gb[k]
            if len(set(na)) == len(na) and len(set(nb)) == len(nb) and len(na) == 1:
                yield na[0], nb[0]
            elif k == 'sup' or k.startswith('if'):
                for x, y in zip(na, nb): yield x, y
    # call-site references aligned by string-bearing methods
    ma = {tuple(m['strings']): m for m in ca['methods'] if m['strings']}
    mb = {tuple(m['strings']): m for m in cb['methods'] if m['strings']}
    for key in ma:
        if key in mb:
            xa = [t for t in ma[key]['calls']]; xb = [t for t in mb[key]['calls']]
            if len(xa) == len(xb):
                for p, q in zip(xa, xb):
                    ta = TYPE.findall(p); tb = TYPE.findall(q)
                    if len(ta) == len(tb):
                        for x, y in zip(ta, tb): yield x, y

for it in range(8):
    votes = collections.defaultdict(collections.Counter)
    for a, b in list(match.items()):
        for x, y in align(A[a], B[b]):
            x = x.lstrip('['); y = y.lstrip('[')
            if is_obf(x) and is_obf(y) and x in A and y in B:
                votes[x][y] += 1
    k = 0
    cands = []
    for x, cnt in votes.items():
        if x in match: continue
        (y, v), *rest = cnt.most_common()
        if y in rmatch: continue
        if rest and v < 2 * rest[0][1]: continue
        if coarse(A[x])[:2] != coarse(B[y])[:2] and v < 3: continue
        cands.append((v, x, y))
    for v, x, y in sorted(cands, reverse=True):
        k += accept(x, y, 'prop%d' % v)
    k += phaseB()
    print('iter', it, 'new', k, 'total', len(match))
    if k == 0: break

json.dump({a: [b, reason[a]] for a, b in match.items()}, open('/tmp/cmap.json', 'w'))
print('done', len(match), 'of', len(A))
