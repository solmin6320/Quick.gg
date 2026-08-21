package com.example.quick_gg.repository;

import com.example.quick_gg.entity.FavoriteSummonerEntity;
import com.example.quick_gg.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteSummonerRepository extends JpaRepository<FavoriteSummonerEntity, Integer> {

    // 특정 학생의 즐겨찾기 목록 전체 조회
    List<FavoriteSummonerEntity> findByStudent(StudentEntity student);

    // 본인 소유의 즐겨찾기만 삭제할 수 있도록 id + 학생으로 단건 조회
    Optional<FavoriteSummonerEntity> findByIdAndStudent(Integer id, StudentEntity student);

    // 같은 소환사를 중복 등록하는 것을 방지
    boolean existsByStudentAndSummonerNameAndTag(StudentEntity student,
                                                 String summonerName,
                                                 String tag);
}
