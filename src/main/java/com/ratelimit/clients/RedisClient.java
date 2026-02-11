package com.ratelimit.clients;

import java.util.List;

public interface RedisClient {
    Object eval(String script, List<String> keys, List<String> args);
}

