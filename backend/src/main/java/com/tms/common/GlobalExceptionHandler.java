package com.tms.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public R<Void> biz(BizException e) { return R.fail(400, e.getMessage()); }
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> dup(DuplicateKeyException e) { return R.fail(400, "编码已存在，请勿重复"); }
    @ExceptionHandler(Exception.class)
    public R<Void> other(Exception e) { log.error("Unhandled error", e); return R.fail(500, e.getMessage()); }
}
