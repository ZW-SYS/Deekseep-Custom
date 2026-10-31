"""Deekseep launcher icon v2: stylised whale + hook. GPL-3.0 project asset."""
from PIL import Image, ImageDraw, ImageFilter
import math, sys
S=1024; SS=4; W=S*SS; u=W/108.0
def bez(p0,p1,p2,p3,n=60):
    return [tuple((1-t)**3*a+3*(1-t)**2*t*b+3*(1-t)*t*t*c+t**3*d for a,b,c,d in zip(p0,p1,p2,p3)) for t in [i/n for i in range(n+1)]]
def P(*pts): return [(x*u,y*u) for x,y in pts]
def path(segs):
    out=[]
    for s in segs: out+=bez(*P(*s))
    return out
def bg(size):
    img=Image.new("RGB",(size,size)); px=img.load()
    c1=(86,120,255); c2=(30,52,170)
    for y in range(size):
        for x in range(size):
            t=min(1,max(0,(0.35*x+y)/(1.35*size)))
            px[x,y]=tuple(int(c1[i]+(c2[i]-c1[i])*t) for i in range(3))
    # soft light blob
    g=Image.new("L",(size,size),0); ImageDraw.Draw(g).ellipse([-.2*size,-.35*size,.8*size,.45*size],fill=70)
    g=g.filter(ImageFilter.GaussianBlur(size*.12))
    img=Image.composite(Image.new("RGB",(size,size),(150,180,255)),img,g)
    return img
def fg_mask():
    m=Image.new("L",(W,W),0); d=ImageDraw.Draw(m)
    body=path([
        ((84,54),(84,41),(72,35),(60,36)),      # snout top -> back
        ((60,36),(48,37),(40,44),(34,46)),      # back -> tail stock
        ((34,46),(30,47),(27,44),(26,38)),      # stock curls up
        ((26,38),(22,36),(18,32),(16,27)),      # upper-left fluke tip
        ((16,27),(22,28),(26,31),(28,33)),      # fluke notch
        ((28,33),(29,29),(31,25),(35,22)),      # right fluke tip
        ((35,22),(35,28),(34,33),(33,37)),
        ((33,37),(35,42),(38,44),(40,46)),
        ((40,46),(46,60),(58,66),(68,65)),      # belly
        ((68,65),(79,64),(84,60),(84,54)),
    ])
    d.polygon(body,fill=255)
    # spout
    for cx,cy,r in [(66,25,2.3),(61,27.5,1.7),(71,27.5,1.7)]:
        d.ellipse(P((cx-r,cy-r),(cx+r,cy+r)),fill=255)
        d.polygon(P((cx-r*.97,cy-r*.25),(cx,cy-r*2.4),(cx+r*.97,cy-r*.25)),fill=255)
    cut=Image.new("L",(W,W),0); c=ImageDraw.Draw(cut)
    c.ellipse(P((72.5,46.3),(75.9,49.7)),fill=255)
    c.line(path([((83.8,55.5),(78,57.6),(70,58.2),(62,57))]),fill=255,width=int(1.5*u),joint="curve")
    m=Image.composite(Image.new("L",(W,W),0),m,cut)
    # badge with hook, bottom-right
    bx,by,br=77,75,9.5
    ring=Image.new("L",(W,W),0); ImageDraw.Draw(ring).ellipse(P((bx-br-2.2,by-br-2.2),(bx+br+2.2,by+br+2.2)),fill=255)
    m=Image.composite(Image.new("L",(W,W),0),m,ring)
    ImageDraw.Draw(m).ellipse(P((bx-br,by-br),(bx+br,by+br)),fill=255)
    hk=Image.new("L",(W,W),0); h=ImageDraw.Draw(hk); w=int(2.1*u)
    h.line(P((bx+1.5,by-6),(bx+1.5,by+1.5)),fill=255,width=w)
    h.arc(P((bx-4.5,by-2.5),(bx+1.5+1.05,by+5.5)),start=0,end=180,fill=255,width=w)
    h.ellipse(P((bx+1.5-2.1,by-8.4),(bx+1.5+2.1,by-4.2)),outline=255,width=int(1.4*u))
    h.polygon(P((bx-5.6,by+2.2),(bx-3.4,by-2.4),(bx-1.6,by+1.6)),fill=255)
    m=Image.composite(Image.new("L",(W,W),0),m,hk)
    bb=m.getbbox(); crop=m.crop(bb)
    tgt=60*u  # fit inside the 66dp safe zone
    k=tgt/max(crop.size); crop=crop.resize((int(crop.size[0]*k),int(crop.size[1]*k)),Image.LANCZOS)
    out=Image.new("L",(W,W),0); out.paste(crop,((W-crop.size[0])//2,(W-crop.size[1])//2))
    return out.resize((S,S),Image.LANCZOS)
def compose(size,round_=False):
    base=bg(S).convert("RGBA")
    mk=fg_mask()
    sh=mk.filter(ImageFilter.GaussianBlur(14)).point(lambda v:int(v*.35))
    shadow=Image.new("RGBA",(S,S),(10,20,80,0)); shadow.putalpha(sh)
    base.alpha_composite(shadow,(0,10))
    white=Image.new("RGBA",(S,S),(255,255,255,0)); white.putalpha(mk)
    base.alpha_composite(white)
    msk=Image.new("L",(S*2,S*2),0); dm=ImageDraw.Draw(msk)
    if round_: dm.ellipse([0,0,S*2-1,S*2-1],fill=255)
    else: dm.rounded_rectangle([0,0,S*2-1,S*2-1],radius=int(S*2*.23),fill=255)
    base.putalpha(msk.resize((S,S),Image.LANCZOS))
    return base.resize((size,size),Image.LANCZOS), mk
if __name__=="__main__":
    img,mk=compose(512); img.save("icon_512.png")
    r,_=compose(512,True); r.save("icon_round_512.png")
    pv=Image.new("RGBA",(1100,560),(240,242,248,255))
    pv.alpha_composite(img,(30,24)); pv.alpha_composite(r.resize((256,256)),(580,24))
    for i,s in enumerate([48,72,96,144]):
        pv.alpha_composite(img.resize((s,s),Image.LANCZOS),(580+sum([48,72,96,144][:i])+i*20,330))
    pv.save("preview.png")
