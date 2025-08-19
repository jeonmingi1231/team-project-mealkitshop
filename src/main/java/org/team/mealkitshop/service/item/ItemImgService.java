package org.team.mealkitshop.service.item;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.ItemImage;
import org.team.mealkitshop.dto.item.ItemImgDTO;
import org.team.mealkitshop.repository.item.ItemImgRepository;
import org.team.mealkitshop.repository.item.ItemRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 아이템 이미지 서비스 (ItemImgService)
 * - 역할:
 *   • 아이템 이미지 업로드/조회/수정/삭제를 담당하는 서비스 계층
 *   • 실제 파일 저장은 FileService, DB 저장은 ItemImgRepository 사용
 *   • 단일/다중 업로드 지원, 대표 이미지 자동 지정 로직 포함
 *   • 업로드 중 실패 발생 시 이미 저장된 파일 정리(rollback 보완)
 * - 트랜잭션:
 *   • 클래스 레벨 @Transactional → 기본적으로 모든 메서드 트랜잭션 처리
 *   • 읽기 전용 조회 메서드는 @Transactional(readOnly = true)로 성능 최적화
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ItemImgService {

    @Value("${uploadPath}")
    private String imgBase; // 업로드된 파일 접근 URL prefix

    private final FileService fileService;              // 파일 입출력 담당
    private final ItemImgRepository itemImgRepository;  // DB 저장소
    private final ItemRepository itemRepository;        // 아이템 존재 확인용

    /* ========== CREATE ========== */

    /**
     * 단일 이미지 업로드
     * - 파일이 비어있으면 IllegalArgumentException 발생
     * - 대표 이미지 요청(asRep=true)이거나, 해당 아이템에 기존 대표 이미지가 없으면 새 업로드 이미지를 대표로 지정
     * - 기존 대표 이미지가 있다면 clearRep()으로 해제 후 지정
     * - 업로드 중 IOException/RuntimeException 발생 시 이미 저장된 파일 정리
     *
     * @param itemId 아이템 ID
     * @param file 업로드할 MultipartFile
     * @param asRep 대표로 지정할지 여부
     * @return 저장된 이미지 DTO
     */
    public ItemImgDTO create(Long itemId, MultipartFile file, boolean asRep) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("empty file");
        Item item = getItem(itemId);

        List<String> savedThisCall = new ArrayList<>();
        try {
            boolean makeRep = asRep || !hasRep(itemId);
            if (makeRep) itemImgRepository.clearRep(itemId);

            ItemImage e = uploadAndMakeEntity(item, file, makeRep, savedThisCall);
            return toDTO(itemImgRepository.save(e));
        } catch (IOException | RuntimeException e) {
            cleanupSaved(savedThisCall);
            throw e;
        }
    }

    /**
     * 다중 이미지 업로드
     * - 파일 리스트가 null 또는 비어있으면 빈 리스트 반환
     * - 기존 대표 이미지가 없으면 첫 유효 파일을 대표로 지정
     * - 업로드 실패 시, 저장된 파일 모두 삭제 처리
     *
     * @param itemId 아이템 ID
     * @param files 업로드할 MultipartFile 리스트
     * @return 저장된 이미지 DTO 리스트
     */
    public List<ItemImgDTO> saveImages(Long itemId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) return List.of();
        Item item = getItem(itemId);

        boolean alreadyHasRep = hasRep(itemId);
        boolean setOnce = false;
        List<ItemImage> batch = new ArrayList<>();
        List<String> savedThisBatch = new ArrayList<>();

        try {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                boolean makeRep = !alreadyHasRep && !setOnce;
                if (makeRep) { itemImgRepository.clearRep(itemId); setOnce = true; }
                batch.add(uploadAndMakeEntity(item, f, makeRep, savedThisBatch));
            }
            if (batch.isEmpty()) return List.of();
            return itemImgRepository.saveAll(batch).stream().map(this::toDTO).toList();
        } catch (IOException | RuntimeException e) {
            cleanupSaved(savedThisBatch);
            throw e;
        }
    }

    /* ========== READ ========== */

    /**
     * 특정 아이템의 전체 이미지 목록 조회
     *
     * @param itemId 아이템 ID
     * @return 이미지 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<ItemImgDTO> list(Long itemId) {
        return itemImgRepository.findByItemIdOrderByIdAsc(itemId).stream().map(this::toDTO).toList();
    }

    /**
     * 특정 아이템의 대표 이미지 조회
     *
     * @param itemId 아이템 ID
     * @return Optional<ItemImgDTO> (대표 이미지 없을 시 empty)
     */
    @Transactional(readOnly = true)
    public Optional<ItemImgDTO> getRepresentative(Long itemId) {
        return itemImgRepository.findTopByItemIdAndRepimgYnTrueOrderByIdAsc(itemId).map(this::toDTO);
    }

    /**
     * 여러 아이템의 대표 이미지를 한 번에 조회
     * - Map<itemId, DTO> 형식으로 반환
     *
     * @param itemIds 아이템 ID 리스트
     * @return 대표 이미지 맵
     */
    @Transactional(readOnly = true)
    public Map<Long, ItemImgDTO> getRepresentatives(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return Collections.emptyMap();
        return itemImgRepository.findByItem_IdInAndRepimgYnTrue(itemIds).stream()
                .collect(Collectors.toMap(img -> img.getItem().getId(), this::toDTO, (a, b) -> a));
    }

    /* ========== UPDATE ========== */

    /**
     * 특정 이미지를 대표 이미지로 변경
     * - 해당 이미지가 지정한 아이템의 것이 아닐 경우 IllegalArgumentException 발생
     * - 기존 대표 이미지 해제 후, 지정 이미지 대표 설정
     * - 업데이트 실패 시 IllegalStateException 발생
     *
     * @param itemId 아이템 ID
     * @param imageId 이미지 ID
     */
    public void setRepresentative(Long itemId, Long imageId) {
        ItemImage target = itemImgRepository.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("Image not found: " + imageId));
        if (!Objects.equals(target.getItem().getId(), itemId)) {
            throw new IllegalArgumentException("Image does not belong to item");
        }
        itemImgRepository.clearRep(itemId);
        int updated = itemImgRepository.setRep(imageId);
        if (updated == 0) {
            throw new IllegalStateException("Failed to set representative image: " + imageId);
        }
    }

    /* ========== DELETE ========== */

    /**
     * 단일 이미지 삭제
     * - 파일 삭제 시 예외 무시
     * - 삭제 후 대표 이미지가 없으면 첫 번째 이미지를 대표로 승격
     *
     * @param imageId 삭제할 이미지 ID
     */
    public void delete(Long imageId) {
        ItemImage e = itemImgRepository.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("Image not found: " + imageId));
        try { fileService.deleteBySavedName(e.getImgName()); } catch (Exception ignore) {}
        Long itemId = e.getItem().getId();
        itemImgRepository.delete(e);
        ensureRep(itemId);
    }

    /**
     * 여러 이미지 일괄 삭제
     * - 대상 이미지 모두 존재하는지 검증
     * - 다른 아이템에 속한 이미지 포함 시 IllegalArgumentException 발생
     * - 파일 삭제 후 DB 삭제
     * - 삭제 후 대표 이미지가 없으면 첫 번째 이미지를 대표로 승격
     *
     * @param itemId 아이템 ID
     * @param ids 삭제할 이미지 ID 리스트
     */
    public void deleteAll(Long itemId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;

        List<ItemImage> targets = new ArrayList<>(itemImgRepository.findAllById(ids));

        if (targets.size() != ids.size()) {
            throw new NoSuchElementException("Some images not found among: " + ids);
        }
        if (targets.stream().anyMatch(t -> !Objects.equals(t.getItem().getId(), itemId))) {
            throw new IllegalArgumentException("contains images of other item");
        }

        for (ItemImage t : targets) {
            try { fileService.deleteBySavedName(t.getImgName()); } catch (Exception ignore) {}
        }
        itemImgRepository.deleteAll(targets);
        ensureRep(itemId);
    }

    /* ========== HELPER 메서드 ========== */

    /** 아이템 조회 (없으면 예외 발생) */
    private Item getItem(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Item not found: " + id));
    }

    /** 대표 이미지 존재 여부 확인 */
    private boolean hasRep(Long itemId) {
        return itemImgRepository.existsByItemIdAndRepimgYnTrue(itemId);
    }

    /** 대표 이미지 없으면 첫 번째 이미지를 대표로 승격 */
    private void ensureRep(Long itemId) {
        if (hasRep(itemId)) return;
        var rest = itemImgRepository.findByItemIdOrderByIdAsc(itemId);
        if (!rest.isEmpty()) itemImgRepository.setRep(rest.get(0).getId());
    }

    /**
     * 업로드 + 엔티티 변환 (공통 로직)
     * - 파일을 FileService를 이용해 저장하고, ItemImage 엔티티 생성
     * - 저장 파일명(saved)을 추적 리스트에 추가하여 실패 시 삭제 가능
     */
    private ItemImage uploadAndMakeEntity(Item item, MultipartFile file, boolean rep, List<String> savedTracker) throws IOException {
        String ori = Objects.requireNonNull(file.getOriginalFilename(), "original filename is null");
        String saved;
        try (InputStream in = file.getInputStream()) {
            saved = fileService.uploadFile(ori, in);
        }
        if (savedTracker != null) savedTracker.add(saved);

        ItemImage e = new ItemImage();
        e.setItem(item);
        e.setOriImgName(ori);
        e.setImgName(saved);
        e.setImgUrl(imgBase + saved);
        e.setRepimgYn(rep);
        return e;
    }

    /** 업로드 중 실패한 파일들을 정리 (삭제) */
    private void cleanupSaved(List<String> savedNames) {
        for (String fn : savedNames) {
            try { fileService.deleteBySavedName(fn); } catch (Exception ignore) {}
        }
    }

    /** ItemImage → DTO 변환 */
    private ItemImgDTO toDTO(ItemImage e) {
        return ItemImgDTO.builder()
                .id(e.getId())
                .imgName(e.getImgName())
                .oriImgName(e.getOriImgName())
                .imgUrl(e.getImgUrl())
                .repimgYn(Boolean.TRUE.equals(e.getRepimgYn())) // null-safe 처리
                .build();
    }
}
