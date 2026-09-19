package com.smartdesk.knowledge;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Component
public class DocumentTextExtractor {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "pdf", "docx"
    );

    private final Tika tika = new Tika();
    private final KnowledgeProperties properties;

    public DocumentTextExtractor(KnowledgeProperties properties) {
        this.properties = properties;
    }

    public String extract(MultipartFile file) {
        validateBasicFile(file);

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extensionOf(filename);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 txt、md、pdf 和 docx 文件");
        }

        try {
            String detectedType;
            try (InputStream input = file.getInputStream()) {
                detectedType = tika.detect(input, filename);
            }
            String content = switch (extension) {
                case "txt", "md", "markdown" -> extractText(file, detectedType);
                case "pdf" -> extractPdf(file, detectedType);
                case "docx" -> extractDocx(file, detectedType);
                default -> throw new IllegalArgumentException("不支持的文件类型");
            };
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("文件中没有可提取的文本内容");
            }
            if (content.length() > properties.maxExtractedChars()) {
                throw new IllegalArgumentException("文件提取后的文本内容过长");
            }
            return content;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("文件解析失败，文件可能已损坏或被加密", exception);
        }
    }

    private String extractText(MultipartFile file, String detectedType) throws IOException {
        if (!detectedType.startsWith("text/")) {
            throw typeMismatch(detectedType);
        }
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    private String extractPdf(MultipartFile file, String detectedType) throws IOException {
        if (!"application/pdf".equals(detectedType)) {
            throw typeMismatch(detectedType);
        }
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(MultipartFile file, String detectedType) throws IOException {
        if (!"application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(detectedType)
                && !"application/zip".equals(detectedType)) {
            throw typeMismatch(detectedType);
        }
        try (InputStream input = file.getInputStream();
             XWPFDocument document = new XWPFDocument(input);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private IllegalArgumentException typeMismatch(String detectedType) {
        return new IllegalArgumentException(
                "文件内容与扩展名不匹配，检测到的类型为: " + detectedType
        );
    }

    private void validateBasicFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > properties.maxUploadBytes()) {
            throw new IllegalArgumentException("上传文件超过大小限制");
        }
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
