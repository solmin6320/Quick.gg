package com.example.quick_gg.repository;

import com.example.quick_gg.entity.SchoolRankEntity;
import com.example.quick_gg.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SchoolRankRepository extends JpaRepository<SchoolRankEntity, Integer> {

    // 특정 학생의 랭킹 레코드 조회 (갱신 시 사용)
    Optional<SchoolRankEntity> findByStudent(StudentEntity student);

    // 랭킹 목록 조회 시 학생 정보까지 한 번에 가져옴 (N+1 방지)
    @Query("select sr from SchoolRankEntity sr join fetch sr.student")
    List<SchoolRankEntity> findAllWithStudent();
}
