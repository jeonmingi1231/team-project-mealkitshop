package org.team.mealkitshop.dto.address;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AddressResponseDTO {
    private Long id;
    private String alias;
    private String zipCode;
    private String addr1;
    private String addr2;
    private Boolean isDefault;
    private LocalDateTime regDate;
    private LocalDateTime updDate;
}
