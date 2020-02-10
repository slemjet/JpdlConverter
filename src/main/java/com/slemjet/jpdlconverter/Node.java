package com.slemjet.jpdlconverter;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
public class Node {
    private String name;
    private String handler;
    private Set<Decision> decisions;
}
