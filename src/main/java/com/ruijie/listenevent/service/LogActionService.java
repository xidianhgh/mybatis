package com.ruijie.listenevent.service;

import com.google.gerrit.extensions.common.AccountInfo;
import com.ruijie.listenevent.common.GerritClient;
import com.ruijie.listenevent.common.constant.Constant;
import com.ruijie.listenevent.dao.GroupMemberMapper;
import com.ruijie.listenevent.dto.AuditEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogActionService {
    @Autowired
    GerritClient gerritClient;
    @Autowired
    QywxService qywxService;
    @Autowired
    GroupMemberService groupMemberService;

    public void detectHttpDelete(String content) throws Exception {
        if (content.contains("DELETE") || content.contains("PUT")) {

            AuditEvent auditEvent = parseEvent(content);
            String what = auditEvent.getWhat();
            String[] whatArr = what.split("/");
            String eventType = whatArr[1];
            String eventSubType = whatArr[3];
            String method = auditEvent.getMethod();
            String when = auditEvent.getWhen();

            String who = auditEvent.getWho();
            String prefixWho = "IdentifiedUser[account ";
            int beginIndex = who.indexOf(prefixWho) + prefixWho.length();
            String executorId = who.substring(beginIndex, who.length() - 1);
            AccountInfo executorInfo = gerritClient.getGerritApi().accounts().id(executorId).get();
            //groups组事件
            if (Constant.EVENT_TYPE_GROUPS.equals(eventType)) {

                String memberId = whatArr[4];
                String groupName = whatArr[2];
                AccountInfo memberInfo = gerritClient.getGerritApi().accounts().id(memberId).get();

                List<AccountInfo> membersList = gerritClient.getGerritApi().groups().id("").members();
                StringBuilder toUser = new StringBuilder();
                for (int i = 0; i < membersList.size(); i++) {
                    toUser.append(membersList.get(i).username);
                    if (i != membersList.size() - 1) {
                        toUser.append("\\|");
                    }
                }

                if (method.contains("DELETE")) {
                    qywxService.sendMessageTxt(String.format("%s(%s)把%s(%s)从%s组移除了！\r\n时间：%s",
                            executorInfo.name, executorInfo.username, memberInfo.name, memberInfo.username, groupName, when),toUser.toString());
                } else if (method.contains("PUT")) {
                    qywxService.sendMessageTxt(String.format("%s(%s)把%s(%s)加入%s组！\r\n时间：%s",
                            executorInfo.name, executorInfo.username, memberInfo.name, memberInfo.username, groupName, when),toUser.toString());
                }

                groupMemberService.saveGroupMemberEvent(method,
                        executorInfo.name,
                        memberInfo.name,
                        when, groupName);

            }
            //project events
//            if (Constant.EVENT_TYPE_PROJECTS.equals(eventType)) {
//
//                String memberId = whatArr[4];
//                String groupName = whatArr[2];
//                AccountInfo memberInfo = gerritClient.getGerritApi().accounts().id(memberId).get();
//
//                if (method.contains("DELETE")) {
//                    qywxService.sendMessageTxt(String.format("%s(%s)把%s(%s)从%s组移除了！\r\n时间：%s",
//                            executorInfo.name, executorInfo.username, memberInfo.name, memberInfo.username, groupName, when));
//                } else if (method.contains("PUT")) {
//                    qywxService.sendMessageTxt(String.format("%s(%s)把%s(%s)加入%s组！\r\n时间：%s",
//                            executorInfo.name, executorInfo.username, memberInfo.name, memberInfo.username, groupName, when));
//                }
//
//                groupMemberService.saveGroupMemberEvent(method,
//                        executorInfo.name,
//                        memberInfo.name,
//                        when, groupName);
//
//            }

        }
    }

    public AuditEvent parseEvent(String content) {
        String beginFlag = "audit:";
        int beginIndex = content.indexOf(beginFlag) + beginFlag.length();
        String remain = content.substring(beginIndex);
        // error_log 中使用｜分隔
        String[] element = remain.split("\\|");
        AuditEvent auditEvent = new AuditEvent();
        int i = 0;
        auditEvent.setUuid(element[i++].trim());
        auditEvent.setWhen(element[i++].trim());
        auditEvent.setSessionId(element[i++].trim());
        auditEvent.setWho(element[i++].trim());
        auditEvent.setMethod(element[i++].trim());
        auditEvent.setWhat(element[i++].trim());
        auditEvent.setParams(element[i++].trim());
        auditEvent.setResult(element[i++].trim());
        auditEvent.setTimeAtStart(element[i++].trim());
        auditEvent.setElapsed(element[i++].trim());
        return auditEvent;
    }

}
