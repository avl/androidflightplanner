import Image,ImageDraw,ImageFont

font="FreeMonoBold.ttf"
font=ImageFont.truetype(font,16)
canvas=Image.new("RGB",(256,256),(0,0,0))
draw=ImageDraw.Draw(canvas)

i=0
for y in xrange(0,256,16):
    for x in xrange(0,16*16,16):
        c=unichr(i)
        draw.text((x,y),c,font=font,fill=(255,255,255))
        i+=1
canvas.save("font.png")

