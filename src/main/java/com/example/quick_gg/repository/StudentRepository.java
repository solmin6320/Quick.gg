package com.example.quick_gg.repository;


import com.example.quick_gg.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentEntity, Integer> {
        // 조회 결과가 없을 수 있기 때문에 Optional 사용
        Optional<StudentEntity> findByStudentNumber(String studentNumber);

        // 소환사 이름과 태그가 이미 등록되어 있는지 확인
         boolean existsBySummonerNameAndTag(String summonerName, String tag);

         // Puuid가 이미 등록되어 있는지 확인
         boolean existsByPuuid(String puuid);

         // 학번이 이미 등록되어 있는지 확인
         boolean existsByStudentNumber(String studentNumber);

         // id가 이미 등록되어 있는지 확인
         boolean existsById(Integer id);

}
