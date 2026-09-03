# error-audit 결과

총평: 동시성 시그널(비동기/멀티스레드)이 없는 소규모 프로젝트라 표준 체크리스트를 가중치 없이 적용했다. `./gradlew compileJava`를 직접 실행해 실제 컴파일 여부까지 확인했으며, 현재 `main` 소스는 **컴파일 자체가 실패하는 상태**다. 그 외 회원가입 로직에 TOCTOU(check-then-act) 경쟁 조건과 응답 DTO 필드 누락이 있다.

---

### [카테고리: 에러 / Critical] SignupService.java 컴파일 실패 — 정의되지 않은 변수 `puuid` 참조

- **위치**: `src/main/java/com/example/quick_gg/service/SignupService.java:53`
- **문제**: `.puuid(puuid)`에서 `puuid`는 이 메서드 어디에도 선언/할당되지 않은 식별자다. 실제로 `./gradlew compileJava`를 실행해 확인한 결과 아래와 같이 컴파일이 즉시 실패한다. 이 상태에서는 애플리케이션을 빌드/실행/배포할 수 없다.
- **코드 근거**:
  ```
  > Task :compileJava FAILED
  SignupService.java:53: error: cannot find symbol
                  .puuid(puuid) // 라이엇 API 연동 로직이 아직 완성되지 않아 컴파일 에러(고칠 예정)
                         ^
    symbol:   variable puuid
    location: class SignupService
  ```
- **판단 근거(왜 문제인지)**: Java 언어 명세상 선언되지 않은 식별자는 컴파일 타임 오류이며 예외적 상황이 아니라 빌드 자체가 성립하지 않는다. 코드 주석("라이엇 API 연동 로직이 아직 완성되지 않아 컴파일 에러(고칠 예정)")으로 보아 개발자 본인도 인지하고 있는 미완성 상태로 보인다.

### [카테고리: 에러 / High] 회원가입 중복 검사에 TOCTOU(Check-Then-Act) 경쟁 조건 존재

- **위치**: `src/main/java/com/example/quick_gg/service/SignupService.java:23-59`
- **문제**: 동일한 학번(`studentID`) 또는 동일한 (소환사명+태그)로 두 개의 회원가입 요청이 거의 동시에 들어오면, 둘 다 `existsByStudentNumber`/`existsBySummonerNameAndTag` 검사를 통과(둘 다 "아직 없음"으로 조회)한 뒤 각자 `repository.save(student)`를 호출할 수 있다. `StudentEntity`의 DB 유니크 제약(`student_number`, `puuid`, `summoner_name`+`tag` 복합 유니크) 덕분에 실제 중복 행이 생기지는 않지만, 두 번째 요청은 의도한 `CustomException(ErrorCode.CONFLICT)`(409) 대신 처리되지 않은 `DataIntegrityViolationException`으로 실패해 `GlobalExceptionHandler`의 캐치올 `Exception` 핸들러를 타고 500(내부 서버 오류)으로 응답된다.
- **코드 근거**:
  ```java
  if (repository.existsByStudentNumber(request.getStudentID())) {
      throw new CustomException(ErrorCode.CONFLICT);
  }
  // ... 이 사이에 다른 요청이 끼어들 수 있음 (메서드에 @Transactional/락 없음) ...
  repository.save(student); // 두 번째 요청은 여기서 DataIntegrityViolationException
  ```
- **판단 근거(왜 문제인지)**: "확인 후 행동(check-then-act)" 패턴은 두 단계 사이에 원자성이 보장되지 않으면 경쟁 조건이 발생한다는 것이 일반적인 동시성 원칙이다. 이 메서드에는 `@Transactional`도, DB 유니크 제약 위반을 사용자 친화적 응답으로 변환하는 예외 처리도 없어, 동시 요청 시 사용자에게 "이미 가입된 학번입니다(409)" 대신 "서버 내부 오류(500)"가 노출된다.

### [카테고리: 에러 / Medium] SignupResponse의 `id` 필드가 항상 null로 반환됨

- **위치**: `src/main/java/com/example/quick_gg/service/SignupService.java:62-69`, `src/main/java/com/example/quick_gg/dto/response/SignupResponse.java:14-15`
- **문제**: `SignupResponse`는 "가입된 학생의 PK"를 담는 `id` 필드를 갖고 있지만, `SignupService.signup()`의 빌더 호출에는 `.id(student.getId())`가 없다. `repository.save(student)` 이후 `student` 엔티티에는 DB가 생성한 PK가 채워져 있음에도 응답에는 반영되지 않아, 클라이언트는 항상 `id: null`을 받는다.
- **코드 근거**:
  ```java
  repository.save(student); // 이 시점에 student.getId()는 채워짐 (IDENTITY 전략)

  return SignupResponse.builder()
          .studentID(student.getStudentID())
          .name(student.getName())
          .summonerName(student.getSummonerName())
          .tag(student.getTag())
          .createAt(student.getCreateAt())
          // .id(...) 호출 누락
          .build();
  ```
- **판단 근거(왜 문제인지)**: JPA `GenerationType.IDENTITY` 전략에서는 `save()` 호출 직후 엔티티에 생성된 PK가 채워지는 것이 명세된 동작(Hibernate/JPA 공식 동작)이다. 값이 존재함에도 응답 빌더에 옮기지 않은 것은 예외를 던지지 않는 "조용한" 로직 누락이라 테스트 없이는 발견되기 어렵다.

### [카테고리: 에러 / Low] JwtAuthenticationFilter의 `jwtTokenProvider != null` 검사는 항상 참인 무의미한 코드

- **위치**: `src/main/java/com/example/quick_gg/security/JwtAuthenticationFilter.java:38`
- **문제**: `jwtTokenProvider`는 `@RequiredArgsConstructor`로 생성자 주입되는 `final` 필드라 스프링 컨테이너가 정상적으로 빈을 구성하면 항상 non-null이다. `null` 체크가 실제로 방어하는 실패 시나리오가 없어 오히려 "이 필드가 null일 수 있다"는 잘못된 인상을 줄 수 있다.
- **코드 근거**:
  ```java
  private final JwtTokenProvider jwtTokenProvider;
  ...
  if (jwtTokenProvider != null && jwtTokenProvider.validateToken(token)) {
  ```
- **판단 근거(왜 문제인지)**: Spring의 생성자 주입 방식에서 `final` 필드는 빈 생성 시점에 반드시 할당되며, 할당에 실패하면 애플리케이션 컨텍스트 자체가 기동에 실패한다(즉 이 필터 코드에 도달할 수 없음). 버그를 유발하지는 않지만 죽은 방어 코드다.

### [카테고리: 에러 / Info] 문제 없음 — 리소스 누수 / 형변환 비교 / 페이징

- 프로젝트에 `InputStream`/`Connection`을 직접 다루는 코드, `Integer`/`Long` 등을 `==`로 비교하는 코드, 페이징/캐시 로직이 존재하지 않아 해당 체크리스트는 점검 대상이 없다.

### [카테고리: 에러 / Info] 문제 없음 — 동시성(공유 상태, `@Async`)

- `static` 필드에 대한 동시 쓰기, 스레드 안전하지 않은 컬렉션의 멀티스레드 공유 사용, `@Async` 메서드 자체가 프로젝트에 없다 (위 TOCTOU 항목은 "동시 요청 간 DB 경쟁"이며 이 체크리스트 항목이 다루는 "애플리케이션 내부 공유 상태 동시성"과는 별개로 구분해 기록했다).
