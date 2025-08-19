package org.team.mealkitshop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.team.mealkitshop.domain.board.Reply;
import org.team.mealkitshop.dto.board.PageRequestDTO;
import org.team.mealkitshop.dto.board.PageResponseDTO;
import org.team.mealkitshop.dto.board.ReplyDTO;
import org.team.mealkitshop.repository.ReplyRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor  // final이 붙은 필드를 이용해 생성자를 만듬
@Log4j2
public class ReplyServiceImpl implements ReplyService {

    private final ReplyRepository replyRepository; // 댓글용 db
    private final ModelMapper modelMapper;         // 엔티티와 dto 변환용

    @Override
    public Long register(ReplyDTO replyDTO) {
        // 댓글 등록
        log.info("모델로 변환전 객체 : " + replyDTO);

        Reply reply = modelMapper.map(replyDTO, Reply.class);

        log.info("모델로 변환된 객체 : " + reply);

        // dto를 엔티티로 변환
        Long rno = replyRepository.save(reply).getRno();
        //                         저정     후  번호를 가져와 rno에 넣음
        return rno;
    }

    @Override
    public ReplyDTO read(Long rno) {
        // 댓글 번호가 들어오면 자세히 보기용
        Optional<Reply> replyOptional = replyRepository.findById(rno);
        //                                  select * from reply where rno = rno

        Reply reply = replyOptional.orElseThrow(); // 객체가 있으면

        return modelMapper.map(reply, ReplyDTO.class);
        //                     엔티티가 dto로 변환되어 리턴
    }

    @Override
    public PageResponseDTO<ReplyDTO> getListOfBoard(Long bno, PageRequestDTO pageRequestDTO) {

        Pageable pageable = PageRequest.of(
                //           조건              참  거짓
                pageRequestDTO.getPage() <=0 ? 0 : pageRequestDTO.getPage() -1,
                // 페이지번호가 0부터 시작하는데 프론트는 1부터 시작한다.
                pageRequestDTO.getSize(),
                Sort.by("rno").ascending() // 댓글은 처음 등록한 것이 위로 올라옴!
        );

        Page<Reply> result = replyRepository.listOfBoard(bno, pageable);
        //                                            게시물의 번호로 댓글을 가져옴

        List<ReplyDTO> dtoList = result.getContent().stream()
                .map(reply -> modelMapper.map(reply, ReplyDTO.class))
                .collect(Collectors.toList());

        return PageResponseDTO.<ReplyDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .total((int)result.getTotalElements())
                .build();
    }

    @Override
    public void modify(ReplyDTO replyDTO) {

        Optional<Reply> replyOptional = replyRepository.findById(replyDTO.getRno());
        // 댓글번호를 찾아서 엔티티 객체로 가져옴

        Reply reply = replyOptional.orElseThrow();

        reply.changeText(replyDTO.getReplyText()); // 댓글에 내용만 가져와
        // 내용 수정용 메서드로 기록

        replyRepository.save(reply); // 있으면 update

        // 차후에 프론트에서 새로고침으로 진행할 예정

    }

    @Override
    public void remove(Long rno) {

        replyRepository.deleteById(rno);  // 댓글 번호를 이용해서 삭제

    }
}