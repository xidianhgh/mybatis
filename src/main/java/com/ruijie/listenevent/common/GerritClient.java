package com.ruijie.listenevent.common;


import com.google.common.collect.Lists;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.api.changes.ReviewerInfo;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.*;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApi;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * <p>Title:      </p>
 * <p>Description: Gerrit客户端
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2019/7/31 14:58       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
@Slf4j
public class GerritClient {

//    private AssSystem assSystem;
    private GerritRestApi gerritApi;

    public GerritClient() {
        this.gerritApi = newGerritApi();
    }
    private GerritRestApi newGerritApi() {
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
//        String url =assSystem.getUrlPrefix()+assSystem.getIp()+ StringPool.COLON+assSystem.getPort();
//        String url="http://gerrit.ruijie.work:8081";
        String url="http://192.168.160.128:8091";
        GerritAuthData.Basic authData = new GerritAuthData.Basic(url,
                "admin", "IMd3jzGPf3F9VMFZp7rfHL9oqKkTfRme0o71ONYupg", true);
        return gerritRestApiFactory.create(authData);
    }

    public GerritRestApi getGerritApi(){
        return this.gerritApi;
    }

    public Set<String> getMembers(String groupID){
        Set<String> set = new HashSet<String>();
        try {
            List<AccountInfo> members = gerritApi.groups().id(groupID).detail().members;
            for(AccountInfo accountInfo:members){
                if(accountInfo.username!=null){
                   set.add(accountInfo.username);
                }
            }
        } catch (RestApiException e) {
            e.printStackTrace();
        }
        return  set;
    }

    public List<String> getReviewers(String changeId){
        try {
            List<String> reviewers = Lists.newArrayList();
            List<ReviewerInfo> list  = gerritApi.changes().id(changeId).listReviewers();
            list.forEach(i->{
                if(!i.username.equals("switch_svn_rw") && !i.username.equals("gerrit_caf")){
                    reviewers.add(i.username);
                }
            });
            return reviewers;
        } catch (RestApiException e) {
            log.error("获取Reviewer失败：{}",e.getMessage());
        }
        return Lists.newArrayList();
    }

    public List<ProjectInfo> getAllProject(){
        List<ProjectInfo> projectInfos = null;
        try {
            projectInfos = gerritApi.projects().list().get();
        } catch (RestApiException e) {
            e.printStackTrace();
        }
        return projectInfos;
    }

    public boolean checkCIVerified(int number) {
        try {
            /**GerritV2.15使用DETAILED_LABELS选项才能获取到lable详情，GerritV3.0直接使用changes().id(xx).info()获取**/
            ChangeInfo changeInfo = gerritApi.changes().query("change:" + number + "&o=DETAILED_LABELS").get().get(0);
            Map<String, LabelInfo> labels = changeInfo.labels;
            LabelInfo verified = labels.get("Verified");
            if(verified!=null){
                for(ApprovalInfo approvalInfo:verified.all){
                    /**判断是否switch_svn_rw账号已verified+1**/
                    if(approvalInfo.value!=null && approvalInfo.value==1 && approvalInfo._accountId.equals(1000108)){
                        return true;
                    }
                }
            }
        } catch (RestApiException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getChangeCodeLine(String changeId){
        try {
            ChangeApi changes = gerritApi.changes().id(changeId);
            /**估计是版本问题*/
            EnumSet<ListChangesOption> options = EnumSet.allOf(ListChangesOption.class);
            options.remove(ListChangesOption.SKIP_MERGEABLE);
            ChangeInfo changeInfo = changes.get(options);

            if (changeInfo.subject.contains("init from")) {
                return 0;
            }
            int inset = changeInfo.insertions == null ? 0 : changeInfo.insertions;
            int dele = changeInfo.deletions == null ? 0 : changeInfo.deletions;
            return inset+Math.abs(dele);
        } catch (RestApiException e) {
            log.info("查询出现异常：{}",changeId);
        }
        return 0;
    }

}
