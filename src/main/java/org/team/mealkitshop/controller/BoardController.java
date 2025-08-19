package org.team.mealkitshop.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.team.mealkitshop.domain.board.BoardReaction;
import org.team.mealkitshop.dto.board.*;
import org.team.mealkitshop.service.BoardReactionService;
import org.team.mealkitshop.service.BoardService;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@Controller
@RequestMapping("/board")  // http://192.168.111.105:80/board
@Log4j2
@RequiredArgsConstructor // final을 붙인 필드로 생성자 만듬.
public class BoardController {

    @Value("${org.mbc.upload.path")
    private String uploadPath;
    private final BoardService boardService;
    private final BoardReactionService boardReactionService;

    @GetMapping("/list")
    public void list(PageRequestDTO pageRequestDTO, Model model){
        // 페이징 처리와 정렬과 검색이 추가된 리스트가 나옴.

        // p548쪽 제외PageResponseDTO<BoardDTO> responseDTO = boardService.list(pageRequestDTO);
        // 페이징 처리가 되는 요청을 처리하고 결과를 response로 받는다.

        PageResponseDTO<BoardListAllDTO> responseDTO =
                boardService.listWithAll(pageRequestDTO); // p662 수정
        // 댓글의 갯수용 dto로 프론트 전달!!

        log.info(responseDTO);

        model.addAttribute("responseDTO",responseDTO); // 결과를 스프링이 관리하는 모델 객체로 전달
    }

    @PreAuthorize("hasRole('USER')") //  User.roles("USER")  // User.authorities("ROLE_USER")
    @GetMapping("/register")
    public void registerGET(){
        // http://localhost/board/register로 들어오면 board.register.html로 간다.
    }

    @PostMapping("/register")
    public String registerPost(@Valid BoardDTO boardDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes){
        // BindingResult : @Valid,  @ModelAttribute에 데이터 바인딩 오류가 발생할때 오류정보를 담는다.
        // bindingResult가 없으면 400 오류가 발생하게 되고 Controller가 호출되지 않고 Error Page로 이동함.

        log.info("board POST register.......");

        if(bindingResult.hasErrors()) { // 오류발생시 addFlashAttribute로 1회용 에러 메시지를 담고 전달한다.
            log.info("has errors.......");
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors() );
            return "redirect:/board/register";
        }

        log.info(boardDTO);

        Long bno  = boardService.register(boardDTO);

        redirectAttributes.addFlashAttribute("result", bno);
        // 정상 처리시 addFlashAttribute에 결과 정보를 bno를 담아 전달한다.
        return "redirect:/board/list";
    }


//    @GetMapping("/read")
//    public void read(Long bno, PageRequestDTO pageRequestDTO, Model model){
//
//        BoardDTO boardDTO = boardService.readOne(bno);
//
//        log.info(boardDTO);
//
//        model.addAttribute("dto", boardDTO);
//
//    }


    @PreAuthorize("isAuthenticated()") // 로그인한 상태이면!!! (권한에 상관없음)
    @GetMapping({"/read", "/modify"})
    public void read(Long bno, PageRequestDTO pageRequestDTO, Model model){

        BoardDTO boardDTO = boardService.readOne(bno);

        log.info(boardDTO);

        model.addAttribute("dto", boardDTO);

    }

    @PreAuthorize("principal.username == #boardDTO.writer")  // p714 로그인한 사용자와 작성자가 같은지
    @PostMapping("/modify")
    public String modify( PageRequestDTO pageRequestDTO,
                          @Valid BoardDTO boardDTO,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes){

        log.info("board modify post......." + boardDTO);

        if(bindingResult.hasErrors()) {
            log.info("has errors.......");

            String link = pageRequestDTO.getLink();

            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors() );

            redirectAttributes.addAttribute("bno", boardDTO.getBno());

            return "redirect:/board/modify?"+link;
        }

        boardService.modify(boardDTO);

        redirectAttributes.addFlashAttribute("result", "modified");

        redirectAttributes.addAttribute("bno", boardDTO.getBno());

        return "redirect:/board/read";
    }

    @PreAuthorize("principal.username == #boardDTO.writer") // p719 작성자가 삭제 가능!!!!
    @PostMapping("/remove")
    public String remove(BoardDTO boardDTO, RedirectAttributes redirectAttributes) {

        Long bno = boardDTO.getBno();

        boardService.remove(bno);

        List<String> fileNames = boardDTO.getFileNames();
        if(fileNames != null && fileNames.size() > 0) {
            removeFiles(fileNames);
        }
        redirectAttributes.addFlashAttribute("result", "removed");

        return "redirect:/board/list";

    }

    private void removeFiles(List<String> files) {

        for (String fileName:files) {
            // import org.springframework.core.io.Resource
            Resource resource = new FileSystemResource(uploadPath + File.separator + fileName);
            String resourceName = resource.getFilename();


            try {
                String contentType = Files.probeContentType(resource.getFile().toPath());
                resource.getFile().delete();

                //섬네일이 존재한다면
                if (contentType.startsWith("image")) {
                    File thumbnailFile = new File(uploadPath + File.separator + "s_" + fileName);
                    thumbnailFile.delete();
                }

            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }//end for
    }

    @PostMapping("/read/secret") // 비밀게시글에 들어갈 때 비밀번호가 맞으면 이동 아니면 리턴
    public String checkSecretPassword(Long bno, String inputPassword, Model model, RedirectAttributes redirectAttributes) {
        BoardDTO boardDTO = boardService.readOne(bno);

        if(boardDTO.isSecretBoard()) {
            if(boardDTO.getSecretPassword().equals(inputPassword)) {
                // 비밀번호 맞음 -> 상세페이지로 이동
                model.addAttribute("dto", boardDTO);
                return "board/read";
            } else {
                // 비밀번호 틀림 -> 다시 입력폼으로
                redirectAttributes.addFlashAttribute("error", "비밀번호가 틀렸습니다.");
                redirectAttributes.addAttribute("bno", bno);
                return "redirect:/board/read";
            }
        }

        return "redirect:/board/list";  // 비밀글이 아닌 경우, 리스트로 리다이렉트
    }

    // 비밀글 설정 여부
    @PostMapping("/register/secret")
    public String register(BoardDTO dto) {
        boardService.register(dto);
        return "redirect:/board/list";
    }

    // 좋아요, 추천, 비추천
    @PostMapping
    public ResponseEntity<String> reactToBoard(@RequestBody BoardReactionDTO reactionDTO){
        boardReactionService.reactToBoard(reactionDTO);
        return ResponseEntity.ok("좋아요, 추천, 비추천 저장이 완료되었습니다.");
    }

}
