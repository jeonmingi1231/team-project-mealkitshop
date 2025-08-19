package org.team.mealkitshop.repository.item;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.common.ItemSortType;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.QItem;
import org.team.mealkitshop.domain.item.QItemImage;
import org.team.mealkitshop.dto.item.ItemSearchDTO;
import org.team.mealkitshop.dto.item.MainItemDTO;
import org.team.mealkitshop.dto.item.QMainItemDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
public class ItemRepositoryCustomImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    // QueryDSL 쿼리 생성을 위한 팩토리 (EntityManager 기반)

    public ItemRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    /**
     * 관리자 페이지용 상품 목록 조회
     * - 검색 조건(ItemSearchDTO)에 따라 Item 엔티티 자체를 페이징 반환
     * - 조건: 등록일(최근 N일/주/월), 판매상태, itemNm/createdBy 부분 검색
     * - 정렬: 최신 등록순(id desc)
     */
    @Override
    public Page<Item> getAdminItemPage(ItemSearchDTO dto, Pageable pageable) {
        QItem item = QItem.item;

        List<Item> content = queryFactory
                .selectFrom(item)
                .where(
                        regDtsAfter(dto != null ? dto.getSearchDateType() : null),
                        sellStatusEq(dto != null ? dto.getSearchSellStatus() : null),
                        searchByLike(dto != null ? dto.getSearchBy() : null,
                                dto != null ? dto.getSearchQuery() : null)
                )
                .orderBy(item.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(item.count())
                .from(item)
                .where(
                        regDtsAfter(dto != null ? dto.getSearchDateType() : null),
                        sellStatusEq(dto != null ? dto.getSearchSellStatus() : null),
                        searchByLike(dto != null ? dto.getSearchBy() : null,
                                dto != null ? dto.getSearchQuery() : null)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /**
     * 메인 페이지용 상품 목록 조회
     * - 대표이미지(repimgYn=true)와 조인하여 MainItemDTO 형태로 반환
     * - 정렬 조건: 조회수/좋아요/가격/최신순 등 (ItemSortType에 따라 동적 변경)
     * - count 쿼리는 이미지 조인 제외(중복 방지)
     */
    @Override
    public Page<MainItemDTO> getMainItemPage(ItemSearchDTO dto, Pageable pageable) {
        QItem item = QItem.item;
        QItemImage itemImage = QItemImage.itemImage;

        List<MainItemDTO> content = queryFactory
                .select(new QMainItemDTO(
                        item.id, item.itemNm, item.itemDetail,
                        itemImage.imgUrl, item.price,
                        item.itemLike, item.itemViewCnt
                ))
                .from(itemImage)
                .join(itemImage.item, item)
                .where(itemImage.repimgYn.isTrue())
                .distinct()
                .orderBy(getSortOrder(dto != null ? dto.getSortType() : null))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(item.count())
                .from(item)
                .where(
                        JPAExpressions.selectOne()
                                .from(itemImage)
                                .where(itemImage.item.eq(item)
                                        .and(itemImage.repimgYn.isTrue()))
                                .exists()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /* ================== 검색 조건 Helper ================== */

    /** 최근 1일/1주/1달/6달 등록 상품 필터 */
    private BooleanExpression regDtsAfter(String searchDateType) {
        if (searchDateType == null || searchDateType.isEmpty()) return null;
        LocalDateTime dt = LocalDateTime.now();
        switch (searchDateType) {
            case "1d": dt = dt.minusDays(1); break;
            case "1w": dt = dt.minusWeeks(1); break;
            case "1m": dt = dt.minusMonths(1); break;
            case "6m": dt = dt.minusMonths(6); break;
            default: return null;
        }
        return QItem.item.regTime.after(dt);
    }

    /** 판매 상태 조건 (null 시 무시) */
    private BooleanExpression sellStatusEq(ItemSellStatus status) {
        return status == null ? null : QItem.item.itemSellStatus.eq(status);
    }

    /** itemNm 또는 createdBy LIKE 검색 */
    private BooleanExpression searchByLike(String searchBy, String searchQuery) {
        if (searchBy == null || searchQuery == null || searchQuery.trim().isEmpty()) return null;
        switch (searchBy) {
            case "itemNm":    return QItem.item.itemNm.containsIgnoreCase(searchQuery.trim());
            case "createdBy": return QItem.item.createdBy.containsIgnoreCase(searchQuery.trim());
            default:          return null;
        }
    }

    /** 정렬 조건 생성 (기본: 최신순) */
    private OrderSpecifier<?>[] getSortOrder(ItemSortType sortType) {
        ItemSortType sort = (sortType != null) ? sortType : ItemSortType.NEW;
        switch (sort) {
            case POPULAR_LIKE: return new OrderSpecifier[]{ QItem.item.itemLike.desc(), QItem.item.id.desc() };
            case POPULAR_VIEW: return new OrderSpecifier[]{ QItem.item.itemViewCnt.desc(), QItem.item.id.desc() };
            case PRICE_ASC:    return new OrderSpecifier[]{ QItem.item.price.asc(), QItem.item.id.desc() };
            case PRICE_DESC:   return new OrderSpecifier[]{ QItem.item.price.desc(), QItem.item.id.desc() };
            case NEW:
            default:           return new OrderSpecifier[]{ QItem.item.id.desc() };
        }
    }
}
