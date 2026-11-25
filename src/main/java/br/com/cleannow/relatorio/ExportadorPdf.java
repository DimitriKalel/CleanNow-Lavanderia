package br.com.cleannow.relatorio;

import br.com.cleannow.excecao.PersistenciaException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportadorPdf implements ExportadorRelatorio {
    private static final BaseColor AZUL = new BaseColor(18, 82, 140);
    private static final BaseColor CINZA = new BaseColor(235, 240, 245);

    private static final Font TITULO = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, AZUL);
    private static final Font SUBTITULO = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font SECAO = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, AZUL);
    private static final Font TEXTO = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
    private static final Font NEGRITO = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);
    private static final Font CABECALHO = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
    private static final Font DESTAQUE = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, AZUL);

    private final Path pasta;

    public ExportadorPdf(Path pasta) {
        this.pasta = pasta;
    }

    @Override
    public String getFormato() {
        return "PDF";
    }

    @Override
    public String exportar(Documento doc, String nomeBase) {
        String carimbo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path arquivo = pasta.resolve(nomeBase + "_" + carimbo + ".pdf");
        try {
            Files.createDirectories(pasta);
            try (OutputStream out = Files.newOutputStream(arquivo)) {
                Document pdf = new Document(PageSize.A4, 40, 40, 40, 40);
                PdfWriter.getInstance(pdf, out);
                pdf.open();
                escrever(pdf, doc);
                pdf.close();
            }
            return "PDF gerado em: " + arquivo.toAbsolutePath();
        } catch (IOException | DocumentException e) {
            throw new PersistenciaException("Não foi possível gerar o PDF: " + e.getMessage(), e);
        }
    }

    private void escrever(Document pdf, Documento doc) throws DocumentException {
        Paragraph titulo = new Paragraph("CleanNow Lavanderia - " + doc.getTitulo(), TITULO);
        titulo.setSpacingAfter(4);
        pdf.add(titulo);
        if (doc.getSubtitulo() != null) {
            Paragraph sub = new Paragraph(doc.getSubtitulo(), SUBTITULO);
            sub.setSpacingAfter(10);
            pdf.add(sub);
        }
        for (Documento.Bloco b : doc.getBlocos()) {
            if (b instanceof Documento.Secao s) {
                Paragraph p = new Paragraph(s.titulo(), SECAO);
                p.setSpacingBefore(12);
                p.setSpacingAfter(6);
                pdf.add(p);
            } else if (b instanceof Documento.Paragrafo p) {
                pdf.add(new Paragraph(p.texto(), TEXTO));
            } else if (b instanceof Documento.Destaque d) {
                Paragraph p = new Paragraph(d.rotulo() + ": " + d.valor(), DESTAQUE);
                p.setAlignment(Element.ALIGN_RIGHT);
                p.setSpacingBefore(6);
                pdf.add(p);
            } else if (b instanceof Documento.Tabela t) {
                pdf.add(tabela(t));
            }
        }
        Paragraph rodape = new Paragraph("Gerado em " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), SUBTITULO);
        rodape.setSpacingBefore(16);
        pdf.add(rodape);
    }

    private PdfPTable tabela(Documento.Tabela t) {
        boolean campos = t.cabecalho().isEmpty();
        int colunas = campos ? 2 : t.cabecalho().size();
        PdfPTable tabela = new PdfPTable(colunas);
        tabela.setWidthPercentage(100);
        tabela.setSpacingBefore(4);
        tabela.setSpacingAfter(4);
        for (String c : t.cabecalho()) {
            PdfPCell cel = new PdfPCell(new Phrase(c, CABECALHO));
            cel.setBackgroundColor(AZUL);
            cel.setPadding(5);
            tabela.addCell(cel);
        }
        int i = 0;
        for (List<String> linha : t.linhas()) {
            for (int col = 0; col < colunas; col++) {
                String texto = col < linha.size() && linha.get(col) != null ? linha.get(col) : "";
                PdfPCell cel = new PdfPCell(new Phrase(texto, campos && col == 0 ? NEGRITO : TEXTO));
                cel.setPadding(4);
                if (i % 2 == 1) cel.setBackgroundColor(CINZA);
                tabela.addCell(cel);
            }
            i++;
        }
        if (t.linhas().isEmpty()) {
            PdfPCell vazio = new PdfPCell(new Phrase("(nenhum registro)", TEXTO));
            vazio.setColspan(colunas);
            vazio.setPadding(4);
            tabela.addCell(vazio);
        }
        return tabela;
    }
}
