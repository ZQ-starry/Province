package com.sx.server0.entity.result;

import lombok.Data;

@Data
public class FaultsLoadRateEntity {
    private String time;
    private float loadRate;
    private String nodeNameStart;
    private String nodeNameEnd;
}
