import json,pickle,sys
A=pickle.load(open('/tmp/c257.pkl','rb'));B=pickle.load(open('/tmp/c287.pkl','rb'))
src=sys.argv[1]; dst=sys.argv[2]
m={k:v for k,v in json.load(open(src)).items()}
mm={k:v[0] for k,v in m.items()}
bad=[];keep={}
for a,(b,why) in m.items():
    ra=[x for x in A[a]['refs'] if x in mm and x!=a]
    if len(ra)<3: keep[a]=[b,why]; continue
    agree=sum(1 for x in ra if mm[x] in B[b]['refs'])/len(ra)
    import re
    v=int(re.sub(r'[^0-9]','',why.split('/')[0]) or 0) if not why.startswith('fz') else 0
    strong=(why.startswith('prop') and v>=20) or (why.startswith('str') and v>=5)
    if agree<0.5 and not strong: bad.append((agree,a,b,why))
    else: keep[a]=[b,why]
json.dump(keep,open(dst,'w'))
print('kept',len(keep),'dropped',len(bad))
for x in sorted(bad)[:15]: print(x)
