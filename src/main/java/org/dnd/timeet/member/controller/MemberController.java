package org.dnd.timeet.member.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dnd.timeet.member.application.MemberService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User 컨트롤러", description = "User API입니다.")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class MemberController {

    private final MemberService memberService;

}

