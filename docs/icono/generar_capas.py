"""Separa klipo_icon.png en capas de adaptive icon (primer plano y monocromo)."""
import sys
from pathlib import Path
from PIL import Image

origen, destino = Path(sys.argv[1]), Path(sys.argv[2])
img = Image.open(origen).convert("RGB")
w, h = img.size

# Colores del degradado del fondo (esquinas, algo hacia dentro)
m = w // 40
print("fondo_inicio", "#%02X%02X%02X" % img.getpixel((m, m)))
print("fondo_fin", "#%02X%02X%02X" % img.getpixel((w - m, h - m)))

# Máscara del símbolo amarillo: el fondo naranja tiene G bajo, el amarillo G alto
lo, hi = 170, 230
mascara = Image.new("L", (w, h))
px, mpx = img.load(), mascara.load()
for y in range(h):
    for x in range(w):
        g = px[x, y][1]
        mpx[x, y] = 0 if g <= lo else 255 if g >= hi else int((g - lo) * 255 / (hi - lo))

caja = mascara.getbbox()
print("caja", caja)
simbolo = mascara.crop(caja)

# Lienzo xxxhdpi (108dp * 4); el símbolo ocupa 50dp para quedar dentro de la zona segura
lado = 432
tam = round(lado * 46 / 108)
sw, sh = simbolo.size
escala = tam / max(sw, sh)
simbolo = simbolo.resize((round(sw * escala), round(sh * escala)), Image.LANCZOS)
pos = ((lado - simbolo.width) // 2, (lado - simbolo.height) // 2)

amarillo = img.getpixel(((caja[0] + caja[2]) // 2, caja[1] + 5))
print("amarillo", "#%02X%02X%02X" % amarillo)

for nombre, color in (("ic_launcher_primer_plano.png", amarillo), ("ic_launcher_monocromo.png", (255, 255, 255))):
    capa = Image.new("RGBA", (lado, lado), (0, 0, 0, 0))
    solido = Image.new("RGBA", simbolo.size, color + (255,))
    capa.paste(solido, pos, simbolo)
    capa.save(destino / nombre, optimize=True)
    print("guardado", nombre)
