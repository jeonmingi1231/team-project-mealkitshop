package org.team.mealkitshop.service.item;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.ItemImage;
import org.team.mealkitshop.dto.item.ItemDTO;
import org.team.mealkitshop.dto.item.ItemFormDTO;
import org.team.mealkitshop.dto.item.ItemSearchDTO;
import org.team.mealkitshop.dto.item.MainItemDTO;
import org.team.mealkitshop.repository.item.ItemImgRepository;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.item.ReviewRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * ItemService
 * --------------------------------------------------------------------
 * - 역할:
 *   • 아이템(Item) 엔티티에 대한 비즈니스 로직 처리
 *   • CRUD(Create/Read/Update/Delete) 및 커스텀 조회 기능 제공
 *   • 이미지/리뷰 관련 레포지토리와 연계하여 추가 정보(평점, 리뷰 수, 이미지 파일 정리 등) 처리
 * - 주요 의존성:
 *   • ItemRepository : 아이템 기본 CRUD 및 Querydsl 기반 커스텀 조회
 *   • ItemImgRepository : 아이템 삭제 시 관련 이미지 파일 정리
 *   • ReviewRepository : 아이템 상세 조회 시 평점/리뷰 개수 계산
 *   • FileService : 실제 디스크 파일 삭제 처리
 * - 트랜잭션:
 *   • @Transactional 클래스 레벨 선언 → 메서드 기본 트랜잭션
 *   • 읽기 전용 메서드에는 @Transactional(readOnly = true) 적용
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemImgRepository itemImgRepository; // 이미지 파일 정리를 위해 주입
    private final ReviewRepository reviewRepository;
    private final FileService fileService;

    /* ==================== CREATE ==================== */

    /**
     * 아이템 생성
     * - DTO → Item 엔티티 변환 (ItemFormDTO.createItem)
     * - 저장 후 ID 반환
     *
     * @param dto 아이템 생성 DTO
     * @return 생성된 Item의 PK
     */
    public Long create(ItemFormDTO dto) {
        Objects.requireNonNull(dto, "dto must not be null");
        Item item = dto.createItem(); // DTO -> 엔티티 변환
        return itemRepository.save(item).getId();
    }

    /* ==================== READ ==================== */

    /**
     * 아이템 단건 조회
     * - Item 엔티티를 DTO로 변환
     * - ReviewRepository 통해 평균 평점/리뷰 수 채워넣음
     *
     * @param itemId 아이템 ID
     * @return ItemDTO (평점, 리뷰 수 포함)
     */
    @Transactional(readOnly = true)
    public ItemDTO read(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemId));

        ItemDTO dto = toDTO(item);

        // 추가 정보(평점, 리뷰 수) 채우기
        Double avg = reviewRepository.findAverageRatingByItemId(itemId);
        Long cnt  = reviewRepository.countByItemId(itemId);

        dto.setAvgRating(avg != null ? avg : 0.0);
        dto.setReviewCount(cnt != null ? cnt : 0L);

        return dto;
    }

    /**
     * 전체 아이템 조회 (페이징)
     * - Item 엔티티를 DTO로 변환하여 Page 반환
     *
     * @param pageable 페이징 정보
     * @return Page<ItemDTO>
     */
    @Transactional(readOnly = true)
    public Page<ItemDTO> list(Pageable pageable) {
        return itemRepository.findAll(pageable).map(this::toDTO);
    }

    /* ==================== UPDATE ==================== */

    /**
     * 아이템 수정
     * - DB에서 Item 조회 → 없으면 예외
     * - Item.updateItem(dto) 호출하여 변경(더티 체킹 반영)
     * - 수정된 엔티티를 DTO로 변환해 반환
     *
     * @param id 수정할 아이템 ID
     * @param dto 수정 DTO
     * @return 수정된 ItemDTO
     */
    public ItemDTO update(Long id, ItemFormDTO dto) {
        Objects.requireNonNull(dto, "dto must not be null");
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));
        item.updateItem(dto); // 엔티티 수정 (더티 체킹 적용)
        return toDTO(item);
    }

    /* ==================== DELETE ==================== */

    /**
     * 아이템 삭제
     * - 아이템 이미지 파일 선삭제(FileService 활용)
     * - DB에서 Item 삭제 (연관 매핑에 cascade/orphanRemoval 설정 시 관련 이미지 레코드도 함께 제거됨)
     *
     * @param id 삭제할 아이템 ID
     */
    public void delete(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));

        // 1) 실제 파일 삭제
        List<ItemImage> images = itemImgRepository.findByItemIdOrderByIdAsc(id);
        for (ItemImage img : images) {
            try {
                fileService.deleteBySavedName(img.getImgName());
            } catch (Exception ignore) {
                // 파일이 없거나 삭제 실패해도 서비스 레벨에서는 무시
            }
        }

        // 2) DB에서 아이템 삭제
        itemRepository.delete(item);
    }

    /* ==================== CUSTOM 조회 ==================== */

    /**
     * 관리자 페이지용 상품 목록 조회
     * - ItemRepositoryCustomImpl.getAdminItemPage 위임
     */
    @Transactional(readOnly = true)
    public Page<Item> getAdminPage(ItemSearchDTO cond, Pageable pageable) {
        return itemRepository.getAdminItemPage(cond, pageable);
    }

    /**
     * 메인 페이지용 상품 목록 조회
     * - ItemRepositoryCustomImpl.getMainItemPage 위임
     */
    @Transactional(readOnly = true)
    public Page<MainItemDTO> getMainPage(ItemSearchDTO cond, Pageable pageable) {
        return itemRepository.getMainItemPage(cond, pageable);
    }

    /* ==================== 매핑 유틸 ==================== */

    /**
     * Item 엔티티 → ItemDTO 변환
     * - 엔티티 필드 전부 복사
     * - avgRating, reviewCount는 서비스에서 별도로 세팅
     */
    private ItemDTO toDTO(Item i) {
        ItemDTO dto = new ItemDTO();
        dto.setId(i.getId());
        dto.setItemNm(i.getItemNm());
        dto.setPrice(i.getPrice());
        dto.setStockNumber(i.getStockNumber());
        dto.setItemDetail(i.getItemDetail());
        dto.setItemSellStatus(i.getItemSellStatus());
        dto.setCategory(i.getCategory());
        dto.setFoodItem(i.getFoodItem());
        dto.setItemLike(i.getItemLike());
        dto.setItemViewCnt(i.getItemViewCnt());
        dto.setRegTime(i.getRegTime());
        dto.setUpdateTime(i.getUpdateTime());
        dto.setCreatedBy(i.getCreatedBy());
        dto.setModifiedBy(i.getModifiedBy());
        return dto;
    }
}
