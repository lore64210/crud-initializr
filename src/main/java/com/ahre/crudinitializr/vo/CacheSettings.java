package com.ahre.crudinitializr.vo;

import lombok.Data;

@Data
public class CacheSettings {

    private boolean enabled;
    private String prefix = "crud";
    private long ttlSeconds = 600;
    private boolean cacheFindAll;
}
