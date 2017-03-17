package com.fanyu.boundless.bean.theclass;

import com.fanyu.boundless.config.HttpPostService;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.Api.BaseApi;
import retrofit2.Retrofit;
import rx.Observable;

public class OutClassApi extends BaseApi {
    private String classid;
    private String userid;

    public String getUserid() {
        return this.userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getClassid() {
        return this.classid;
    }

    public void setClassid(String classid) {
        this.classid = classid;
    }

    public OutClassApi() {
        setMothed("OutClass.action");
        setCancel(true);
        setShowProgress(true);
    }

    public Observable getObservable(Retrofit retrofit) {
        return ((HttpPostService) retrofit.create(HttpPostService.class)).outClass(getUserid(), getClassid());
    }
}
