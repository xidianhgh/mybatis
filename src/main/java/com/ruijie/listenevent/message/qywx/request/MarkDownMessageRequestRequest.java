package com.ruijie.listenevent.message.qywx.request;

/**
 * <p>Title:      </p>
 * <p>Description:
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2020/3/2 11:28       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
public class MarkDownMessageRequestRequest extends QywxMessageRequest {

    class MarkDown{
        private String content;

        MarkDown(String content){
            this.content = content;
        }
    }

    private MarkDown text;

    public void  setContent(String content){
        this.text = new MarkDown(content);
    }
}
