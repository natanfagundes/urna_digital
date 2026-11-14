"""
Gera os PDFs da documentação (docs/pdf/) a partir dos arquivos Markdown.

Converte o Markdown para HTML com um estilo próprio para impressão e usa o
Google Chrome (ou o Microsoft Edge) em modo headless para gerar o PDF.
Não precisa de nenhuma biblioteca Python além da padrão.

Uso:  python ferramentas/gerar_pdfs.py
"""
import html
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
DOCS = RAIZ / "docs"
SAIDA = DOCS / "pdf"
REPO = "https://github.com/natanfagundes/urna_digital"

DOCUMENTOS = [
    {
        "origem": "guia-para-leigos.md",
        "destino": "Urna-Digital-Guia-Simples.pdf",
        "titulo": "Guia Simples",
        "subtitulo": "Como usar a urna, explicado sem termos técnicos",
        "publico": "Para alunos, professores, funcionários da escola e quem for testar o programa",
        "leigo": True,
    },
    {
        "origem": "documentacao-tecnica.md",
        "destino": "Urna-Digital-Documentacao-Tecnica.pdf",
        "titulo": "Documentação Técnica",
        "subtitulo": "Arquitetura, funcionamento interno, build, testes e manutenção",
        "publico": "Para desenvolvedores, avaliadores e mantenedores do projeto",
        "leigo": False,
    },
]

NAVEGADORES = [
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    "google-chrome", "chromium", "chromium-browser", "microsoft-edge",
]


# ----------------------------------------------------------------------
# Markdown -> HTML (o subconjunto usado na documentação do projeto)
# ----------------------------------------------------------------------

def ancora(texto):
    """Mesmo formato de âncora do GitHub: minúsculas, sem pontuação, espaços viram '-'."""
    texto = re.sub(r"<[^>]+>", "", texto).strip().lower()
    texto = re.sub(r"[^\w\- ]", "", texto)
    return texto.replace(" ", "-")


def ajustar_link(href):
    if href.startswith(("http://", "https://", "#", "mailto:")):
        return href
    caminho, _, fragmento = href.partition("#")
    destino = f"{REPO}/blob/main/docs/{caminho}" if caminho.endswith(".md") else f"{REPO}/tree/main/docs/{caminho}"
    return destino + ("#" + fragmento if fragmento else "")


def inline(texto):
    codigos = []

    def guardar_codigo(m):
        codigos.append("<code>" + html.escape(m.group(1)) + "</code>")
        return f"\x00{len(codigos) - 1}\x00"

    texto = re.sub(r"`([^`]+)`", guardar_codigo, texto)
    texto = html.escape(texto, quote=False)
    texto = re.sub(r"!\[([^\]]*)\]\(([^)]+)\)",
                   lambda m: f'<img src="{m.group(2)}" alt="{m.group(1)}">', texto)
    texto = re.sub(r"\[([^\]]+)\]\(([^)]+)\)",
                   lambda m: f'<a href="{ajustar_link(m.group(2))}">{m.group(1)}</a>', texto)
    texto = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", texto)
    texto = re.sub(r"(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])", r"<em>\1</em>", texto)
    texto = re.sub(r"\x00(\d+)\x00", lambda m: codigos[int(m.group(1))], texto)
    return texto


MARCADOR_LISTA = re.compile(r"^(\s*)([-*]|\d+\.)\s+(.*)$")


def lista_html(linhas):
    """Converte linhas de uma lista (com sublistas por indentação) em HTML."""
    itens = []  # (indentação, ordenada, texto)
    for linha in linhas:
        m = MARCADOR_LISTA.match(linha)
        if m:
            itens.append([len(m.group(1)), m.group(2)[0].isdigit(), m.group(3).strip()])
        elif linha.strip() and itens:
            itens[-1][2] += " " + linha.strip()

    saida, pilha = [], []  # pilha de (indentação, tag)
    for indent, ordenada, texto in itens:
        tag = "ol" if ordenada else "ul"
        while pilha and indent < pilha[-1][0]:
            saida.append(f"</li></{pilha.pop()[1]}>")
        if not pilha or indent > pilha[-1][0]:
            pilha.append((indent, tag))
            saida.append(f"<{tag}><li>")
        else:
            saida.append("</li><li>")
        saida.append(inline(texto))
    while pilha:
        saida.append(f"</li></{pilha.pop()[1]}>")
    return "".join(saida)


