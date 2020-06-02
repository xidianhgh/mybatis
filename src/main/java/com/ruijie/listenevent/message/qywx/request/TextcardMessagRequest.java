package com.ruijie.listenevent.message.qywx.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>Title:      </p>
 * <p>Description:
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2020/3/2 11:13       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
public class TextcardMessagRequest extends QywxMessageRequest {

    class TextCard{
        private String title;
        private String description;
        private String url;
        private String btntxt;

        TextCard(String title,String description,String url,String btntxt){
            this.title = title;
            this.description = description;
            this.url = url;
            this.btntxt = btntxt;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getBtntxt() {
            return btntxt;
        }

        public void setBtntxt(String btntxt) {
            this.btntxt = btntxt;
        }
    }

    @JsonProperty("textcard")
    private TextCard textCard;

    public void setTextCard(String title,String description,String url,String btntxt){
        this.textCard = new TextCard(title, description, url, btntxt);
    }

}
