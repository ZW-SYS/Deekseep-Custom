import json,gzip,re
mm=json.load(open('/tmp/mmap.json'))
def j(d): return d.replace(' ','')
lines=[]
for a,e in sorted(mm.items()):
    lines.append('C %s %s'%(a[1:-1],e['to'][1:-1]))
    for o,n in e['f'].items():
        if n.endswith('~') or o==n: 
            if not n.endswith('~'): lines.append('F %s'%o)  # identity
            continue
        lines.append('F %s %s'%(o,n))
    for o,n in e['m'].items():
        if n.endswith('~'): continue
        on,od=o.split('(',1); nn,nd=n.split('(',1)
        if on.startswith('<'): continue
        lines.append('M %s (%s %s'%(on,j(od),nn))
data='\n'.join(lines)+'\n'
open('/tmp/hostmap287.txt','w').write(data)
gz=gzip.compress(data.encode(),9); open('/tmp/hostmap287.txt.gz','wb').write(gz)
print(len(lines),len(data),len(gz))
