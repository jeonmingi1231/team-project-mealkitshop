package org.team.mealkitshop.service.item;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

/** 로컬 디스크 파일 저장/삭제 서비스 — 최종본 */
/**
 * 로컬 디스크 파일 저장/삭제 서비스
 * - 설정된 uploadPath 경로에 파일을 저장/삭제하는 유틸성 서비스
 * - byte[] / InputStream 방식 업로드 지원
 * - 파일명은 UUID 기반으로 생성 → 중복/충돌 방지
 * - 경로 안전성 검증(safeResolve)으로 디렉터리 탈출 방지
 */
@Service
@Log4j2
public class FileService {

    private final String uploadPath;

    /** application.properties 의 uploadPath 값을 주입 */
    public FileService(@Value("${uploadPath}") String uploadPath) {
        this.uploadPath = uploadPath;
    }

    /**
     * 서비스 초기화 시 업로드 루트 디렉토리 존재 확인/생성
     */
    @PostConstruct
    void checkUploadDir() {
        if (uploadPath == null || uploadPath.isBlank()) {
            throw new IllegalStateException("uploadPath is not set (application.properties)");
        }
        try {
            Files.createDirectories(getUploadRoot());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create upload directory: " + uploadPath, e);
        }
    }

    /** 파일 업로드 (byte[] 기반) → 저장된 파일명 반환 */
    public String uploadFile(String originalFileName, byte[] fileData) throws IOException {
        return uploadFileInternal(getUploadRoot(), originalFileName, fileData);
    }

    /** 파일 업로드 (InputStream 기반) → 저장된 파일명 반환 */
    public String uploadFile(String originalFileName, InputStream in) throws IOException {
        return uploadFileInternal(getUploadRoot(), originalFileName, in);
    }

    /** 저장된 파일명을 기준으로 파일 삭제 (삭제 성공 여부 반환) */
    public boolean deleteBySavedName(String savedFileName) throws IOException {
        return deleteFileInternal(getUploadRoot(), savedFileName);
    }

    /* ================= 내부 유틸 메서드 ================= */

    /** 업로드 루트 디렉토리 경로 반환 */
    private Path getUploadRoot() {
        return Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    /** 원본 파일명에서 확장자 추출 */
    private static String extractExt(String originalFileName) {
        String safe = Paths.get(originalFileName).getFileName().toString();
        int dot = safe.lastIndexOf('.');
        return (dot >= 0 && dot < safe.length() - 1)
                ? safe.substring(dot).toLowerCase(Locale.ROOT)
                : "";
    }

    /** 디렉토리 경로 내 안전한 상대 경로 생성 (경로 탈출 방지) */
    private static Path safeResolve(Path dir, String savedFileName) {
        Path target = dir.resolve(savedFileName).normalize();
        if (!target.startsWith(dir)) throw new SecurityException("Invalid path traversal: " + savedFileName);
        return target;
    }

    /** UUID 기반의 새로운 저장 파일명 생성 (확장자 유지) */
    private static String newSavedName(String originalFileName) {
        return UUID.randomUUID() + extractExt(originalFileName);
    }

    /** 내부 업로드 로직 (byte[] 기반) */
    private String uploadFileInternal(Path dir, String originalFileName, byte[] fileData) throws IOException {
        Objects.requireNonNull(originalFileName, "originalFileName must not be null");
        Objects.requireNonNull(fileData, "fileData must not be null");

        Files.createDirectories(dir);
        String saved = newSavedName(originalFileName);
        Path target = safeResolve(dir, saved);
        Files.write(target, fileData, CREATE_NEW); // 이미 존재하면 실패
        log.info("Saved file: {} ({} bytes)", target, fileData.length);
        return saved;
    }

    /** 내부 업로드 로직 (InputStream 기반) */
    private String uploadFileInternal(Path dir, String originalFileName, InputStream in) throws IOException {
        Objects.requireNonNull(originalFileName, "originalFileName must not be null");
        Objects.requireNonNull(in, "input stream must not be null");

        Files.createDirectories(dir);
        String saved = newSavedName(originalFileName);
        Path target = safeResolve(dir, saved);

        try (in) {
            Files.copy(in, target); // 존재 시 FileAlreadyExistsException 발생
        } catch (IOException e) {
            Files.deleteIfExists(target); // 실패 시 생성된 파일 제거
            throw e;
        }
        log.info("Saved file: {}", target);
        return saved;
    }

    /** 내부 파일 삭제 로직 (성공 여부 반환) */
    private boolean deleteFileInternal(Path dir, String savedFileName) throws IOException {
        Objects.requireNonNull(savedFileName, "savedFileName must not be null");
        Path target = safeResolve(dir, savedFileName);
        boolean deleted = Files.deleteIfExists(target);
        if (deleted) log.info("Deleted file: {}", target);
        else log.info("File not found for delete: {}", target);
        return deleted;
    }
}
