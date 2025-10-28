-- =====================================================
-- Settlement Number Sequences
-- =====================================================
-- 정산 번호 생성을 위한 시퀀스들
-- 각 정산 유형별로 별도의 시퀀스를 사용합니다.
--
-- 사용 예시:
-- SELECT nextval('daily_settlement_seq')  -> 1, 2, 3, ...
-- =====================================================

-- 일일 정산 번호 시퀀스
CREATE SEQUENCE IF NOT EXISTS daily_settlement_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO CYCLE
    CACHE 1;

COMMENT ON SEQUENCE daily_settlement_seq IS '일일 정산 번호 생성용 시퀀스 (DAILY-YYYYMMDD-XXXXX)';

-- 주간 정산 번호 시퀀스
CREATE SEQUENCE IF NOT EXISTS weekly_settlement_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO CYCLE
    CACHE 1;

COMMENT ON SEQUENCE weekly_settlement_seq IS '주간 정산 번호 생성용 시퀀스 (WEEKLY-YYYYWXX-XXXXX)';

-- 월간 정산 번호 시퀀스
CREATE SEQUENCE IF NOT EXISTS monthly_settlement_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO CYCLE
    CACHE 1;

COMMENT ON SEQUENCE monthly_settlement_seq IS '월간 정산 번호 생성용 시퀀스 (MONTHLY-YYYYMM-XXXXX)';

-- 연간 정산 번호 시퀀스
CREATE SEQUENCE IF NOT EXISTS yearly_settlement_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO CYCLE
    CACHE 1;

COMMENT ON SEQUENCE yearly_settlement_seq IS '연간 정산 번호 생성용 시퀀스 (YEARLY-YYYY-XXXXX)';
