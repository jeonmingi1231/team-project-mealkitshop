package org.team.mealkitshop.repository.address;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.team.mealkitshop.domain.member.Address;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    // === 이미 있던 화면용 리스트 ===
    List<Address> findAllByMember_MnoOrderByIsDefaultDescAddressIdAsc(Long mno);
    List<Address> findByMemberOrderByIsDefaultDescAddressIdAsc(org.team.mealkitshop.domain.member.Member member);

    // === 기본배송지 단건 조회 ===
    Optional<Address> findFirstByMember_MnoAndIsDefaultTrue(Long mno);

    // === 기본배송지 존재/개수 확인 (선택) ===
    boolean existsByMember_MnoAndIsDefaultTrue(Long mno);
    long countByMember_MnoAndIsDefaultTrue(Long mno);

    // === 기본배송지 갱신 유틸 (트랜잭션 안에서 호출) ===
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Address a set a.isDefault=false " +
            "where a.member.mno=:mno and a.addressId<>:keepId and a.isDefault=true")
    int clearDefaultExcept(@Param("mno") Long mno, @Param("keepId") Long keepId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Address a set a.isDefault=false " +
            "where a.member.mno=:mno and a.isDefault=true")
    int clearAllDefault(@Param("mno") Long mno);
}
