package com.back.domain.wiseSaying.repository;

import com.back.AppTestRunner;
import com.back.DbTestHelper;
import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.global.app.AppConfig;
import com.back.global.app.AppContext;
import com.back.global.app.DbConfig;
import com.back.standard.dto.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

class WiseSayingDbRepositoryTest {
    private WiseSayingDbRepository repository;

    @BeforeEach
    void beforeEach() {
        // 테스트용 DB 사용 + 매 테스트 전에 TRUNCATE
        DbTestHelper.reset();
        AppConfig.setSampleDataEnabled(false);

        repository = new WiseSayingDbRepository(DbConfig.load(true), AppConfig.getDbDirPath() + "/data.json");
    }

    @Test
    @DisplayName("테스트는 개발 DB 가 아닌 테스트용 DB 를 쓴다")
    void t0() {
        assertThat(DbConfig.load(true).dbName()).isEqualTo("wiseSaying__test");
        assertThat(DbConfig.load(false).dbName()).isEqualTo("wiseSaying__dev");
    }

    @Test
    @DisplayName("앱은 MySQL 리포지터리를 쓴다")
    void t1() {
        AppContext.renew(new Scanner(""));

        assertThat(AppContext.wiseSayingRepository).isInstanceOf(WiseSayingDbRepository.class);
    }

    @Test
    @DisplayName("save / findById / delete")
    void t2() {
        WiseSaying saved = repository.save(new WiseSaying("현재를 사랑하라.", "작자미상"));

        assertThat(saved.getId()).isEqualTo(1);
        assertThat(repository.findById(1)).get()
                .extracting(WiseSaying::getContent)
                .isEqualTo("현재를 사랑하라.");

        saved.setContent("현재와 자신을 사랑하라.");
        repository.save(saved);
        assertThat(repository.findById(1).get().getContent()).isEqualTo("현재와 자신을 사랑하라.");

        assertThat(repository.delete(1)).isTrue();
        assertThat(repository.delete(1)).isFalse();
        assertThat(repository.findById(1)).isEmpty();
    }

    @Test
    @DisplayName("검색·페이징을 SQL 로 처리 - 최신순, 5개씩, 검색 결과 기준")
    void t3() {
        for (int i = 1; i <= 10; i++) {
            repository.save(new WiseSaying("명언 " + i, "작자미상 " + i));
        }

        Page<WiseSaying> page1 = repository.findForList("content", "", 1, 5);
        assertThat(page1.totalCount()).isEqualTo(10);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.content()).extracting(WiseSaying::getId).containsExactly(10, 9, 8, 7, 6);

        Page<WiseSaying> page2 = repository.findForList("content", "", 2, 5);
        assertThat(page2.content()).extracting(WiseSaying::getId).containsExactly(5, 4, 3, 2, 1);

        Page<WiseSaying> search = repository.findForList("content", "1", 1, 5);
        assertThat(search.totalCount()).isEqualTo(2);
        assertThat(search.content()).extracting(WiseSaying::getId).containsExactly(10, 1);

        Page<WiseSaying> authorSearch = repository.findForList("author", "작자미상 3", 1, 5);
        assertThat(authorSearch.content()).extracting(WiseSaying::getId).containsExactly(3);
    }

    @Test
    @DisplayName("재시작해도 DB 에서 읽어 목록을 보여 주고, 삭제한 번호는 재사용하지 않는다")
    void t4() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                삭제?id=2
                종료
                """);

        // 재시작
        String out = AppTestRunner.run("""
                등록
                새 명언
                새 작가
                목록
                종료
                """);

        assertThat(out)
                .contains("3번 명언이 등록되었습니다.")
                .containsSubsequence("3 / 새 작가 / 새 명언", "1 / 작자미상 / 현재를 사랑하라.")
                .doesNotContain("2 / 작자미상 / 과거에 집착하지 마라.");
    }

    @Test
    @DisplayName("빌드 - DB 의 모든 명언을 data.json 으로")
    void t5() throws IOException {
        String out = AppTestRunner.run("""
                등록
                명언 1
                작가 1
                등록
                명언 2
                작가 2
                빌드
                종료
                """);

        assertThat(out).contains("data.json 파일의 내용이 갱신되었습니다.");

        String json = Files.readString(Path.of(AppConfig.getDbDirPath(), "data.json"), StandardCharsets.UTF_8);
        assertThat(json).isEqualTo("""
                [
                  {
                    "id": 1,
                    "content": "명언 1",
                    "author": "작가 1"
                  },
                  {
                    "id": 2,
                    "content": "명언 2",
                    "author": "작가 2"
                  }
                ]""");
    }
}
