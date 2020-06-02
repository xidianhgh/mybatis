package com.ruijie.listenevent.service;

import com.ruijie.listenevent.common.GerritClient;
import com.ruijie.listenevent.common.constant.Constant;
import com.ruijie.listenevent.entity.GroupMemberEntity;
import com.ruijie.listenevent.dao.GroupMemberMapper;
import com.ruijie.listenevent.utils.DateUtils;
import com.ruijie.listenevent.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

@Component
@Slf4j
public class ListenFileService implements ApplicationRunner {
    @Autowired
    GerritClient gerritClient;
    @Autowired
    ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    LogActionService logActionService;
    private WatchService watcher;

    private Path path;
    private int lastTimeLine = 0;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        threadPoolTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                log.info("开始监听文件...");
                String filePath = "\\\\192.168.160.128\\share";

                try {
                    setFilePath(Paths.get(filePath));
                } catch (Exception e) {
                    log.error(filePath + "监听的文件路径有误，请确认是否已为共享文件夹:" + e.getMessage());
                }

                try {
                    listenEvents();
                } catch (Exception e) {
                    log.error("文件解析失败:" + e.getMessage(), e);
                }


            }
        });


    }

    public ListenFileService() {

    }

    public void setFilePath(Path filePath) throws IOException {
        this.path = filePath;
        watcher = FileSystems.getDefault().newWatchService();
        this.path.register(watcher, OVERFLOW, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    }

    public void listenEvents() throws Exception {
        // start to process the data files
        while (true) {
            // start to handle the file change event
            final WatchKey key = watcher.take();

            for (WatchEvent<?> event : key.pollEvents()) {
                // get event type
                final WatchEvent.Kind<?> kind = event.kind();

                // get file name
                @SuppressWarnings("unchecked") final WatchEvent<Path> pathWatchEvent = (WatchEvent<Path>) event;
                final Path fileName = pathWatchEvent.context();

                if (kind == ENTRY_CREATE) {
                    System.out.println(fileName);
                    System.out.println("ENTRY_CREATE");
                    // 说明点1
                    // create a new thread to monitor the new file
                    new Thread(new Runnable() {
                        public void run() {
                            File file = new File(path.toFile().getAbsolutePath() + "/" + fileName);
                            boolean exist;
                            long size = 0;
                            long lastModified = 0;
                            int sameCount = 0;
                            while (exist = file.exists()) {
                                // if the 'size' and 'lastModified' attribute keep same for 3 times,
                                // then we think the file was transferred successfully
                                if (size == file.length() && lastModified == file.lastModified()) {
                                    if (++sameCount >= 3) {
                                        break;
                                    }
                                } else {
                                    size = file.length();
                                    lastModified = file.lastModified();
                                }
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    return;
                                }
                            }
                            // if the new file was cancelled or deleted
                            if (!exist) {
                                return;
                            } else {

                            }
                        }
                    }).start();
                } else if (kind == ENTRY_DELETE) {
                    System.out.println(fileName);
                    System.out.println("ENTRY_DELETE");
                } else if (kind == ENTRY_MODIFY) {
                    System.out.println(fileName);
                    System.out.println("ENTRY_MODIFY");
                    if (Constant.ERROR_LOG.equals(fileName.toString())) {
                        String filePath = "\\\\192.168.160.128\\share\\error_log";
                        int totalLine = FileUtils.countFileTotalLine(filePath);
                        System.out.println("文件总行数：" + totalLine);

                        if (lastTimeLine != totalLine) {

                            if (lastTimeLine != 0 && lastTimeLine < totalLine) {
                                for (int i = lastTimeLine + 1; i <= totalLine; i++) {
                                    String content = FileUtils.readFile(filePath, i);
                                    try {
                                        logActionService.detectHttpDelete(content);
                                    } catch (Exception e) {
                                        log.error("处理事件有误", e);
                                    }
                                }
                            } else {
                                String content = FileUtils.readFile(filePath, totalLine);
                                try {
                                    logActionService.detectHttpDelete(content);
                                } catch (Exception e) {
                                    log.error("处理事件有误:" + content, e);
                                }
                            }
                            lastTimeLine = totalLine;
                        }
//                        System.out.println(FileUtils.countFileTotalLine("\\\\192.168.160.128\\share\\error_log"));
                    }
//                    Groups.ListRequest groups = gerritClient.getGerritApi().groups().list();
//                    System.out.println("member name:" + groups.get());
                } else if (kind == OVERFLOW) {
                    System.out.println(fileName);
                    System.out.println("OVERFLOW");
                }
            }

            // IMPORTANT: the key must be reset after processed
            if (!key.reset()) {
                return;
            }
        }
    }
}
