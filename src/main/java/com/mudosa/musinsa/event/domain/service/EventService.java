package com.mudosa.musinsa.event.domain.service;

import com.mudosa.musinsa.event.domain.repository.EventImageRepository;
import com.mudosa.musinsa.event.domain.repository.EventOptionRepository;
import com.mudosa.musinsa.event.domain.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j

//controller에서 List<EventListResDto> eventList = eventService.getEventByType(eventType); // EventService에 만들어야됨
/*
* 이벤트 목록,이벤트에 매핑된 옵션리스트,
*
* */

public class EventService {
    private final EventRepository eventRepository;
    private final EventImageRepository imageRepository;
    private final EventOptionRepository optionRepository;

    public void create




}
