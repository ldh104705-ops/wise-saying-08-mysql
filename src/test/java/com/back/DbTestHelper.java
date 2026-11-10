package com.back;

import com.back.domain.wiseSaying.repository.WiseSayingDbRepository;
import com.back.global.app.AppConfig;
import com.back.global.app.DbConfig;
import com.back.standard.util.JsonUtil;

/**
 * 테스트용 DB(wiseSaying__test)를 쓰고, 매 테스트 전에 테이블을 비운다.
 */
public final class DbTestHelper {
    private DbTestHelper() {
    }

    public static void reset() {
        AppConfig.setModeToTest();

        WiseSayingDbRepository repository = new WiseSayingDbRepository(
                DbConfig.load(true),
                AppConfig.getDbDirPath() + "/data.json"
        );
        repository.createTableIfNotExists();
        repository.truncate();

        JsonUtil.deleteDir(AppConfig.getDbDirPath());
    }
}
