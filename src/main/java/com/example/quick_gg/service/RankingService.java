package com.example.quick_gg.service;

import com.example.quick_gg.dto.response.RankingResponse;
import com.example.quick_gg.entity.SchoolRankEntity;
import com.example.quick_gg.entity.StudentEntity;
import com.example.quick_gg.exception.CustomException;
import com.example.quick_gg.exception.ErrorCode;
import com.example.quick_gg.ranking.Tier;
import com.example.quick_gg.repository.SchoolRankRepository;
import com.example.quick_gg.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final SchoolRankRepository schoolRankRepository;
    private final StudentRepository studentRepository;

    // 학교 랭킹 조회
    // 점수(티어점수 + LP) 내림차순 정렬, 동점이면 승률 높은 순
    @Transactional(readOnly = true)
    public List<RankingResponse> getRanking() {

        List<SchoolRankEntity> sorted = schoolRankRepository.findAllWithStudent().stream()
                .sorted(Comparator
                        .comparingInt(this::calculateScore).reversed()
                        .thenComparing(Comparator.comparingDouble(this::calculateWinRate).reversed()))
                .toList();

        List<RankingResponse> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            // 인덱스 0이 1위
            result.add(toResponse(sorted.get(i), i + 1));
        }
        return result;
    }

    // 특정 학생의 랭킹 정보를 저장 또는 갱신
    // 라이엇 API에서 받아온 값을 넘겨주면 되도록 파라미터를 원시 값으로 받음
    @Transactional
    public void upsertRank(Integer studentId,
                           String tier,
                           String rankTier,
                           Integer lp,
                           Integer wins,
                           Integer losses) {

        // 티어 문자열이 유효한지 먼저 검증
        Tier.from(tier);

        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        schoolRankRepository.findByStudent(student)
                .ifPresentOrElse(
                        // 기존 레코드가 있으면 값만 갱신
                        existing -> existing.updateRank(tier, rankTier, lp, wins, losses),
                        // 없으면 새로 생성
                        () -> {
                            SchoolRankEntity created = SchoolRankEntity.builder()
                                    .student(student)
                                    .tier(tier)
                                    .rankTier(rankTier)
                                    .lp(lp)
                                    .wins(wins)
                                    .losses(losses)
                                    .build();
                            // updatedAt 세팅을 위해 동일한 갱신 메서드를 재사용
                            created.updateRank(tier, rankTier, lp, wins, losses);
                            schoolRankRepository.save(created);
                        }
                );
    }

    // 최종점수 = 티어점수 + LP
    private int calculateScore(SchoolRankEntity rank) {
        return Tier.from(rank.getTier()).getBaseScore() + rank.getLp();
    }

    // 승률(%) 계산, 소수 첫째 자리까지 반올림
    private double calculateWinRate(SchoolRankEntity rank) {
        int wins = rank.getWins();
        int losses = rank.getLosses();
        int total = wins + losses;

        // 전적이 없으면 0%
        if (total == 0) {
            return 0.0;
        }
        return Math.round((wins * 1000.0) / total) / 10.0;
    }

    // 엔티티 -> 응답 DTO 변환
    private RankingResponse toResponse(SchoolRankEntity rank, int position) {
        StudentEntity student = rank.getStudent();

        return RankingResponse.builder()
                .position(position)
                .summonerName(student.getSummonerName())
                .tag(student.getTag())
                .maskedStudentInfo(student.getStudentID() + " " + maskName(student.getName()))
                .tier(rank.getTier())
                .rankTier(rank.getRankTier())
                .lp(rank.getLp())
                .score(calculateScore(rank))
                .wins(rank.getWins())
                .losses(rank.getLosses())
                .winRate(calculateWinRate(rank))
                .build();
    }

    // 이름 첫 글자만 남기고 마스킹 (예: 김민준 -> 김**)
    private String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
