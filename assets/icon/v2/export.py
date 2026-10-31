from whale import *
R="/workspace/work/Deekseep/module-universal/res/"
mk=fg_mask(); bgi=bg(S)
dens={"mdpi":1,"hdpi":1.5,"xhdpi":2,"xxhdpi":3,"xxxhdpi":4}
sq,_=compose(1024); rd,_=compose(1024,True)
for n,k in dens.items():
    a=int(108*k); l=int(48*k)
    fg=Image.new("RGBA",(S,S),(255,255,255,0)); fg.putalpha(mk)
    fg.resize((a,a),Image.LANCZOS).save(R+f"mipmap-{n}/ic_launcher_foreground.png")
    bgi.resize((a,a),Image.LANCZOS).save(R+f"mipmap-{n}/ic_launcher_background.png")
    sq.resize((l,l),Image.LANCZOS).save(R+f"mipmap-{n}/ic_launcher.png")
    rd.resize((l,l),Image.LANCZOS).save(R+f"mipmap-{n}/ic_launcher_round.png")
sq.resize((512,512),Image.LANCZOS).save("../deekseep_icon_512.png")
