package com.fanyu.boundless.bean.theclass;

import com.fanyu.boundless.config.HttpPostService;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.Api.BaseApi;
import retrofit2.Retrofit;
import rx.Observable;

public class ClassXiaoXiListApi extends BaseApi {
    private String page;
    private String pagesize;
    private String userid;

    public ClassXiaoXiListApi() {
        setMothed("getClassXiaoXi.action");
        setShowProgress(true);
        setCancel(true);
    }

    public String getUserid() {
        return this.userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getPage() {
        return this.page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getPagesize() {
        return this.pagesize;
    }

    public void setPagesize(String pagesize) {
        this.pagesize = pagesize;
    }

    public Observable getObservable(Retrofit retrofit) {
        HttpPostService httpPostService = (HttpPostService) retrofit.create(HttpPostService.class);
        System.out.println(getUserid() + getPage() + getPagesize());
        return httpPostService.getClassXiaoXi(getUserid(), getPage(), getPagesize());
    }
}
