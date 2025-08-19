package org.team.mealkitshop.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SocialJoinDTO {

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @NotBlank
    @Size(max = 20)
    private String memberName;

    @NotBlank
    @Size(max = 20)
    private String phone;

    private boolean marketingYn;
}
