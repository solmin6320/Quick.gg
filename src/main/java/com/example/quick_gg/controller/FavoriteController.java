package com.example.quick_gg.controller;

import com.example.quick_gg.dto.request.FavoriteRequest;
import com.example.quick_gg.dto.response.FavoriteResponse;
import com.example.quick_gg.service.FavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // 즐겨찾기 목록 조회
    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> getFavorites() {
        return ResponseEntity.ok(favoriteService.getFavorites());
    }

    // 즐겨찾기 추가
    @PostMapping
    public ResponseEntity<FavoriteResponse> addFavorite(
            @Valid @RequestBody FavoriteRequest request) {

        FavoriteResponse response = favoriteService.addFavorite(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 즐겨찾기 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFavorite(@PathVariable Integer id) {
        favoriteService.deleteFavorite(id);

        return ResponseEntity.noContent().build();
    }
}
