package com.example.common;

public interface Constants {

    String TOKEN = "token";

    String USER_DEFAULT_PASSWORD = "123456";

    /** 密码加密盐值，修改后数据库中的历史密码将全部失效 */
    String PASSWORD_SALT = "devops_salt_2026";

}
