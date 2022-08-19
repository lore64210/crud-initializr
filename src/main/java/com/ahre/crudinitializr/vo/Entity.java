package com.ahre.crudinitializr.vo;

import com.ahre.crudinitializr.enums.TypeEnum;
import lombok.Data;

import java.util.HashMap;

@Data
public class Entity {

    private String entityName;
    private HashMap<String, Attribute> attributes;
}
