package org.team.mealkitshop.controller.member;

import jakarta.annotation.security.PermitAll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/thymeleaf")
public class HomeController {

    @GetMapping("/main") // ★ 슬래시 추가
    public String main() {
        return "thymeleaf/main"; // → templates/thymeleaf/main.html
    }

}
