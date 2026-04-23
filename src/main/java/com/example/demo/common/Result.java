package com.example.demo.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(ResultCode.SUCCESS.getMsg());
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(ResultCode code) {
        Result<T> r = new Result<>();
        r.setCode(code.getCode());
        r.setMsg(code.getMsg());
        return r;
    }
}