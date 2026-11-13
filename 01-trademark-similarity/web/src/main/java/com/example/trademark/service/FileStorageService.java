package com.example.trademark.service;

import com.example.trademark.config.AppProperties;
import com.example.trademark.exception.AppExceptions.InvalidFileException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 업로드 이미지 저장: {uploadDir}/{category}/{yyyyMMdd}/{uuid}.{ext} */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");

    private final Path root;

    public FileStorageService(AppProperties props) {
        this.root = Paths.get(props.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 폴더를 만들 수 없습니다: " + root, e);
        }
    }

    public Path getRoot() {
        return root;
    }

    /** @return uploadDir 기준 상대경로 (예: trademark/20261112/uuid.png) */
    public String store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("이미지 파일을 선택하세요.");
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        ext = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(ext)) {
            throw new InvalidFileException("이미지 파일(png, jpg, jpeg, gif, bmp, webp)만 업로드할 수 있습니다.");
        }
        String relative = category + "/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            + "/" + UUID.randomUUID() + "." + ext;
        Path target = resolve(relative);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패", e);
        }
        return relative;
    }

    public byte[] load(String relative) {
        try {
            return Files.readAllBytes(resolve(relative));
        } catch (IOException e) {
            throw new InvalidFileException("저장된 이미지를 읽을 수 없습니다: " + relative);
        }
    }

    public void deleteQuietly(String relative) {
        if (relative == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relative));
        } catch (IOException ignored) {
            // 파일 삭제 실패는 서비스 흐름에 영향을 주지 않는다
        }
    }

    private Path resolve(String relative) {
        Path p = root.resolve(relative).normalize();
        if (!p.startsWith(root)) {
            throw new InvalidFileException("잘못된 파일 경로입니다.");
        }
        return p;
    }
}
