package com.ahre.crudinitializr.vo;

import com.ahre.crudinitializr.enums.TypeEnum;
import com.ahre.crudinitializr.enums.ValidationEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Attribute {

    private String name;
    private String customType;
    private TypeEnum type;
    private List<ValidationEnum> validations = new ArrayList<>();

}
