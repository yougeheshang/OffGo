package org.tinkerhub.offgo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Point {
    private double latitude;
    private double longitude;
}