def tabela_html(linhas):
    def celulas(linha):
        return [c.strip() for c in linha.strip().strip("|").split("|")]

    cabecalho = celulas(linhas[0])
    partes = ["<table><thead><tr>"]
    partes += [f"<th>{inline(c)}</th>" for c in cabecalho]
    partes.append("</tr></thead><tbody>")
    for linha in linhas[2:]:
        partes.append("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in celulas(linha)) + "</tr>")
    partes.append("</tbody></table>")
    return "".join(partes)


def markdown_html(texto, pular_titulo=True):
    linhas = texto.splitlines()
    saida, i = [], 0
    titulo_pulado = not pular_titulo
    while i < len(linhas):
        linha = linhas[i]
        if not linha.strip():
            i += 1
            continue
        if linha.startswith("```"):
            bloco = []
            i += 1
            while i < len(linhas) and not linhas[i].startswith("```"):
                bloco.append(linhas[i])
                i += 1
            i += 1
            saida.append("<pre><code>" + html.escape("\n".join(bloco)) + "</code></pre>")
            continue
        m = re.match(r"^(#{1,6})\s+(.*)$", linha)
        if m:
            nivel, conteudo = len(m.group(1)), m.group(2).strip()
            i += 1
            if nivel == 1 and not titulo_pulado:
                titulo_pulado = True  # o título vai para a capa
                continue
            saida.append(f'<h{nivel} id="{ancora(conteudo)}">{inline(conteudo)}</h{nivel}>')
            continue
        if re.match(r"^\s*-{3,}\s*$", linha):
            saida.append("<hr>")
            i += 1
            continue
        if linha.lstrip().startswith(">"):
            bloco = []
            while i < len(linhas) and linhas[i].lstrip().startswith(">"):
                bloco.append(linhas[i].lstrip()[1:].strip())
                i += 1
            paragrafos = " ".join(l if l else "\n" for l in bloco).split("\n")
            saida.append("<blockquote>" + "".join(f"<p>{inline(p.strip())}</p>" for p in paragrafos if p.strip())
                         + "</blockquote>")
            continue
        if linha.lstrip().startswith("|") and i + 1 < len(linhas) and re.match(r"^\s*\|[\s:|-]+\|\s*$", linhas[i + 1]):
            bloco = []
            while i < len(linhas) and linhas[i].lstrip().startswith("|"):
                bloco.append(linhas[i])
                i += 1
            saida.append(tabela_html(bloco))
            continue
        if MARCADOR_LISTA.match(linha):
            bloco = []
            while i < len(linhas):
                atual = linhas[i]
                if MARCADOR_LISTA.match(atual) or (atual.startswith((" ", "\t")) and atual.strip()):
                    bloco.append(atual)
                    i += 1
                elif not atual.strip() and i + 1 < len(linhas) and (
                        MARCADOR_LISTA.match(linhas[i + 1]) and linhas[i + 1].startswith((" ", "\t"))):
                    i += 1  # linha em branco dentro de uma lista aninhada
                else:
                    break
            saida.append(lista_html(bloco))
            continue
        m = re.match(r"^!\[([^\]]*)\]\(([^)]+)\)\s*$", linha.strip())
        if m:
            saida.append(f'<figure><img src="{m.group(2)}" alt="{m.group(1)}"><figcaption>{html.escape(m.group(1))}</figcaption></figure>')
            i += 1
            continue
        paragrafo = []
        while i < len(linhas) and linhas[i].strip() and not re.match(r"^(#{1,6}\s|```|\s*>|\s*\||\s*-{3,}\s*$)", linhas[i]) \
                and not MARCADOR_LISTA.match(linhas[i]):
            paragrafo.append(linhas[i].strip())
            i += 1
        saida.append("<p>" + inline(" ".join(paragrafo)) + "</p>")
    return "\n".join(saida)


# ----------------------------------------------------------------------
# Página HTML para impressão
# ----------------------------------------------------------------------

CSS = """
@page { size: A4; margin: 20mm 18mm 20mm 18mm;
        @bottom-left { content: "Urna Digital — %(titulo)s"; font: 8pt 'Segoe UI', Arial, sans-serif; color: #888; }
        @bottom-right { content: "Página " counter(page) " de " counter(pages); font: 8pt 'Segoe UI', Arial, sans-serif; color: #888; } }
@page :first { @bottom-left { content: none; } @bottom-right { content: none; } }
* { box-sizing: border-box; }
body { font-family: 'Segoe UI', 'Segoe UI Emoji', Arial, sans-serif; font-size: %(fonte)s; line-height: 1.5; color: #222; margin: 0; }
.capa { height: 252mm; display: flex; flex-direction: column; justify-content: center; page-break-after: always; }
.capa img { width: 46mm; margin-bottom: 10mm; }
.capa .faixa { width: 30mm; height: 3mm; background: #2E9E48; margin-bottom: 8mm; }
.capa h1 { font-size: 34pt; margin: 0; color: #1a1a1a; border: 0; }
.capa h2 { font-size: 22pt; margin: 2mm 0 6mm; color: #2E9E48; border: 0; font-weight: 600; }
.capa .sub { font-size: 13pt; color: #444; margin: 0 0 14mm; }
.capa .meta { font-size: 10pt; color: #555; border-top: 1px solid #ccc; padding-top: 5mm; }
.capa .meta p { margin: 1mm 0; }
h1, h2, h3 { color: #1a1a1a; break-after: avoid; page-break-after: avoid; line-height: 1.25; }
h2 { font-size: 16pt; margin: 9mm 0 3mm; padding-bottom: 1.5mm; border-bottom: 2px solid #2E9E48; }
h3 { font-size: 12.5pt; margin: 6mm 0 2mm; color: #2B2B2B; }
p { margin: 0 0 3mm; }
a { color: #1E5AA8; text-decoration: none; }
code { font-family: Consolas, 'Courier New', monospace; font-size: 0.88em; background: #F1F3F5; padding: 0.3mm 1mm; border-radius: 1mm; }
pre { background: #F6F8FA; border: 1px solid #E1E4E8; border-left: 3px solid #2E9E48; border-radius: 1.5mm;
      padding: 3mm 4mm; white-space: pre-wrap; word-break: break-word; font-size: 8.3pt; line-height: 1.4; break-inside: avoid; }
pre code { background: none; padding: 0; font-size: inherit; }
table { width: 100%%; border-collapse: collapse; margin: 2mm 0 5mm; font-size: 0.92em; break-inside: auto; }
tr { break-inside: avoid; }
th { background: #2B2B2B; color: #fff; text-align: left; padding: 1.6mm 2.2mm; font-weight: 600; }
td { border-bottom: 1px solid #E1E4E8; padding: 1.6mm 2.2mm; vertical-align: top; }
tbody tr:nth-child(even) td { background: #F8F9FA; }
blockquote { margin: 3mm 0 5mm; padding: 3mm 5mm; background: #FFF6D5; border-left: 4px solid #E8862A; border-radius: 1.5mm; break-inside: avoid; }
blockquote p { margin: 0 0 1.5mm; }
blockquote p:last-child { margin: 0; }
ul, ol { margin: 0 0 3mm; padding-left: 7mm; }
li { margin-bottom: 1mm; }
li > ul, li > ol { margin: 1mm 0 0; }
hr { border: 0; border-top: 1px dashed #ccc; margin: 6mm 0; }
figure { margin: 3mm 0 6mm; text-align: center; break-inside: avoid; }
figure img { max-width: 100%%; max-height: 105mm; border: 1px solid #ccc; border-radius: 1.5mm; }
figcaption { font-size: 8.5pt; color: #666; margin-top: 1.5mm; }
td img { max-width: 100%%; }
"""


def pagina(doc, corpo, icone):
    css = CSS % {"titulo": doc["titulo"], "fonte": "11.5pt" if doc["leigo"] else "10pt"}
    return f"""<!doctype html>
<html lang="pt-BR"><head><meta charset="utf-8">
<base href="{DOCS.as_uri()}/">
<title>Urna Digital — {html.escape(doc['titulo'])}</title>
<style>{css}</style></head>
<body>
<section class="capa">
  <img src="{icone.as_uri()}" alt="">
  <div class="faixa"></div>
  <h1>Urna Digital</h1>
  <h2>{html.escape(doc['titulo'])}</h2>
  <p class="sub">{html.escape(doc['subtitulo'])}</p>
  <div class="meta">
    <p><strong>{html.escape(doc['publico'])}</strong></p>
    <p>Projeto de Extensão — Engenharia de Software</p>
    <p>Versão 2.0 · <a href="{REPO}">{REPO.replace('https://', '')}</a></p>
  </div>
</section>
{corpo}
</body></html>"""


def desenhar_icone(destino):
    """Ícone da capa em SVG (o mesmo desenho do ícone do programa)."""
    teclas = "".join(
        f'<rect x="{160 + c * 24}" y="{64 + l * 28}" width="18" height="20" rx="5" fill="#9A9A9A"/>'
        for l in range(3) for c in range(3))
    destino.write_text(f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256">
<rect x="8" y="40" width="240" height="176" rx="18" fill="#2B2B2B"/>
<rect x="26" y="60" width="118" height="136" rx="7" fill="#F2F2F2"/>
<rect x="40" y="84" width="60" height="12" fill="#2B2B2B"/><rect x="40" y="108" width="88" height="10" fill="#2B2B2B"/>
<rect x="40" y="128" width="70" height="10" fill="#2B2B2B"/>{teclas}
<rect x="158" y="154" width="22" height="26" rx="5" fill="#FFFFFF"/>
<rect x="184" y="154" width="22" height="26" rx="5" fill="#E8862A"/>
<rect x="210" y="154" width="26" height="26" rx="5" fill="#2E9E48"/></svg>""", encoding="utf-8")


def encontrar_navegador():
    for candidato in NAVEGADORES:
        caminho = candidato if os.path.isabs(candidato) else shutil.which(candidato)
        if caminho and os.path.exists(caminho):
            return caminho
    sys.exit("Não encontrei o Google Chrome nem o Microsoft Edge para gerar o PDF.")


def main():
    navegador = encontrar_navegador()
    SAIDA.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        icone = tmp / "icone.svg"
        desenhar_icone(icone)
        for doc in DOCUMENTOS:
            markdown = (DOCS / doc["origem"]).read_text(encoding="utf-8")
            arquivo_html = tmp / (Path(doc["origem"]).stem + ".html")
            arquivo_html.write_text(pagina(doc, markdown_html(markdown), icone), encoding="utf-8")
            pdf = SAIDA / doc["destino"]
            subprocess.run([navegador, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                            "--allow-file-access-from-files", f"--user-data-dir={tmp / 'perfil'}",
                            f"--print-to-pdf={pdf}", arquivo_html.as_uri()],
                           check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=180)
            print(f"Gerado: {pdf.relative_to(RAIZ)} ({pdf.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
