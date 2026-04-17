package haku.kmm.org.koreatechmajormeeting.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Major {
    MECHANICAL("기계공학부"),
    MECHATRONICS("메카트로닉스공학부"),
    ELECTRICAL_ELECTRONICS_COMMUNICATION("전기·전자·통신공학부"),
    COMPUTER_SCIENCE("컴퓨터공학부"),
    DESIGN("디자인공학부"),
    ENERGY_MATERIALS_CHEMICAL("에너지신소재화학공학부"),
    INDUSTRIAL_MANAGEMENT("산업경영학부"),
    EMPLOYMENT_SERVICES("고용서비스정책학과");

    private final String description;
}