package com.ahre.crudinitializr.vo;

import com.ahre.crudinitializr.enums.ApiEnum;
import com.ahre.crudinitializr.enums.DatabaseEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Domain {

    private String title;
    private DatabaseEnum database = DatabaseEnum.POSTGRESQL;
    private ApiEnum apiType = ApiEnum.REST;
    private CacheSettings cache = new CacheSettings();
    private List<Entity> entities = new ArrayList<>();

}
