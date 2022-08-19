package com.ahre.crudinitializr.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Domain {

    private String title;
    private List<Entity> entities = new ArrayList<>();

}
