package com.mudosa.musinsa.event.domain.repository;

import com.mudosa.musinsa.event.domain.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
// 인터페이스로 만들기, jpa 생성 ?

/**
 * Event Repository
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    List<Event> findByIsActiveTrue();
    
    List<Event> findByEventStartTimeBeforeAndEventEndTimeAfter(
        LocalDateTime now1, 
        LocalDateTime now2
    );
}
