# 명언 앱 ⑧ MySQL 전환 (15단계)

명언 앱 ⑦(파일 저장)에서 **리포지터리 구현체만 MySQL(JDBC)로 갈아 끼운** 버전입니다.
등록·목록·삭제·수정·검색·페이징·빌드가 모두 MySQL 저장소로 동작합니다.

## 1. MySQL 띄우기 · DB/테이블 만들기

```
docker run --name mysql-1 -p 127.0.0.1:3306:3306 -e TZ=Asia/Seoul -e MYSQL_ROOT_PASSWORD=<비밀번호> -d mysql:8.4
docker exec -i mysql-1 mysql -uroot -p<비밀번호> < sql/schema.sql
```

[`sql/schema.sql`](sql/schema.sql) 이 개발용 DB `wiseSaying__dev` 와 테스트용 DB `wiseSaying__test` 를 만들고, 두 DB 에 아래 테이블을 만듭니다.
(앱도 시작할 때 `CREATE TABLE IF NOT EXISTS` 로 테이블을 만듭니다.)

```sql
CREATE TABLE IF NOT EXISTS wise_saying (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    createDate DATETIME NOT NULL,
    modifyDate DATETIME NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4;
```

## 2. 접속 정보 (하드코딩 금지)

접속 정보는 소스에 없습니다. `DbConfig` 가 아래 순서로 읽습니다.

1. 환경변수 `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`, `DB_TEST_NAME`
2. 프로젝트 루트의 `db.properties` (git 에 올리지 않음)

```
cp db.properties.example db.properties   # db.password 채우기
```

## 3. 실행 / 테스트

```
./gradlew run     # 개발 DB(wiseSaying__dev) 사용
./gradlew test    # 테스트 DB(wiseSaying__test) 사용, 매 테스트 전에 TRUNCATE
```

## 4. 리포지터리를 어떻게 갈아 끼웠나

```
App → WiseSayingController → WiseSayingService → WiseSayingRepository (interface)
                                                   ├─ WiseSayingMemoryRepository   (④~⑥)
                                                   ├─ WiseSayingFileRepository     (⑦)
                                                   └─ WiseSayingDbRepository       (⑧) ← AppContext 에서 이것으로 교체
```

- `WiseSayingRepository` 인터페이스(`save`, `findAll`, `findForList`, `count`, `findById`, `delete`, `build`)는 그대로 두고,
  `AppContext.renew()` 에서 `new WiseSayingFileRepository(...)` → `new WiseSayingDbRepository(DbConfig.load(...), ...)` 로 한 줄만 바꿨습니다.
- **컨트롤러·서비스 코드는 바꾸지 않았습니다.** JDBC·SQL 은 `WiseSayingDbRepository` 에만 있습니다.
- 검색·페이징은 SQL 로 처리합니다: `WHERE content|author LIKE CONCAT('%', ?, '%') ORDER BY id DESC LIMIT ? OFFSET ?` + `COUNT(*)`.
- 번호는 `AUTO_INCREMENT` 라 재시작해도 이어지고, 삭제한 번호를 재사용하지 않습니다.
- `빌드` 는 DB 의 모든 명언을 `db/wiseSaying/data.json`(테스트는 `db_test/wiseSaying/data.json`)으로 내보냅니다.
- 의존성: `runtimeOnly("com.mysql:mysql-connector-j:9.5.0")`

## 5. 테스트

- `WiseSayingControllerTest` : 종료·등록·목록·삭제·수정, 없는 번호
- `WiseSayingSearchPagingTest` : 내용/작가 검색, 샘플 10개 페이징, 검색 결과 기준 페이징
- `WiseSayingDbRepositoryTest` : 테스트 DB 사용 확인, CRUD, SQL 검색·페이징, 재시작 후 유지·번호 미재사용, `data.json` 빌드

모든 테스트는 `DbTestHelper.reset()` 으로 테스트용 DB 의 `wise_saying` 테이블을 `TRUNCATE` 한 뒤 시작합니다.
