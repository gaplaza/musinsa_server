package com.mudosa.musinsa.event.domain.controller;

import com.mudosa.musinsa.event.domain.dto.EventListResDto;
import com.mudosa.musinsa.event.domain.dto.EventOptionResDto;
import com.mudosa.musinsa.event.domain.model.Event;
import com.mudosa.musinsa.event.domain.service.EventService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j

public class EventController {

    private final EventService eventService;

    /*
    이벤트 목록 조회 api
    @param type ( drop, comment )
    @return 필터링된 이벤트 목록 + 특정 이벤트에 포함된 상품 list
    */

    @GetMapping
    public ResponseEntity<List<EventListResDto>> getEventList(@RequestParam("type") String type) {
        Event.EventType eventType;

        try{
            eventType = Event.EventType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e){
            // 잘못된 타입을 파라미터로 넘겨줄 경우
            return ResponseEntity.badRequest().build();  // 400 잘못된 요청
        }

        List<EventListResDto> eventList = eventService.getEventByType(eventType); // EventService에 만들어야됨
        return ResponseEntity.ok(eventList);
    }
}
