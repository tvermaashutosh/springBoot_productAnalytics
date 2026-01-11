package com.example.productAnalytics.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class ViewEvent {
    UUID id;
    String clazz;
    String productId;
    String userIp;

    public ViewEvent(String productId, String userIp) {
        this.id = UUID.randomUUID();
        this.clazz = getClass().getSimpleName();
        this.productId = productId;
        this.userIp = userIp;
    }
}
