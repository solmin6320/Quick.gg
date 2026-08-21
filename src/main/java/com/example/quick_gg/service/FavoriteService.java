package com.example.quick_gg.service;

import com.example.quick_gg.dto.request.FavoriteRequest;
import com.example.quick_gg.dto.response.FavoriteResponse;
import com.example.quick_gg.entity.FavoriteSummonerEntity;
import com.example.quick_gg.entity.StudentEntity;
import com.example.quick_gg.exception.CustomException;
import com.example.quick_gg.exception.ErrorCode;
import com.example.quick_gg.repository.FavoriteSummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteSummonerRepository favoriteSummonerRepository;
    private final CurrentStudentProvider currentStudentProvider;

    // 즐겨찾기 목록 조회 (본인 것만)
    @Transactional(readOnly = true)
    public List<FavoriteResponse> getFavorites() {
        StudentEntity student = currentStudentProvider.getCurrentStudent();

        return favoriteSummonerRepository.findByStudent(student).stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    // 즐겨찾기 추가
    @Transactional
    public FavoriteResponse addFavorite(FavoriteRequest request) {
        StudentEntity student = currentStudentProvider.getCurrentStudent();

        // 앞뒤 공백 제거 (같은 소환사가 다른 값으로 중복 등록되는 것 방지)
        String summonerName = request.getSummonerName().trim();
        String tag = request.getTag().trim();

        // 소환사 중복 등록 방지
        if (favoriteSummonerRepository
                .existsByStudentAndSummonerNameAndTag(student, summonerName, tag)) {
            throw new CustomException(ErrorCode.CONFLICT);
        }

        FavoriteSummonerEntity favorite = FavoriteSummonerEntity.builder()
                .student(student)
                .summonerName(summonerName)
                .tag(tag)
                .build();

        favoriteSummonerRepository.save(favorite);

        return FavoriteResponse.from(favorite);
    }

    // 즐겨찾기 삭제 (본인 소유가 아니면 404)
    @Transactional
    public void deleteFavorite(Integer id) {
        StudentEntity student = currentStudentProvider.getCurrentStudent();

        FavoriteSummonerEntity favorite = favoriteSummonerRepository
                .findByIdAndStudent(id, student)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        favoriteSummonerRepository.delete(favorite);
    }
}
