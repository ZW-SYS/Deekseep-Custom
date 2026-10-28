import json,re
m=json.load(open('/tmp/cmap2.json')); out={}
for a,(b,w) in m.items():
    if w.startswith('fz'):
        s1,s2=map(float,w[2:].split('/'))
        if s1<4 or s1<1.4*s2: continue
    out[a]=[b,w]
json.dump(out,open('/tmp/cmap2.json','w')); print(len(m),'->',len(out))
