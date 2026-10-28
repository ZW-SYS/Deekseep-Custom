import sys,json,pickle
sys.argv=[sys.argv[0]]+sys.argv[1:]
import fuzzy as F
names=sys.argv[1:]
for n in names:
    a='L%s;'%n
    if a not in F.A: print(n,'not in A'); continue
    ca=F.A[a]
    pool=[b for b in F.B if b not in F.rm]
    sc=sorted(((F.score(a,b),b) for b in pool if F.block(F.B[b])==F.block(ca) or (ca['refs'] and F.mapped(ca['refs'],F.m)&F.B[b]['refs'])),reverse=True)[:4]
    print('==',n,'sup',ca['sup'],'ifs',ca['ifs'],'nf',len(ca['fields']),'nm',len(ca['methods']),'str',sorted(ca['strings'])[:6])
    for s,b in sc:
        cb=F.B[b]; print('   %.2f %s sup=%s nf=%d nm=%d str=%s'%(s,b,cb['sup'],len(cb['fields']),len(cb['methods']),sorted(cb['strings'])[:4]))
